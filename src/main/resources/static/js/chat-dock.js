(() => {
    // Chat Dock Manager
    const MAX_CHATS = 3;
    const activeChats = new Map(); // friendId -> { element, pollTimer, ... }

    const getCsrfData = () => {
        let token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        let header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
        if (!token) {
            token = document.querySelector('input[name="_csrf"]')?.value;
        }
        if (!token) {
            const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
            if (match) token = decodeURIComponent(match[1]);
        }
        return { token, header };
    };

    const getCsrfHeaders = () => {
        const { token, header } = getCsrfData();
        return token && header ? { [header]: token } : {};
    };

    const showWarningToast = (title) => {
        if (window.Swal) {
            window.Swal.fire({
                toast: true,
                position: 'bottom-end',
                icon: 'warning',
                title: title,
                showConfirmButton: false,
                timer: 3500,
                timerProgressBar: true
            });
        } else {
            alert(title);
        }
    };

    const formatBytes = (bytes) => {
        if (!bytes || bytes < 1024) return (bytes || 0) + ' B';
        const k = 1024;
        const sizes = ['KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k)) - 1;
        return (bytes / Math.pow(k, i + 1)).toFixed(1) + ' ' + sizes[i];
    };

    const formatTime = (isoString) => {
        if (!isoString) return '';
        const d = new Date(isoString);
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const getDockContainer = () => {
        let container = document.getElementById('chatDock');
        if (!container) {
            container = document.createElement('div');
            container.id = 'chatDock';
            container.className = 'chat-dock';
            document.body.appendChild(container);
        }
        return container;
    };

    const EMOJIS = ['😀', '😂', '🤣', '😊', '😍', '😎', '🤔', '😭', '😅', '👍', '👏', '🎉', '🔥', '❤️', '🙏', '🤝', '🚀', '💡', '🎯', '✅'];
    const GIFS = [
        '3o6Zt481isNVuQI1l6', '26ufdipQqU2lhNA4g', '3o7TKsQ8UQ2p0f9V8A', '3o6Zt6ML6BklcajjsA',
        '3o7btPCcdNniyf0ArS', '3o6Zt4HU9uwXmXSAuI', 'l0MYC0LajbaPoEADu', 'ICOgUNjpvO0PC',
        'xT9IgG50Fb7Mi0prBC', 'l0MYt5jPR6QX5pnqM', '3oEjHI8WJv4x6UPDB6', '26n6WywJyh39n1pBu',
        '3oKIPwoeGErMmaI43S', '5GoVLqeAOo6PK', '13CoXDiaCcCoyk', '2A75RyXVzzSI2bx4Gj',
        '9Ai5dIk8xvBm0', '3oEduSbSGpGaRX2Vri', '12XDYvMJNcmLgQ', '3ohs4w0OrUm5GIkBKE'
    ];

    const STORAGE_KEY = 'postera_chat_dock_state';

    const saveStorageState = () => {
        try {
            const list = [];
            activeChats.forEach((state, friendId) => {
                list.push({
                    friend: state.friend,
                    isMinimized: state.element.classList.contains('is-minimized')
                });
            });
            localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
        } catch (e) {
            console.warn('ChatDock save state error:', e);
        }
    };

    const restoreFromStorage = () => {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) return;
            const list = JSON.parse(raw);
            if (Array.isArray(list)) {
                list.slice(0, MAX_CHATS).forEach(item => {
                    if (item && item.friend && item.friend.id) {
                        openChat(item.friend, item.isMinimized === true, true);
                    }
                });
            }
        } catch (e) {
            console.warn('ChatDock restore state error:', e);
        }
    };

    const openChat = (friend, startMinimized = false, isRestoring = false) => {
        const friendId = Number(friend.id);
        const container = getDockContainer();

        // 1. Zaten açıksa odaklan ve küçültülmüşse büyüt (kullanıcı tıkladığında)
        if (activeChats.has(friendId)) {
            const chat = activeChats.get(friendId);
            if (!isRestoring) {
                chat.element.classList.remove('is-minimized');
                chat.minimizeBtn.setAttribute('title', 'Küçült');
                chat.minimizeBtn.querySelector('.material-symbols-outlined').textContent = 'remove';
                chat.editor.focus();
                saveStorageState();
            }
            return;
        }

        // 2. Maksimum 3 pencere kontrolü
        if (activeChats.size >= MAX_CHATS) {
            if (!isRestoring) {
                showWarningToast('En fazla 3 sohbet penceresi açabilirsiniz.');
            }
            return;
        }

        // 3. Yeni mini chat penceresi oluştur
        const chatBox = document.createElement('div');
        chatBox.className = 'chat-box' + (startMinimized ? ' is-minimized' : '');
        chatBox.setAttribute('data-chat-friend-id', friendId);

        chatBox.innerHTML = `
            <div class="chat-box-header">
                <div class="chat-box-friend-meta">
                    <div class="chat-box-avatar-wrapper">
                        <img src="${friend.profileImageUrl || '/images/default-avatar.svg'}" class="chat-box-avatar" alt="${friend.fullName}">
                        <div class="chat-box-status-dot ${friend.isOnline ? 'online' : 'offline'}"></div>
                    </div>
                    <div class="chat-box-friend-details">
                        <span class="chat-box-name" title="${friend.fullName}">${friend.fullName}</span>
                        <span class="chat-box-presence">${friend.isOnline ? friend.presenceStatusLabel : 'Çevrimdışı'}</span>
                    </div>
                </div>
                <div class="chat-box-header-actions">
                    <button type="button" class="chat-btn-icon chat-btn-minimize" title="${startMinimized ? 'Büyüt' : 'Küçült'}" aria-label="${startMinimized ? 'Büyüt' : 'Küçült'}">
                        <span class="material-symbols-outlined">${startMinimized ? 'check_box_outline_blank' : 'remove'}</span>
                    </button>
                    <button type="button" class="chat-btn-icon chat-btn-close" title="Kapat" aria-label="Kapat">
                        <span class="material-symbols-outlined">close</span>
                    </button>
                </div>
            </div>

            <div class="chat-box-body">
                <div class="chat-loading-spinner" style="display: flex;">
                    <span class="material-symbols-outlined chat-spin">progress_activity</span>
                    <span>Sohbet yükleniyor...</span>
                </div>
                <div class="chat-messages-container" style="display: none;"></div>
                <div class="chat-typing-row" style="display: none;">
                    <div class="chat-typing-bubble">
                        <span class="chat-typing-dot"></span>
                        <span class="chat-typing-dot"></span>
                        <span class="chat-typing-dot"></span>
                    </div>
                    <span class="chat-typing-text">${friend.fullName} yazıyor...</span>
                </div>
            </div>

            <div class="chat-box-footer">
                <div class="chat-toolbar" role="toolbar">
                    <button type="button" class="chat-tool-btn" data-chat-cmd="bold" title="Kalın"><strong>B</strong></button>
                    <button type="button" class="chat-tool-btn" data-chat-cmd="italic" title="İtalik"><em>I</em></button>
                    <button type="button" class="chat-tool-btn" data-chat-cmd="insertUnorderedList" title="Madde işaretli liste"><span class="material-symbols-outlined">format_list_bulleted</span></button>
                    <button type="button" class="chat-tool-btn" data-chat-cmd="insertOrderedList" title="Numaralı liste"><span class="material-symbols-outlined">format_list_numbered</span></button>
                    <button type="button" class="chat-tool-btn" data-chat-popover-toggle="emoji" title="Emoji Ekle">😊</button>
                    <button type="button" class="chat-tool-btn" data-chat-popover-toggle="gif" title="GIF Ekle"><span class="material-symbols-outlined">gif_box</span></button>
                    <button type="button" class="chat-tool-btn" data-chat-popover-toggle="link" title="Bağlantı Ekle"><span class="material-symbols-outlined">link</span></button>
                    <button type="button" class="chat-tool-btn" data-chat-cmd="removeFormat" title="Biçimlendirmeyi kaldır"><span class="material-symbols-outlined">format_clear</span></button>
                    
                    <label class="chat-tool-btn chat-file-label" title="Görsel Ekle (En fazla 2, 5MB)">
                        <span class="material-symbols-outlined">image</span>
                        <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" multiple class="chat-input-images" style="display: none !important;">
                    </label>

                    <label class="chat-tool-btn chat-file-label" title="Dosya Ekle (En fazla 1, 25MB)">
                        <span class="material-symbols-outlined">attach_file</span>
                        <input type="file" class="chat-input-file" style="display: none !important;">
                    </label>

                    <!-- Popovers -->
                    <div class="chat-popover chat-emoji-popover" style="display: none;">
                        <div class="chat-emoji-grid">
                            ${EMOJIS.map(em => `<button type="button" class="chat-emoji-btn" data-emoji="${em}">${em}</button>`).join('')}
                        </div>
                    </div>

                    <div class="chat-popover chat-gif-popover" style="display: none;">
                        <div class="chat-gif-grid">
                            ${GIFS.map(g => `<button type="button" class="chat-gif-btn" data-gif="https://media.giphy.com/media/${g}/giphy.gif"><img src="https://media.giphy.com/media/${g}/giphy.gif" alt="GIF"></button>`).join('')}
                        </div>
                    </div>

                    <div class="chat-popover chat-link-popover" style="display: none;">
                        <input type="url" class="chat-link-input" placeholder="https://ornek.com">
                        <button type="button" class="chat-link-submit-btn">Ekle</button>
                    </div>
                </div>

                <!-- Previews -->
                <div class="chat-previews">
                    <div class="chat-image-previews" style="display: none;"></div>
                    <div class="chat-file-preview" style="display: none;">
                        <span class="material-symbols-outlined chat-file-icon">description</span>
                        <div class="chat-file-info">
                            <input type="text" class="chat-file-alias-input" placeholder="Dosya takma adı (isteğe bağlı)">
                            <small class="chat-file-size"></small>
                        </div>
                        <button type="button" class="chat-remove-file-btn" title="Kaldır"><span class="material-symbols-outlined">close</span></button>
                    </div>
                </div>

                <div class="chat-composer-row">
                    <div class="chat-editor" contenteditable="true" role="textbox" aria-multiline="true" data-placeholder="${friend.fullName} ile mesajlaşın..."></div>
                    <button type="button" class="chat-btn-send" title="Gönder">
                        <span class="material-symbols-outlined">send</span>
                    </button>
                </div>
            </div>
        `;

        container.appendChild(chatBox);

        // Elements
        const header = chatBox.querySelector('.chat-box-header');
        const minimizeBtn = chatBox.querySelector('.chat-btn-minimize');
        const closeBtn = chatBox.querySelector('.chat-btn-close');
        const bodyElem = chatBox.querySelector('.chat-box-body');
        const spinner = chatBox.querySelector('.chat-loading-spinner');
        const messagesContainer = chatBox.querySelector('.chat-messages-container');
        const typingRow = chatBox.querySelector('.chat-typing-row');
        const editor = chatBox.querySelector('.chat-editor');
        const sendBtn = chatBox.querySelector('.chat-btn-send');
        const imageInput = chatBox.querySelector('.chat-input-images');
        const fileInput = chatBox.querySelector('.chat-input-file');
        const imagePreviews = chatBox.querySelector('.chat-image-previews');
        const filePreview = chatBox.querySelector('.chat-file-preview');
        const fileAliasInput = chatBox.querySelector('.chat-file-alias-input');
        const fileSizeText = chatBox.querySelector('.chat-file-size');
        const fileRemoveBtn = chatBox.querySelector('.chat-remove-file-btn');

        // Selection & Range for editor
        let savedRange = null;
        const saveSelection = () => {
            const sel = window.getSelection();
            if (sel.rangeCount && editor.contains(sel.anchorNode)) {
                savedRange = sel.getRangeAt(0).cloneRange();
            }
        };
        const restoreSelection = () => {
            if (!savedRange) {
                editor.focus();
                return;
            }
            const sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(savedRange);
        };

        editor.addEventListener('keyup', saveSelection);
        editor.addEventListener('mouseup', saveSelection);
        editor.addEventListener('blur', saveSelection);

        // File states
        let selectedImages = [];
        let selectedFile = null;

        // Minimize / Restore
        const toggleMinimize = (e) => {
            if (e) e.stopPropagation();
            const isMin = chatBox.classList.toggle('is-minimized');
            const icon = minimizeBtn.querySelector('.material-symbols-outlined');
            if (isMin) {
                icon.textContent = 'check_box_outline_blank';
                minimizeBtn.setAttribute('title', 'Büyüt');
            } else {
                icon.textContent = 'remove';
                minimizeBtn.setAttribute('title', 'Küçült');
                scrollToBottom();
                editor.focus();
                markAsRead();
            }
            saveStorageState();
        };

        header.addEventListener('click', (e) => {
            if (!e.target.closest('.chat-btn-icon')) {
                toggleMinimize();
            }
        });
        minimizeBtn.addEventListener('click', toggleMinimize);

        // Close
        const closeChat = (e) => {
            if (e) e.stopPropagation();
            if (chatState.pollTimer) clearInterval(chatState.pollTimer);
            if (stopTypingTimer) clearTimeout(stopTypingTimer);
            if (isTypingSent) {
                isTypingSent = false;
                sendTypingStatus(false);
            }
            chatBox.remove();
            activeChats.delete(friendId);
            saveStorageState();
        };
        closeBtn.addEventListener('click', closeChat);

        // Popovers
        const closeAllPopovers = () => {
            chatBox.querySelectorAll('.chat-popover').forEach(p => {
                p.style.display = 'none';
            });
        };

        chatBox.querySelectorAll('[data-chat-popover-toggle]').forEach(btn => {
            btn.addEventListener('mousedown', (e) => {
                e.preventDefault();
                saveSelection();
                const type = btn.getAttribute('data-chat-popover-toggle');
                const target = chatBox.querySelector(`.chat-${type}-popover`);
                if (!target) return;
                const isCurrentlyOpen = target.style.display === 'flex' || target.style.display === 'block';
                closeAllPopovers();
                if (!isCurrentlyOpen) {
                    target.style.display = (type === 'link' || type === 'emoji') ? 'flex' : 'block';
                    if (type === 'link') {
                        setTimeout(() => target.querySelector('.chat-link-input')?.focus(), 50);
                    }
                }
            });
        });

        // Close popovers when clicking outside
        document.addEventListener('mousedown', (e) => {
            if (!chatBox.contains(e.target)) {
                closeAllPopovers();
            } else if (!e.target.closest('.chat-popover') && !e.target.closest('[data-chat-popover-toggle]')) {
                closeAllPopovers();
            }
        });

        // Toolbar formatting commands
        chatBox.querySelectorAll('[data-chat-cmd]').forEach(btn => {
            btn.addEventListener('mousedown', (e) => {
                e.preventDefault();
                const cmd = btn.getAttribute('data-chat-cmd');
                document.execCommand(cmd, false, null);
                editor.focus();
            });
        });

        // Emoji click
        chatBox.querySelectorAll('.chat-emoji-btn').forEach(btn => {
            btn.addEventListener('mousedown', (e) => {
                e.preventDefault();
                restoreSelection();
                document.execCommand('insertText', false, btn.getAttribute('data-emoji'));
                closeAllPopovers();
                editor.focus();
            });
        });

        // GIF click
        chatBox.querySelectorAll('.chat-gif-btn').forEach(btn => {
            btn.addEventListener('mousedown', (e) => {
                e.preventDefault();
                restoreSelection();
                const gifUrl = btn.getAttribute('data-gif');
                document.execCommand('insertHTML', false, `<img class="rich-gif" src="${gifUrl}" alt="GIF">`);
                closeAllPopovers();
                editor.focus();
            });
        });

        // Link submit
        const linkInput = chatBox.querySelector('.chat-link-input');
        const linkBtn = chatBox.querySelector('.chat-link-submit-btn');
        linkBtn.addEventListener('mousedown', (e) => {
            e.preventDefault();
            const url = linkInput.value.trim();
            if (!/^https?:\/\//i.test(url)) return;
            restoreSelection();
            const selText = window.getSelection().toString();
            if (selText) {
                document.execCommand('createLink', false, url);
            } else {
                document.execCommand('insertHTML', false, `<a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>`);
            }
            linkInput.value = '';
            closeAllPopovers();
            editor.focus();
        });

        // Image Selection
        imageInput.addEventListener('change', () => {
            const files = Array.from(imageInput.files || []);
            if (files.length === 0) return;

            if (selectedImages.length + files.length > 2) {
                showWarningToast('En fazla 2 görsel ekleyebilirsiniz.');
                imageInput.value = '';
                return;
            }

            for (const f of files) {
                if (f.size > 5 * 1024 * 1024) {
                    showWarningToast('Her görsel en fazla 5 MB olabilir.');
                    imageInput.value = '';
                    return;
                }
            }

            selectedImages = selectedImages.concat(files).slice(0, 2);
            imageInput.value = '';
            renderImagePreviews();
        });

        const renderImagePreviews = () => {
            imagePreviews.innerHTML = '';
            if (selectedImages.length === 0) {
                imagePreviews.style.display = 'none';
                return;
            }
            imagePreviews.style.display = 'flex';

            selectedImages.forEach((imgFile, index) => {
                const item = document.createElement('div');
                item.className = 'chat-preview-thumb';
                const img = document.createElement('img');
                img.src = URL.createObjectURL(imgFile);
                const remBtn = document.createElement('button');
                remBtn.type = 'button';
                remBtn.className = 'chat-thumb-remove';
                remBtn.innerHTML = '<span class="material-symbols-outlined">close</span>';
                remBtn.addEventListener('click', () => {
                    selectedImages.splice(index, 1);
                    renderImagePreviews();
                });
                item.appendChild(img);
                item.appendChild(remBtn);
                imagePreviews.appendChild(item);
            });
        };

        // File Selection
        fileInput.addEventListener('change', () => {
            const file = fileInput.files?.[0];
            if (!file) return;

            if (file.size > 25 * 1024 * 1024) {
                showWarningToast('Dosya boyutu en fazla 25 MB olabilir.');
                fileInput.value = '';
                return;
            }

            selectedFile = file;
            filePreview.style.display = 'flex';
            fileAliasInput.value = file.name;
            fileSizeText.textContent = formatBytes(file.size);
            fileInput.value = '';
        });

        fileRemoveBtn.addEventListener('click', () => {
            selectedFile = null;
            filePreview.style.display = 'none';
            fileAliasInput.value = '';
            fileSizeText.textContent = '';
        });

        // Messages rendering
        let lastRenderedMessages = [];

        const scrollToBottom = () => {
            bodyElem.scrollTop = bodyElem.scrollHeight;
        };

        const renderMessages = (messages) => {
            lastRenderedMessages = messages || [];
            if (messages.length === 0) {
                messagesContainer.innerHTML = `
                    <div class="chat-empty-state">
                        <span class="material-symbols-outlined">waving_hand</span>
                        <p>${friend.fullName} ile henüz bir mesajınız yok.<br>İlk mesajı siz gönderin!</p>
                    </div>
                `;
            } else {
                messagesContainer.innerHTML = messages.map(m => {
                    const isOwn = m.sentByCurrentUser;
                    const avatar = isOwn ? '' : `<img src="${m.senderProfileImageUrl || '/images/default-avatar.svg'}" class="chat-msg-avatar" alt="Avatar">`;
                    const imagesHtml = (m.imageUrls && m.imageUrls.length > 0)
                        ? `<div class="chat-bubble-images">${m.imageUrls.map(url => `<a href="${url}" target="_blank"><img src="${url}" alt="Resim"></a>`).join('')}</div>`
                        : '';
                    const attachHtml = m.attachment
                        ? `<a class="chat-bubble-attachment" href="/messages/${m.id}/attachments/${m.attachment.id}" target="_blank">
                                <span class="material-symbols-outlined">attach_file</span>
                                <div class="chat-attach-meta">
                                    <span class="chat-attach-name">${m.attachment.alias || m.attachment.originalName}</span>
                                    <small>${m.attachment.formattedSize}</small>
                                </div>
                                <span class="material-symbols-outlined chat-attach-dl">download</span>
                           </a>`
                        : '';

                    const tickHtml = isOwn
                        ? `<span class="chat-status-tick ${m.read ? 'is-read' : 'is-sent'}" title="${m.read ? 'Görüldü' : 'İletildi'}">
                            <span class="material-symbols-outlined">${m.read ? 'done_all' : 'done'}</span>
                           </span>`
                        : '';

                    return `
                        <div class="chat-message-row ${isOwn ? 'is-own' : 'is-peer'}">
                            ${!isOwn ? avatar : ''}
                            <div class="chat-bubble ${isOwn ? 'bubble-own' : 'bubble-peer'}">
                                ${m.body ? `<div class="chat-bubble-text">${m.body}</div>` : ''}
                                ${imagesHtml}
                                ${attachHtml}
                                <div class="chat-bubble-footer">
                                    <span class="chat-bubble-time">${formatTime(m.sentAt)}</span>
                                    ${tickHtml}
                                </div>
                            </div>
                        </div>
                    `;
                }).join('');
            }
            spinner.style.display = 'none';
            messagesContainer.style.display = 'flex';
            scrollToBottom();
        };

        // Mark as Read
        function markAsRead() {
            if (chatBox.classList.contains('is-minimized')) return;
            const { token, header } = getCsrfData();
            const reqHeaders = {
                'Content-Type': 'application/x-www-form-urlencoded',
                ...(token && header ? { [header]: token } : {})
            };
            const body = new URLSearchParams({ friendId: friendId });
            if (token) body.append('_csrf', token);
            fetch('/api/chat/read', {
                method: 'POST',
                headers: reqHeaders,
                body: body.toString()
            }).catch(e => console.warn('markAsRead error:', e));
        }

        // Typing Status Tracking
        let isTypingSent = false;
        let stopTypingTimer = null;

        function sendTypingStatus(typing) {
            const { token, header } = getCsrfData();
            const reqHeaders = {
                'Content-Type': 'application/x-www-form-urlencoded',
                ...(token && header ? { [header]: token } : {})
            };
            const body = new URLSearchParams({ friendId: friendId, isTyping: typing });
            if (token) body.append('_csrf', token);
            fetch('/api/chat/typing', {
                method: 'POST',
                headers: reqHeaders,
                body: body.toString()
            }).catch(() => {});
        };

        editor.addEventListener('input', () => {
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

        editor.addEventListener('focus', () => {
            markAsRead();
        });

        // Fetch History
        const fetchHistory = (silent = false) => {
            fetch(`/api/chat/history?friendId=${friendId}`, {
                headers: { 'Accept': 'application/json' }
            })
            .then(res => {
                if (!res.ok) throw new Error('Sunucu yanıt vermedi: ' + res.status);
                return res.json();
            })
            .then(data => {
                if (data && data.messages) {
                    const messagesChanged = !silent
                        || data.messages.length !== lastRenderedMessages.length
                        || JSON.stringify(data.messages.map(m => [m.id, m.read])) !== JSON.stringify(lastRenderedMessages.map(m => [m.id, m.read]));

                    if (messagesChanged) {
                        renderMessages(data.messages);
                    }

                    // Typing indicator & Presence update
                    const dot = chatBox.querySelector('.chat-box-status-dot');
                    const presence = chatBox.querySelector('.chat-box-presence');
                    const isTyping = data.isPeerTyping === true;

                    if (typingRow) {
                        const wasHidden = typingRow.style.display === 'none';
                        typingRow.style.display = isTyping ? 'flex' : 'none';
                        if (isTyping && wasHidden) scrollToBottom();
                    }

                    if (dot && presence) {
                        dot.className = `chat-box-status-dot ${data.isOnline ? 'online' : 'offline'}`;
                        presence.textContent = isTyping ? 'Yazıyor...' : (data.isOnline ? data.presenceStatusLabel : 'Çevrimdışı');
                    }

                    if (!chatBox.classList.contains('is-minimized')) {
                        const hasUnread = data.messages.some(m => !m.sentByCurrentUser && !m.read);
                        if (hasUnread) {
                            markAsRead();
                        }
                    }
                } else {
                    renderMessages([]);
                }
            })
            .catch(err => {
                if (!silent) {
                    console.error('Chat history fetch error:', err);
                    spinner.style.display = 'none';
                    messagesContainer.style.display = 'flex';
                    messagesContainer.innerHTML = `
                        <div class="chat-empty-state">
                            <span class="material-symbols-outlined">chat_error</span>
                            <p>Sohbet geçmişi yüklenemedi.<br><small>${err.message || ''}</small></p>
                        </div>
                    `;
                }
            });
        };

        // Send Message
        const handleSendMessage = () => {
            const bodyHtml = editor.innerHTML.trim();
            const textContent = editor.textContent.trim();
            const hasImages = selectedImages.length > 0;
            const hasFile = selectedFile !== null;

            if (!textContent && !hasImages && !hasFile && !bodyHtml.includes('<img')) {
                return;
            }

            if (stopTypingTimer) clearTimeout(stopTypingTimer);
            if (isTypingSent) {
                isTypingSent = false;
                sendTypingStatus(false);
            }

            sendBtn.disabled = true;
            const formData = new FormData();
            formData.append('friendId', friendId);
            formData.append('body', bodyHtml);

            selectedImages.forEach(img => formData.append('images', img));
            if (hasFile) {
                formData.append('file', selectedFile);
                formData.append('fileAlias', fileAliasInput.value.trim() || selectedFile.name);
            }

            const { token, header } = getCsrfData();
            if (token) {
                formData.append('_csrf', token);
            }

            const reqHeaders = token && header ? { [header]: token } : {};

            fetch('/api/chat/send', {
                method: 'POST',
                headers: reqHeaders,
                body: formData
            })
            .then(async (res) => {
                if (!res.ok) {
                    let errMsg = 'Mesaj gönderilemedi.';
                    try {
                        const errObj = await res.json();
                        errMsg = errObj.message || errMsg;
                    } catch (_) {
                        if (res.status === 403) errMsg = 'Yetkisiz erişim veya CSRF hatası (403).';
                        else if (res.status === 401) errMsg = 'Oturum süreniz doldu, lütfen tekrar giriş yapın.';
                    }
                    throw new Error(errMsg);
                }
                return res.json();
            })
            .then(newMsg => {
                editor.innerHTML = '';
                selectedImages = [];
                renderImagePreviews();
                selectedFile = null;
                filePreview.style.display = 'none';
                fileAliasInput.value = '';
                fileSizeText.textContent = '';
                
                // Mesajı listeye hemen ekle
                const updated = lastRenderedMessages.concat([newMsg]);
                renderMessages(updated);
            })
            .catch(err => {
                showWarningToast(err.message || 'Mesaj gönderilemedi.');
            })
            .finally(() => {
                sendBtn.disabled = false;
                editor.focus();
            });
        };

        sendBtn.addEventListener('click', handleSendMessage);

        editor.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSendMessage();
            }
        });

        // Initialize state
        const chatState = {
            element: chatBox,
            minimizeBtn,
            editor,
            friend,
            pollTimer: setInterval(() => fetchHistory(true), 4000)
        };
        activeChats.set(friendId, chatState);

        // First fetch
        fetchHistory(false);
        if (!startMinimized && !isRestoring) {
            setTimeout(() => editor.focus(), 150);
        }

        saveStorageState();
    };

    // Restore previously opened chats on page load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', restoreFromStorage);
    } else {
        restoreFromStorage();
    }

    // Public API on window
    window.ChatDock = {
        openChat,
        getActiveCount: () => activeChats.size
    };
})();

