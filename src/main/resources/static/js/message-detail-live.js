(() => {
    const conversationElem = document.querySelector('section.conversation[data-message-id]');
    if (!conversationElem) return;

    const messageId = conversationElem.getAttribute('data-message-id');
    const typingIndicator = document.getElementById('conversationTypingIndicator');
    const typingText = document.getElementById('conversationTypingText');
    const replyEditor = document.getElementById('reply-editor');
    const replyForm = document.getElementById('reply-form');

    const getCsrfData = () => {
        let token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        let header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
        if (!token) {
            token = document.querySelector('input[name="_csrf"]')?.value;
        }
        return { token, header };
    };

    const scrollToBottom = () => {
        conversationElem.scrollTop = conversationElem.scrollHeight;
    };

    // Scroll to bottom initially
    scrollToBottom();

    // Mark as read
    const markAsRead = () => {
        const { token, header } = getCsrfData();
        const reqHeaders = {
            'Content-Type': 'application/x-www-form-urlencoded',
            ...(token && header ? { [header]: token } : {})
        };
        const body = new URLSearchParams({ messageId: messageId });
        if (token) body.append('_csrf', token);
        fetch('/api/chat/read-message', {
            method: 'POST',
            headers: reqHeaders,
            body: body.toString()
        }).catch(e => console.warn('mark-read error:', e));
    };

    // Call mark as read on load
    markAsRead();

    // Typing tracking
    let isTypingSent = false;
    let stopTypingTimer = null;

    const sendTypingStatus = (typing) => {
        const { token, header } = getCsrfData();
        const reqHeaders = {
            'Content-Type': 'application/x-www-form-urlencoded',
            ...(token && header ? { [header]: token } : {})
        };
        const body = new URLSearchParams({ messageId: messageId, isTyping: typing });
        if (token) body.append('_csrf', token);
        fetch('/api/chat/message-typing', {
            method: 'POST',
            headers: reqHeaders,
            body: body.toString()
        }).catch(() => {});
    };

    if (replyEditor) {
        replyEditor.addEventListener('input', () => {
            if (!isTypingSent) {
                isTypingSent = true;
                sendTypingStatus(true);
            }
            if (stopTypingTimer) clearTimeout(stopTypingTimer);
            stopTypingTimer = setTimeout(() => {
                isTypingSent = false;
                sendTypingStatus(false);
            }, 3000);
        });

        replyEditor.addEventListener('blur', () => {
            if (isTypingSent) {
                if (stopTypingTimer) clearTimeout(stopTypingTimer);
                isTypingSent = false;
                sendTypingStatus(false);
            }
        });
    }

    if (replyForm) {
        replyForm.addEventListener('submit', () => {
            if (stopTypingTimer) clearTimeout(stopTypingTimer);
            if (isTypingSent) {
                isTypingSent = false;
                sendTypingStatus(false);
            }
        });
    }

    const formatDateTime = (isoString) => {
        if (!isoString) return '';
        const d = new Date(isoString);
        const pad = (n) => String(n).padStart(2, '0');
        return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    };

    const renderNewMessage = (m) => {
        const row = document.createElement('div');
        row.className = 'conversation-message' + (m.sentByCurrentUser ? ' conversation-message-own' : '');
        row.setAttribute('data-msg-id', m.id);

        const avatarUrl = m.senderProfileImageUrl || '/images/default-avatar.svg';
        const isOwn = m.sentByCurrentUser;

        const tickHtml = isOwn
            ? `<span class="conversation-read-status ${m.read ? 'is-read' : 'is-sent'}" title="${m.read ? 'Görüldü' : 'İletildi'}">
                <span class="material-symbols-outlined">${m.read ? 'done_all' : 'done'}</span>
               </span>`
            : '';

        let imagesHtml = '';
        if (m.imageUrls && m.imageUrls.length > 0) {
            imagesHtml = `<div class="message-images">${m.imageUrls.map(url => `<img src="${url}" alt="Mesaja eklenen görsel">`).join('')}</div>`;
        }

        let attachHtml = '';
        if (m.attachment) {
            attachHtml = `
                <div class="message-attachment">
                    <a href="/messages/${m.id}/attachment/${m.attachment.id}" class="attachment-link" download="${m.attachment.alias || m.attachment.originalName}" title="İndir: ${m.attachment.alias || m.attachment.originalName}">
                        <span class="material-symbols-outlined attachment-icon">attach_file</span>
                        <div class="attachment-info">
                            <span class="attachment-name">${m.attachment.alias || m.attachment.originalName}</span>
                            <small class="attachment-size">${m.attachment.formattedSize}</small>
                        </div>
                        <span class="material-symbols-outlined attachment-download-icon">download</span>
                    </a>
                </div>
            `;
        }

        row.innerHTML = `
            <img src="${avatarUrl}" alt="${m.senderName} profil fotoğrafı">
            <div class="conversation-bubble">
                <div>
                    <strong>${m.senderName}</strong>
                    <time>${formatDateTime(m.sentAt)}</time>
                    ${tickHtml}
                </div>
                <div class="rich-message-body">${m.body || ''}</div>
                ${imagesHtml}
                ${attachHtml}
            </div>
        `;

        if (typingIndicator) {
            conversationElem.insertBefore(row, typingIndicator);
        } else {
            conversationElem.appendChild(row);
        }
    };

    // Polling function
    const syncLive = () => {
        fetch(`/api/chat/conversation-live?messageId=${messageId}`, {
            headers: { 'Accept': 'application/json' }
        })
        .then(res => {
            if (!res.ok) throw new Error('Status: ' + res.status);
            return res.json();
        })
        .then(data => {
            if (!data) return;

            // Typing indicator update
            if (typingIndicator) {
                const wasHidden = typingIndicator.style.display === 'none';
                if (data.isPeerTyping) {
                    typingIndicator.style.display = 'flex';
                    if (typingText) {
                        typingText.textContent = (data.counterpartName || 'Kullanıcı') + ' yazıyor...';
                    }
                    if (wasHidden) scrollToBottom();
                } else {
                    typingIndicator.style.display = 'none';
                }
            }

            // Messages update
            if (data.messages && data.messages.length > 0) {
                let hasNewIncoming = false;
                let addedAny = false;

                data.messages.forEach(m => {
                    const existing = conversationElem.querySelector(`.conversation-message[data-msg-id="${m.id}"]`);
                    if (!existing) {
                        renderNewMessage(m);
                        addedAny = true;
                        if (!m.sentByCurrentUser) {
                            hasNewIncoming = true;
                        }
                    } else {
                        // Update seen status if changed
                        if (m.sentByCurrentUser) {
                            const statusElem = existing.querySelector('.conversation-read-status');
                            if (statusElem) {
                                if (m.read && !statusElem.classList.contains('is-read')) {
                                    statusElem.classList.remove('is-sent');
                                    statusElem.classList.add('is-read');
                                    statusElem.setAttribute('title', 'Görüldü');
                                    const icon = statusElem.querySelector('.material-symbols-outlined');
                                    if (icon) icon.textContent = 'done_all';
                                }
                            }
                        }
                    }
                });

                if (addedAny) {
                    scrollToBottom();
                }

                if (hasNewIncoming) {
                    markAsRead();
                }
            }
        })
        .catch(err => {
            // Silently ignore network hiccups in live polling
        });
    };

    // Poll every 4 seconds
    const pollInterval = setInterval(syncLive, 4000);

    window.addEventListener('beforeunload', () => {
        clearInterval(pollInterval);
        if (stopTypingTimer) clearTimeout(stopTypingTimer);
        if (isTypingSent) {
            sendTypingStatus(false);
        }
    });
})();
