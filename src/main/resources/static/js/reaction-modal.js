(() => {
    const EMOJI_MAP = { LIKE: '👍', LAUGH: '😂', ANGRY: '😠', SURPRISED: '😮', SUPPORT: '🤝', HEART: '❤️' };
    const LABEL_MAP = { LIKE: 'Beğeni', LAUGH: 'Gülme', ANGRY: 'Kızgınlık', SURPRISED: 'Şaşkınlık', SUPPORT: 'Yanındayım', HEART: 'Kalp' };

    // ── Modal DOM ──────────────────────────────────────────────────────────────
    const backdrop = document.createElement('div');
    backdrop.className = 'rxm-backdrop';
    backdrop.setAttribute('aria-hidden', 'true');

    const modal = document.createElement('div');
    modal.className = 'rxm-modal';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-modal', 'true');
    modal.setAttribute('aria-label', 'Tepkiler');
    modal.innerHTML = `
        <div class="rxm-header">
            <span class="rxm-title">Tepkiler</span>
            <button type="button" class="rxm-close" aria-label="Kapat">
                <span class="material-symbols-outlined">close</span>
            </button>
        </div>
        <div class="rxm-tabs" role="tablist"></div>
        <div class="rxm-body"></div>`;

    document.body.appendChild(backdrop);
    document.body.appendChild(modal);

    const tabsEl = modal.querySelector('.rxm-tabs');
    const bodyEl = modal.querySelector('.rxm-body');

    // ── Open / Close ──────────────────────────────────────────────────────────
    const open = () => { backdrop.classList.add('rxm-visible'); modal.classList.add('rxm-visible'); document.body.style.overflow = 'hidden'; };
    const close = () => { backdrop.classList.remove('rxm-visible'); modal.classList.remove('rxm-visible'); document.body.style.overflow = ''; };

    backdrop.addEventListener('click', close);
    modal.querySelector('.rxm-close').addEventListener('click', close);
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') close(); });

    // ── Render tabs ───────────────────────────────────────────────────────────
    const renderTab = (type, users, active) => {
        const emoji = EMOJI_MAP[type] || type;
        const label = LABEL_MAP[type] || type;
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'rxm-tab' + (active ? ' rxm-tab-active' : '');
        btn.setAttribute('role', 'tab');
        btn.setAttribute('aria-selected', active ? 'true' : 'false');
        btn.dataset.type = type;
        btn.innerHTML = `<span class="rxm-tab-emoji">${emoji}</span><span class="rxm-tab-label">${label}</span><span class="rxm-tab-count">${users.length}</span>`;
        return btn;
    };

    const renderUsers = (users) => {
        if (!users.length) {
            bodyEl.innerHTML = '<p class="rxm-empty">Henüz tepki yok.</p>';
            return;
        }
        bodyEl.innerHTML = users.map(u => `
            <div class="rxm-user-row">
                <img class="rxm-user-avatar" src="${u.profileImageUrl || '/images/default-avatar.svg'}" alt="${escHtml(u.name)}">
                <span class="rxm-user-name">${escHtml(u.name)}</span>
            </div>`).join('');
    };

    const escHtml = (s) => String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');

    const showModal = async (postId, activeType) => {
        tabsEl.innerHTML = '<span class="rxm-loading">Yükleniyor…</span>';
        bodyEl.innerHTML = '';
        open();

        let data;
        try {
            const res = await fetch(`/timeline/api/posts/${postId}/reactions/users`, { headers: { Accept: 'application/json' } });
            if (!res.ok) throw new Error();
            data = await res.json(); // { LIKE: [...], HEART: [...], ... }
        } catch {
            tabsEl.innerHTML = '';
            bodyEl.innerHTML = '<p class="rxm-empty">Tepkiler yüklenemedi.</p>';
            return;
        }

        const types = Object.keys(data);
        if (!types.length) {
            tabsEl.innerHTML = '';
            bodyEl.innerHTML = '<p class="rxm-empty">Henüz tepki yok.</p>';
            return;
        }

        // Ensure activeType is valid, fallback to first
        const currentType = types.includes(activeType) ? activeType : types[0];

        tabsEl.innerHTML = '';
        types.forEach(type => {
            const tab = renderTab(type, data[type], type === currentType);
            tab.addEventListener('click', () => {
                tabsEl.querySelectorAll('.rxm-tab').forEach(t => { t.classList.remove('rxm-tab-active'); t.setAttribute('aria-selected', 'false'); });
                tab.classList.add('rxm-tab-active');
                tab.setAttribute('aria-selected', 'true');
                renderUsers(data[type]);
            });
            tabsEl.appendChild(tab);
        });

        renderUsers(data[currentType]);
    };

    // ── Delegate click on reaction counts ────────────────────────────────────
    document.addEventListener('click', (e) => {
        const chip = e.target.closest('[data-reaction-type][data-post-id]');
        if (!chip) return;
        showModal(chip.dataset.postId, chip.dataset.reactionType);
    });
})();
