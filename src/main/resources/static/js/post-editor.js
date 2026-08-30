(() => {
    const EMOJIS = '😀,😂,🤣,😊,😍,😎,🤔,😭,😅,👍,👏,🎉,🔥,❤️,🙏,🤝,🚀,💡,🎯,✅'.split(',');
    const GIFS = ['3o6Zt481isNVuQI1l6','26ufdipQqU2lhNA4g','3o7TKsQ8UQ2p0f9V8A','3o6Zt6ML6BklcajjsA','3o7btPCcdNniyf0ArS','3o6Zt4HU9uwXmXSAuI','l0MYC0LajbaPoEADu','ICOgUNjpvO0PC','xT9IgG50Fb7Mi0prBC','l0MYt5jPR6QX5pnqM'];

    // ── Build modal DOM ───────────────────────────────────────────────────────
    const backdrop = document.createElement('div');
    backdrop.className = 'ped-backdrop';

    const modal = document.createElement('div');
    modal.className = 'ped-modal';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-modal', 'true');
    modal.setAttribute('aria-label', 'Paylaşımı Düzenle');
    modal.innerHTML = `
        <div class="ped-header">
            <span class="ped-title">Paylaşımı Düzenle</span>
            <button type="button" class="ped-close" aria-label="Kapat">
                <span class="material-symbols-outlined">close</span>
            </button>
        </div>
        <div class="ped-body">
            <div class="ped-editor" contenteditable="true" role="textbox" aria-multiline="true" aria-label="Paylaşım içeriği"></div>
            <div class="ped-image-strip" hidden></div>
            <p class="ped-img-error" hidden></p>
            <div class="ped-popover ped-emoji-pop" hidden>
                ${EMOJIS.map(e => `<button type="button" class="ped-emoji-btn" data-emoji="${e}">${e}</button>`).join('')}
            </div>
            <div class="ped-popover ped-gif-pop" hidden>
                ${GIFS.map(g => `<button type="button" class="ped-gif-btn" data-gif="https://media.giphy.com/media/${g}/giphy.gif"><img src="https://media.giphy.com/media/${g}/giphy.gif" alt="GIF" loading="lazy"></button>`).join('')}
            </div>
        </div>
        <div class="ped-toolbar">
            <div class="ped-fmt-tools">
                <button type="button" class="ped-fmt-btn" data-cmd="bold" title="Kalın"><strong>B</strong></button>
                <button type="button" class="ped-fmt-btn" data-cmd="italic" title="İtalik"><em>I</em></button>
                <button type="button" class="ped-fmt-btn" data-cmd="insertUnorderedList" title="Liste"><span class="material-symbols-outlined">format_list_bulleted</span></button>
                <button type="button" class="ped-fmt-btn" data-cmd="removeFormat" title="Temizle"><span class="material-symbols-outlined">format_clear</span></button>
            </div>
            <div class="ped-media-tools">
                <label class="ped-tool-btn" title="Görsel ekle">
                    <span class="material-symbols-outlined">image</span>
                    <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" multiple hidden class="ped-file-input">
                </label>
                <button type="button" class="ped-tool-btn ped-toggle-emoji" title="Emoji">
                    <span class="material-symbols-outlined">sentiment_satisfied</span>
                </button>
                <button type="button" class="ped-tool-btn ped-toggle-gif" title="GIF">
                    <span class="material-symbols-outlined">gif_box</span>
                </button>
            </div>
            <button type="button" class="ped-save-btn">
                <span class="material-symbols-outlined">check</span> Kaydet
            </button>
        </div>`;

    document.body.appendChild(backdrop);
    document.body.appendChild(modal);

    // ── Element refs ──────────────────────────────────────────────────────────
    const editor    = modal.querySelector('.ped-editor');
    const imgStrip  = modal.querySelector('.ped-image-strip');
    const imgError  = modal.querySelector('.ped-img-error');
    const fileInput = modal.querySelector('.ped-file-input');
    const emojiPop  = modal.querySelector('.ped-emoji-pop');
    const gifPop    = modal.querySelector('.ped-gif-pop');
    const saveBtn   = modal.querySelector('.ped-save-btn');

    let currentPostId = null;
    let keepUrls = [];      // existing image URLs to keep
    let newFiles = [];      // newly selected File objects
    let savedRange = null;

    // ── Open / Close ──────────────────────────────────────────────────────────
    const openModal = () => { backdrop.classList.add('ped-visible'); modal.classList.add('ped-visible'); document.body.style.overflow = 'hidden'; };
    const closeModal = () => { backdrop.classList.remove('ped-visible'); modal.classList.remove('ped-visible'); document.body.style.overflow = ''; };

    backdrop.addEventListener('click', closeModal);
    modal.querySelector('.ped-close').addEventListener('click', closeModal);
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

    // ── Range save/restore for emoji/gif insertion ────────────────────────────
    const saveRange = () => {
        const sel = window.getSelection();
        if (sel.rangeCount && editor.contains(sel.anchorNode)) savedRange = sel.getRangeAt(0).cloneRange();
    };
    const restoreRange = () => {
        if (!savedRange) { editor.focus(); return; }
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(savedRange);
    };
    editor.addEventListener('keyup', saveRange);
    editor.addEventListener('mouseup', saveRange);

    // ── Formatting buttons ────────────────────────────────────────────────────
    modal.querySelectorAll('[data-cmd]').forEach(btn => {
        btn.addEventListener('click', () => {
            editor.focus();
            restoreRange();
            document.execCommand(btn.dataset.cmd, false, null);
        });
    });

    // ── Emoji ─────────────────────────────────────────────────────────────────
    modal.querySelector('.ped-toggle-emoji').addEventListener('click', () => {
        emojiPop.hidden = !emojiPop.hidden;
        gifPop.hidden = true;
    });
    emojiPop.addEventListener('click', e => {
        const btn = e.target.closest('.ped-emoji-btn');
        if (!btn) return;
        editor.focus();
        restoreRange();
        document.execCommand('insertText', false, btn.dataset.emoji);
        emojiPop.hidden = true;
    });

    // ── GIF ───────────────────────────────────────────────────────────────────
    modal.querySelector('.ped-toggle-gif').addEventListener('click', () => {
        gifPop.hidden = !gifPop.hidden;
        emojiPop.hidden = true;
    });
    gifPop.addEventListener('click', e => {
        const btn = e.target.closest('.ped-gif-btn');
        if (!btn) return;
        editor.focus();
        restoreRange();
        document.execCommand('insertHTML', false, `<img class="rich-gif" src="${btn.dataset.gif}" alt="GIF">`);
        gifPop.hidden = true;
    });

    // ── Image strip rendering ─────────────────────────────────────────────────
    const renderStrip = () => {
        const total = keepUrls.length + newFiles.length;
        imgStrip.hidden = total === 0;
        imgStrip.innerHTML = '';

        keepUrls.forEach((url, i) => {
            const item = document.createElement('div');
            item.className = 'ped-img-item';
            item.innerHTML = `<img src="${url}" alt="Mevcut görsel"><button type="button" class="ped-img-remove" aria-label="Görseli kaldır" data-keep-index="${i}"><span class="material-symbols-outlined">close</span></button>`;
            imgStrip.appendChild(item);
        });

        newFiles.forEach((file, i) => {
            const url = URL.createObjectURL(file);
            const item = document.createElement('div');
            item.className = 'ped-img-item';
            item.innerHTML = `<img src="${url}" alt="${file.name}"><button type="button" class="ped-img-remove" aria-label="Görseli kaldır" data-new-index="${i}"><span class="material-symbols-outlined">close</span></button>`;
            imgStrip.appendChild(item);
        });

        validateImages();
    };

    const validateImages = () => {
        const total = keepUrls.length + newFiles.length;
        const tooLarge = newFiles.some(f => f.size > 5 * 1024 * 1024);
        if (total > 2) { imgError.textContent = 'En fazla 2 görsel ekleyebilirsiniz.'; imgError.hidden = false; }
        else if (tooLarge) { imgError.textContent = 'Her görsel en fazla 5 MB olabilir.'; imgError.hidden = false; }
        else { imgError.hidden = true; }
    };

    imgStrip.addEventListener('click', e => {
        const btn = e.target.closest('.ped-img-remove');
        if (!btn) return;
        if (btn.dataset.keepIndex !== undefined) keepUrls.splice(+btn.dataset.keepIndex, 1);
        else if (btn.dataset.newIndex !== undefined) newFiles.splice(+btn.dataset.newIndex, 1);
        renderStrip();
    });

    fileInput.addEventListener('change', () => {
        newFiles = [...newFiles, ...Array.from(fileInput.files)];
        fileInput.value = '';
        renderStrip();
    });

    // ── CSRF ──────────────────────────────────────────────────────────────────
    const csrfToken  = () => document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = () => document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    // ── Load post data ────────────────────────────────────────────────────────
    const loadPost = async (postId) => {
        currentPostId = postId;
        editor.innerHTML = '';
        keepUrls = [];
        newFiles = [];
        imgError.hidden = true;
        emojiPop.hidden = true;
        gifPop.hidden = true;
        renderStrip();
        saveBtn.disabled = true;
        openModal();

        try {
            const res = await fetch(`/timeline/api/posts/${postId}`, { headers: { Accept: 'application/json' } });
            if (!res.ok) throw new Error();
            const post = await res.json();
            editor.innerHTML = post.content || '';
            keepUrls = post.imageUrls ? [...post.imageUrls] : [];
            renderStrip();
        } catch {
            window.PosteraAlerts?.toast?.('Paylaşım yüklenemedi.', 'error');
            closeModal();
        } finally {
            saveBtn.disabled = false;
        }
    };

    // ── Save ──────────────────────────────────────────────────────────────────
    saveBtn.addEventListener('click', async () => {
        const total = keepUrls.length + newFiles.length;
        if (total > 2) { validateImages(); return; }

        saveBtn.disabled = true;
        const fd = new FormData();
        fd.append('content', editor.innerHTML);
        keepUrls.forEach(u => fd.append('keepImageUrls', u));
        newFiles.forEach(f => fd.append('images', f));

        try {
            const res = await fetch(`/timeline/api/posts/${currentPostId}`, {
                method: 'PUT',
                headers: { [csrfHeader()]: csrfToken() },
                body: fd
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Kaydedilemedi.');

            updatePostCard(currentPostId, data);
            closeModal();
            window.PosteraAlerts?.toast?.('Paylaşım güncellendi.', 'success');
        } catch (err) {
            window.PosteraAlerts?.toast?.(err.message || 'Bir hata oluştu.', 'error');
        } finally {
            saveBtn.disabled = false;
        }
    });

    // ── Update post card in DOM ───────────────────────────────────────────────
    const updatePostCard = (postId, post) => {
        const card = document.getElementById(`post-${postId}`);
        if (!card) return;

        // content
        let bodyEl = card.querySelector('.post-body-text');
        if (post.content && post.content.trim()) {
            if (!bodyEl) {
                bodyEl = document.createElement('div');
                bodyEl.className = 'rich-message-body post-body-text';
                card.querySelector('.post-card-header').insertAdjacentElement('afterend', bodyEl);
            }
            bodyEl.innerHTML = post.content;
        } else if (bodyEl) {
            bodyEl.remove();
        }

        // images
        let imgEl = card.querySelector('.post-images');
        if (post.imageUrls && post.imageUrls.length) {
            if (!imgEl) {
                imgEl = document.createElement('div');
                const ref = card.querySelector('.post-reaction-summary') || card.querySelector('.post-card-actions');
                ref.insertAdjacentElement('beforebegin', imgEl);
            }
            imgEl.className = `post-images ${post.imageUrls.length === 1 ? 'post-images-single' : 'post-images-grid'}`;
            imgEl.innerHTML = post.imageUrls.map(u => `<img src="${u}" alt="Paylaşım görseli" loading="lazy">`).join('');
        } else if (imgEl) {
            imgEl.remove();
        }

        // edited badge
        const timeMeta = card.querySelector('.post-time-meta');
        if (timeMeta && post.updatedAt) {
            let badge = timeMeta.querySelector('.post-edited-badge');
            if (!badge) {
                badge = document.createElement('span');
                badge.className = 'post-edited-badge';
                timeMeta.appendChild(badge);
            }
            badge.textContent = '(Düzenlendi)';
        }
    };

    // ── Delegate click on "Düzenle" buttons ───────────────────────────────────
    document.addEventListener('click', e => {
        const btn = e.target.closest('[data-edit-post]');
        if (!btn) return;
        loadPost(btn.dataset.editPost);
    });
})();
