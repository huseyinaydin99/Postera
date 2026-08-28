(() => {
    const csrfHeaders = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    };

    const escHtml = (s) => String(s ?? '')
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#039;');

    const formatDate = (iso) => {
        if (!iso) return '';
        const d = new Date(iso);
        if (isNaN(d)) return '';
        const now = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
        if (d.toDateString() === now.toDateString()) return hm;
        const yest = new Date(now); yest.setDate(now.getDate() - 1);
        if (d.toDateString() === yest.toDateString()) return `Dün ${hm}`;
        const months = ['Oca','Şub','Mar','Nis','May','Haz','Tem','Ağu','Eyl','Eki','Kas','Ara'];
        return `${pad(d.getDate())} ${months[d.getMonth()]} ${hm}`;
    };

    const highlightMentions = (text) =>
        escHtml(text).replace(/@([\w.\-]+(?: [\w.\-]+)?)/g,
            '<span class="comment-mention">@$1</span>');

    // ── Friends cache for mention autocomplete ────────────────────────────────
    let friendsCache = null;
    const loadFriends = async () => {
        if (friendsCache) return friendsCache;
        try {
            const res = await fetch('/friends/api/list', { headers: { Accept: 'application/json' } });
            friendsCache = res.ok ? await res.json() : [];
        } catch { friendsCache = []; }
        return friendsCache;
    };

    // ── Mention autocomplete ──────────────────────────────────────────────────
    const setupMentionAutocomplete = (textarea, suggestionsEl) => {
        let activeIndex = -1;
        let mentionStart = -1;

        const close = () => {
            suggestionsEl.innerHTML = '';
            suggestionsEl.classList.remove('is-open');
            activeIndex = -1;
            mentionStart = -1;
        };

        const pick = (friend) => {
            const val = textarea.value;
            const before = val.slice(0, mentionStart);
            const after = val.slice(textarea.selectionStart);
            textarea.value = before + '@' + friend.name + ' ' + after;
            textarea.selectionStart = textarea.selectionEnd = (before + '@' + friend.name + ' ').length;
            close();
            textarea.focus();
        };

        textarea.addEventListener('input', async () => {
            const val = textarea.value;
            const pos = textarea.selectionStart;
            const textBefore = val.slice(0, pos);
            const match = textBefore.match(/@([\w.\- ]*)$/);
            if (!match) { close(); return; }
            mentionStart = textBefore.lastIndexOf('@');
            const query = match[1].toLowerCase();
            const friends = await loadFriends();
            const filtered = friends.filter(f =>
                f.name.toLowerCase().includes(query) || f.email.toLowerCase().includes(query)
            ).slice(0, 6);
            if (!filtered.length) { close(); return; }
            activeIndex = -1;
            suggestionsEl.innerHTML = filtered.map((f, i) => `
                <div class="mention-suggestion-item" data-idx="${i}">
                    <img src="${escHtml(f.profileImageUrl || '/images/default-avatar.svg')}" alt="">
                    <div style="display:flex;flex-direction:column;min-width:0">
                        <span>${escHtml(f.name)}</span>
                        <small>${escHtml(f.email)}</small>
                    </div>
                </div>`).join('');
            suggestionsEl._friends = filtered;
            suggestionsEl.classList.add('is-open');
        });

        textarea.addEventListener('keydown', (e) => {
            if (!suggestionsEl.classList.contains('is-open')) return;
            const items = suggestionsEl.querySelectorAll('.mention-suggestion-item');
            if (e.key === 'ArrowDown') { e.preventDefault(); activeIndex = Math.min(activeIndex + 1, items.length - 1); }
            else if (e.key === 'ArrowUp') { e.preventDefault(); activeIndex = Math.max(activeIndex - 1, 0); }
            else if (e.key === 'Enter' || e.key === 'Tab') {
                if (activeIndex >= 0) { e.preventDefault(); pick(suggestionsEl._friends[activeIndex]); return; }
            } else if (e.key === 'Escape') { close(); return; }
            items.forEach((el, i) => el.classList.toggle('is-active', i === activeIndex));
        });

        suggestionsEl.addEventListener('mousedown', (e) => {
            const item = e.target.closest('.mention-suggestion-item');
            if (!item) return;
            e.preventDefault();
            pick(suggestionsEl._friends[parseInt(item.dataset.idx, 10)]);
        });

        document.addEventListener('click', (e) => {
            if (!textarea.contains(e.target) && !suggestionsEl.contains(e.target)) close();
        });
    };

    // ── Render helpers ────────────────────────────────────────────────────────
    const currentUserAvatar = () =>
        document.querySelector('.composer-avatar')?.src || '/images/default-avatar.svg';

    const buildCommentEl = (c, isReply = false) => {
        const div = document.createElement('div');
        div.className = `comment-item${isReply ? ' is-reply' : ''}`;
        div.dataset.commentId = c.id;
        div.innerHTML = `
            <img class="comment-avatar" src="${escHtml(c.authorProfileImageUrl || '/images/default-avatar.svg')}" alt="${escHtml(c.authorName)}">
            <div class="comment-body">
                <div class="comment-bubble">
                    <strong>${escHtml(c.authorName)}</strong>
                    ${highlightMentions(c.content)}
                </div>
                <div class="comment-meta">
                    <time class="comment-time">${formatDate(c.createdAt)}</time>
                    <button type="button" class="comment-reply-btn" data-reply-to="${c.id}" data-reply-author="${escHtml(c.authorName)}">Cevapla</button>
                    ${c.ownedByCurrentUser ? `<button type="button" class="comment-delete-btn" data-delete-comment="${c.id}">Sil</button>` : ''}
                </div>
            </div>`;
        return div;
    };

    const buildInputRow = (parentId, placeholder, avatarSrc) => {
        const row = document.createElement('div');
        row.className = `comment-input-row${parentId ? ' is-reply' : ''}`;
        row.dataset.inputRow = parentId ? String(parentId) : 'root';
        row.innerHTML = `
            <img class="comment-input-avatar" src="${escHtml(avatarSrc)}" alt="">
            <div class="comment-input-wrap">
                <textarea class="comment-input" rows="1" placeholder="${escHtml(placeholder)}" data-comment-input></textarea>
                <button type="button" class="comment-send-btn" data-send-comment title="Gönder">
                    <span class="material-symbols-outlined">send</span>
                </button>
                <div class="comment-mention-suggestions"></div>
            </div>
            ${parentId ? `<button type="button" class="comment-cancel-reply" data-cancel-reply>İptal</button>` : ''}`;
        const ta = row.querySelector('[data-comment-input]');
        const sugg = row.querySelector('.comment-mention-suggestions');
        setupMentionAutocomplete(ta, sugg);
        ta.addEventListener('input', () => {
            ta.style.height = 'auto';
            ta.style.height = Math.min(ta.scrollHeight, 120) + 'px';
        });
        return row;
    };

    // ── Per-post comment section ──────────────────────────────────────────────
    const initCommentSection = (section) => {
        const postId = section.dataset.postId;
        // data-comments-list is a child element inside the section
        const list = section.querySelector('[data-comments-list]');
        const countEl = section.closest('.timeline-post-card')?.querySelector('[data-comment-count]');
        let loaded = false;
        let replyRow = null;

        const updateCount = (delta) => {
            if (!countEl) return;
            const n = Math.max(0, (parseInt(countEl.textContent, 10) || 0) + delta);
            countEl.textContent = n;
        };

        const removeReplyRow = () => {
            if (replyRow) { replyRow.remove(); replyRow = null; }
        };

        const submitComment = async (content, parentId, inputRow) => {
            const ta = inputRow.querySelector('[data-comment-input]');
            const btn = inputRow.querySelector('[data-send-comment]');
            btn.disabled = true;
            try {
                const body = new URLSearchParams({ content });
                if (parentId) body.set('parentId', parentId);
                const res = await fetch(`/api/posts/${postId}/comments`, {
                    method: 'POST',
                    headers: { ...csrfHeaders(), 'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json' },
                    body
                });
                if (!res.ok) throw new Error();
                const c = await res.json();
                const el = buildCommentEl(c, !!parentId);
                if (parentId) {
                    // Insert after last reply of this parent
                    const parentEl = list.querySelector(`[data-comment-id="${parentId}"]`);
                    let insertAfter = parentEl;
                    let next = parentEl?.nextElementSibling;
                    while (next && next.classList.contains('is-reply') && !next.dataset.inputRow) {
                        insertAfter = next; next = next.nextElementSibling;
                    }
                    if (insertAfter) insertAfter.after(el);
                    else list.appendChild(el);
                    removeReplyRow();
                } else {
                    list.appendChild(el);
                }
                ta.value = ''; ta.style.height = 'auto';
                updateCount(1);
                // Remove empty state if present
                list.querySelector('[data-empty-state]')?.remove();
            } catch {
                window.PosteraAlerts?.toast?.('Yorum gönderilemedi.', 'error');
            } finally {
                btn.disabled = false;
            }
        };

        const loadComments = async () => {
            if (loaded) return;
            loaded = true;
            list.innerHTML = '<span class="comment-loading">Yükleniyor…</span>';
            try {
                const res = await fetch(`/api/posts/${postId}/comments`, { headers: { Accept: 'application/json' } });
                const comments = res.ok ? await res.json() : [];
                list.innerHTML = '';
                if (!comments.length) {
                    const empty = document.createElement('span');
                    empty.className = 'comment-empty-state';
                    empty.dataset.emptyState = '';
                    empty.textContent = 'Henüz yorum yok. İlk yorumu sen yap!';
                    list.appendChild(empty);
                } else {
                    comments.forEach(c => {
                        list.appendChild(buildCommentEl(c, false));
                        (c.replies || []).forEach(r => list.appendChild(buildCommentEl(r, true)));
                    });
                }
            } catch {
                list.innerHTML = '<span style="color:var(--danger);font-size:.78rem;padding:.3rem 0;display:block">Yorumlar yüklenemedi.</span>';
            }
        };

        // Root input row — appended after the list
        const rootRow = buildInputRow(null, 'Yorum yaz…', currentUserAvatar());
        section.appendChild(rootRow);

        rootRow.querySelector('[data-send-comment]').addEventListener('click', () => {
            const val = rootRow.querySelector('[data-comment-input]').value.trim();
            if (val) submitComment(val, null, rootRow);
        });
        rootRow.querySelector('[data-comment-input]').addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                const val = e.target.value.trim();
                if (val) submitComment(val, null, rootRow);
            }
        });

        // Delegation: reply / delete on the list
        list.addEventListener('click', async (e) => {
            // Reply
            const replyBtn = e.target.closest('[data-reply-to]');
            if (replyBtn) {
                removeReplyRow();
                const parentId = replyBtn.dataset.replyTo;
                const authorName = replyBtn.dataset.replyAuthor;
                replyRow = buildInputRow(parentId, `@${authorName} cevapla…`, currentUserAvatar());

                const parentEl = list.querySelector(`[data-comment-id="${parentId}"]`);
                let insertAfter = parentEl;
                let next = parentEl?.nextElementSibling;
                while (next && next.classList.contains('is-reply') && !next.dataset.inputRow) {
                    insertAfter = next; next = next.nextElementSibling;
                }
                if (insertAfter) insertAfter.after(replyRow);
                else list.appendChild(replyRow);

                const ta = replyRow.querySelector('[data-comment-input]');
                ta.value = `@${authorName} `;
                ta.focus();
                ta.selectionStart = ta.selectionEnd = ta.value.length;

                replyRow.querySelector('[data-send-comment]').addEventListener('click', () => {
                    const val = ta.value.trim();
                    if (val) submitComment(val, parentId, replyRow);
                });
                ta.addEventListener('keydown', (ev) => {
                    if (ev.key === 'Enter' && !ev.shiftKey) {
                        ev.preventDefault();
                        const val = ta.value.trim();
                        if (val) submitComment(val, parentId, replyRow);
                    }
                });
                replyRow.querySelector('[data-cancel-reply]')?.addEventListener('click', removeReplyRow);
                return;
            }

            // Delete
            const delBtn = e.target.closest('[data-delete-comment]');
            if (delBtn) {
                const commentId = delBtn.dataset.deleteComment;
                try {
                    const res = await fetch(`/api/posts/comments/${commentId}`, {
                        method: 'DELETE',
                        headers: { ...csrfHeaders(), Accept: 'application/json' }
                    });
                    if (res.ok) {
                        const el = list.querySelector(`[data-comment-id="${commentId}"]`);
                        if (el) { el.remove(); updateCount(-1); }
                    }
                } catch { /* ignore */ }
            }
        });

        section.dataset.initialized = 'true';
        section._loadComments = loadComments;
    };

    // ── Toggle button wiring ──────────────────────────────────────────────────
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-toggle-comments]');
        if (!btn) return;
        const postCard = btn.closest('.timeline-post-card');
        if (!postCard) return;
        const section = postCard.querySelector('.post-comments-section');
        if (!section) return;

        const isHidden = section.hidden;
        section.hidden = !isHidden;
        btn.classList.toggle('is-active', isHidden);

        if (isHidden) {
            if (!section.dataset.initialized) initCommentSection(section);
            section._loadComments?.();
        }
    });

    // ── Deep-link: /?comment=ID#post-POSTID ──────────────────────────────────
    // URL format: /?comment=5#post-42
    const params = new URLSearchParams(window.location.search);
    const targetCommentId = params.get('comment');
    // Hash may be #post-42 — extract from location.hash
    const hashMatch = window.location.hash.match(/^#post-(\d+)$/);

    if (targetCommentId && hashMatch) {
        const postId = hashMatch[1];
        const postCard = document.getElementById(`post-${postId}`);
        if (postCard) {
            const section = postCard.querySelector('.post-comments-section');
            const toggleBtn = postCard.querySelector('[data-toggle-comments]');
            if (section && toggleBtn) {
                section.hidden = false;
                toggleBtn.classList.add('is-active');
                if (!section.dataset.initialized) initCommentSection(section);

                const doHighlight = () => {
                    const commentEl = section.querySelector(`[data-comment-id="${targetCommentId}"]`);
                    if (commentEl) {
                        commentEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        commentEl.classList.add('comment-highlight');
                        // Auto-open reply input
                        const replyBtn = commentEl.querySelector('[data-reply-to]');
                        if (replyBtn) replyBtn.click();
                    }
                };

                const loadResult = section._loadComments?.();
                if (loadResult && typeof loadResult.then === 'function') {
                    loadResult.then(() => requestAnimationFrame(doHighlight));
                } else {
                    requestAnimationFrame(doHighlight);
                }
            }
        }
    }
})();
