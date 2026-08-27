(() => {
    const searchInput = document.querySelector('[data-discover-search-input]');
    const clearBtn = document.querySelector('[data-discover-clear]');
    const grid = document.querySelector('[data-discover-grid]');
    const emptyState = document.querySelector('[data-discover-empty]');
    const loadingState = document.querySelector('[data-discover-loading]');

    const getCsrfHeaders = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        return token && header ? { [header]: token } : {};
    };

    const showToast = (title, icon = 'success') => {
        if (window.Swal) {
            window.Swal.fire({
                toast: true,
                position: 'bottom-end',
                icon,
                title,
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true
            });
        }
    };

    const promptState = document.querySelector('[data-discover-prompt]');

    const renderActionButton = (user) => {
        if (user.friendshipStatus === 'PENDING_SENT') {
            return `
                <button type="button" class="btn-friend-action btn-request-sent" disabled>
                    <span class="material-symbols-outlined">done</span>
                    <span>İstek Gönderildi</span>
                </button>
            `;
        }
        if (user.friendshipStatus === 'PENDING_RECEIVED') {
            return `
                <button type="button" class="btn-friend-action btn-send-request" data-send-request data-user-id="${user.id}">
                    <span class="material-symbols-outlined">how_to_reg</span>
                    <span>İsteği Kabul Et</span>
                </button>
            `;
        }
        if (user.friendshipStatus === 'FRIENDS') {
            return `
                <button type="button" class="btn-friend-action btn-already-friends" disabled>
                    <span class="material-symbols-outlined">people</span>
                    <span>Arkadaşsınız</span>
                </button>
            `;
        }
        return `
            <button type="button" class="btn-friend-action btn-send-request" data-send-request data-user-id="${user.id}">
                <span class="material-symbols-outlined">person_add</span>
                <span>Arkadaşlık İsteği Gönder</span>
            </button>
        `;
    };

    const renderUsers = (users, query) => {
        if (!grid) return;

        if (!query || query.trim().length === 0) {
            grid.innerHTML = '';
            if (emptyState) emptyState.classList.add('is-hidden');
            if (promptState) promptState.classList.remove('is-hidden');
            return;
        }

        if (promptState) promptState.classList.add('is-hidden');

        if (!users || users.length === 0) {
            grid.innerHTML = '';
            if (emptyState) emptyState.classList.remove('is-hidden');
            return;
        }

        if (emptyState) emptyState.classList.add('is-hidden');
        grid.innerHTML = users.map((user) => `
            <div class="discover-user-card" data-user-id="${user.id}">
                <div class="discover-card-top">
                    <img class="discover-user-avatar"
                         src="${user.profileImageUrl ? user.profileImageUrl : '/images/default-avatar.svg'}"
                         alt="${user.fullName} profil fotoğrafı">
                    <div class="discover-user-info">
                        <strong class="discover-user-name">${escapeHtml(user.fullName)}</strong>
                        <span class="discover-user-email">${escapeHtml(user.email)}</span>
                    </div>
                </div>
                <div class="discover-card-action">
                    ${renderActionButton(user)}
                </div>
            </div>
        `).join('');
    };

    const escapeHtml = (str) => {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    };

    let debounceTimer = null;
    let currentAbortController = null;

    const performSearch = (query) => {
        if (currentAbortController) {
            currentAbortController.abort();
        }

        const trimmed = (query || '').trim();
        if (trimmed.length === 0) {
            if (loadingState) loadingState.hidden = true;
            renderUsers([], '');
            return;
        }

        currentAbortController = new AbortController();

        if (loadingState) loadingState.hidden = false;
        if (emptyState) emptyState.classList.add('is-hidden');
        if (promptState) promptState.classList.add('is-hidden');

        fetch(`/discover/api/search?q=${encodeURIComponent(trimmed)}`, {
            signal: currentAbortController.signal,
            headers: { 'Accept': 'application/json' }
        })
            .then((res) => {
                if (!res.ok) throw new Error('Arama sırasında bir hata oluştu.');
                return res.json();
            })
            .then((users) => {
                renderUsers(users, trimmed);
            })
            .catch((err) => {
                if (err.name !== 'AbortError') {
                    console.error(err);
                }
            })
            .finally(() => {
                if (loadingState) loadingState.hidden = true;
            });
    };

    if (searchInput) {
        const updateClearBtn = () => {
            if (clearBtn) {
                clearBtn.hidden = searchInput.value.trim().length === 0;
            }
        };

        updateClearBtn();

        searchInput.addEventListener('input', () => {
            updateClearBtn();
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                performSearch(searchInput.value);
            }, 250);
        });

        if (clearBtn) {
            clearBtn.addEventListener('click', () => {
                searchInput.value = '';
                updateClearBtn();
                searchInput.focus();
                performSearch('');
            });
        }
    }

    // Arkadaşlık İsteği Gönderme Event Delegation
    document.addEventListener('click', async (e) => {
        const btn = e.target.closest('button[data-send-request]');
        if (!btn || btn.disabled) return;

        const userId = btn.dataset.userId;
        if (!userId) return;

        const originalHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = `<span class="material-symbols-outlined spin-icon">progress_activity</span> <span>Gönderiliyor…</span>`;

        try {
            const response = await fetch(`/discover/api/request/${userId}`, {
                method: 'POST',
                headers: {
                    ...getCsrfHeaders(),
                    'Accept': 'application/json'
                }
            });

            const data = await response.json();
            if (response.ok && data.success) {
                if (data.status === 'FRIENDS') {
                    btn.className = 'btn-friend-action btn-already-friends';
                    btn.innerHTML = `<span class="material-symbols-outlined">people</span> <span>Arkadaşsınız</span>`;
                    btn.disabled = true;
                    showToast('Arkadaşlık isteği kabul edildi!', 'success');
                } else {
                    btn.className = 'btn-friend-action btn-request-sent';
                    btn.innerHTML = `<span class="material-symbols-outlined">done</span> <span>İstek Gönderildi</span>`;
                    btn.disabled = true;
                    showToast('Arkadaşlık isteği başarıyla gönderildi!', 'success');
                }
            } else {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
                showToast(data.message || 'İstek gönderilemedi.', 'error');
            }
        } catch (err) {
            console.error(err);
            btn.disabled = false;
            btn.innerHTML = originalHtml;
            showToast('Bir bağlantı hatası oluştu.', 'error');
        }
    });
})();
