(() => {
    const dropdown = document.querySelector('[data-friends-dropdown]');
    if (!dropdown) return;

    const toggleBtn = dropdown.querySelector('[data-friends-dropdown-toggle]');
    const popover = dropdown.querySelector('[data-friends-popover]');
    const list = dropdown.querySelector('[data-friends-popover-list]');
    const loading = dropdown.querySelector('[data-friends-loading]');
    const empty = dropdown.querySelector('[data-friends-empty]');
    const badge = dropdown.querySelector('[data-friends-badge]');

    if (!toggleBtn || !popover || !list || !loading || !empty) return;

    let isOpen = false;
    let isLoading = false;
    let hasMore = true;
    let currentOffset = 0;
    let initialLoaded = false;

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

    const formatDate = (isoString) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        if (isNaN(date.getTime())) return '';
        const now = new Date();

        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        const isToday = date.toDateString() === now.toDateString();
        if (isToday) {
            return `${hours}:${minutes}`;
        }

        const yesterday = new Date(now);
        yesterday.setDate(now.getDate() - 1);
        if (date.toDateString() === yesterday.toDateString()) {
            return `Dün ${hours}:${minutes}`;
        }

        const day = String(date.getDate()).padStart(2, '0');
        const months = ['Oca', 'Şub', 'Mar', 'Nis', 'May', 'Haz', 'Tem', 'Ağu', 'Eyl', 'Eki', 'Kas', 'Ara'];
        const monthName = months[date.getMonth()];
        if (date.getFullYear() === now.getFullYear()) {
            return `${day} ${monthName} ${hours}:${minutes}`;
        }
        return `${day}.${String(date.getMonth() + 1).padStart(2, '0')}.${date.getFullYear()}`;
    };

    const createRequestElement = (item) => {
        const row = document.createElement('div');
        row.className = 'popover-friend-item';
        row.dataset.friendshipId = item.friendshipId;

        const avatar = document.createElement('img');
        avatar.className = 'popover-friend-avatar';
        avatar.src = item.senderProfileImageUrl || '/images/default-avatar.svg';
        avatar.alt = item.senderName || 'Profil';

        const content = document.createElement('div');
        content.className = 'popover-friend-content';

        const top = document.createElement('div');
        top.className = 'popover-friend-top';

        const name = document.createElement('strong');
        name.className = 'popover-friend-name';
        name.textContent = item.senderName || 'Kullanıcı';

        const time = document.createElement('time');
        time.className = 'popover-friend-time';
        time.textContent = formatDate(item.sentAt);

        top.appendChild(name);
        top.appendChild(time);

        const email = document.createElement('span');
        email.className = 'popover-friend-email';
        email.textContent = item.senderEmail || '';

        content.appendChild(top);
        content.appendChild(email);

        const action = document.createElement('div');
        action.className = 'popover-friend-action';

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'popover-btn-accept';
        btn.dataset.acceptRequest = 'true';
        btn.dataset.friendshipId = item.friendshipId;
        btn.innerHTML = `<span class="material-symbols-outlined">how_to_reg</span><span>İsteği Kabul Et</span>`;

        action.appendChild(btn);

        row.appendChild(avatar);
        row.appendChild(content);
        row.appendChild(action);

        return row;
    };

    const loadRequests = (offset, limit, isInitial = false) => {
        if (isLoading) return;
        isLoading = true;
        loading.hidden = false;

        fetch(`/friends/api/requests?offset=${offset}&limit=${limit}`, {
            headers: {
                'Accept': 'application/json'
            }
        })
            .then((res) => {
                if (!res.ok) throw new Error('İstekler yüklenemedi');
                return res.json();
            })
            .then((data) => {
                if (badge) {
                    if (data.totalCount > 0) {
                        badge.textContent = data.totalCount > 99 ? '99+' : data.totalCount;
                        badge.hidden = false;
                    } else {
                        badge.hidden = true;
                    }
                }

                if (isInitial && (!data.requests || data.requests.length === 0)) {
                    empty.hidden = false;
                } else {
                    empty.hidden = true;
                    if (data.requests && data.requests.length > 0) {
                        data.requests.forEach((item) => {
                            list.appendChild(createRequestElement(item));
                        });
                    }
                }

                currentOffset = data.nextOffset;
                hasMore = data.hasMore;
                initialLoaded = true;
            })
            .catch((err) => {
                console.error('Arkadaşlık istekleri yüklenirken hata oluştu:', err);
                if (isInitial && list.children.length === 0) {
                    empty.textContent = 'İstekler yüklenemedi.';
                    empty.hidden = false;
                }
            })
            .finally(() => {
                isLoading = false;
                loading.hidden = true;
            });
    };

    const openDropdown = () => {
        isOpen = true;
        popover.hidden = false;
        toggleBtn.setAttribute('aria-expanded', 'true');

        // Diğer menüleri kapat
        const messagesPopover = document.querySelector('[data-messages-popover]');
        const messagesToggle = document.querySelector('[data-messages-dropdown-toggle]');
        if (messagesPopover && messagesToggle) {
            messagesPopover.hidden = true;
            messagesToggle.setAttribute('aria-expanded', 'false');
        }

        const profileMenu = document.querySelector('.profile-menu[open]');
        if (profileMenu) profileMenu.removeAttribute('open');

        if (!initialLoaded) {
            list.innerHTML = '';
            currentOffset = 0;
            hasMore = true;
            loadRequests(0, 6, true);
        }
    };

    const closeDropdown = () => {
        isOpen = false;
        popover.hidden = true;
        toggleBtn.setAttribute('aria-expanded', 'false');
    };

    toggleBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        if (isOpen) {
            closeDropdown();
        } else {
            openDropdown();
        }
    });

    list.addEventListener('scroll', () => {
        if (!hasMore || isLoading) return;
        const scrollBottom = list.scrollHeight - list.scrollTop - list.clientHeight;
        if (scrollBottom < 50) {
            loadRequests(currentOffset, 3, false);
        }
    });

    // Event delegation for "İsteği Kabul Et"
    document.addEventListener('click', async (e) => {
        const btn = e.target.closest('button[data-accept-request]');
        if (!btn || btn.disabled) return;

        const friendshipId = btn.dataset.friendshipId;
        if (!friendshipId) return;

        const originalHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = `<span class="material-symbols-outlined spin-icon">progress_activity</span><span>Kabul ediliyor…</span>`;

        try {
            const response = await fetch(`/friends/api/requests/${friendshipId}/accept`, {
                method: 'POST',
                headers: {
                    ...getCsrfHeaders(),
                    'Accept': 'application/json'
                }
            });

            const data = await response.json();
            if (response.ok && data.success) {
                btn.className = 'popover-btn-accepted';
                btn.innerHTML = `<span class="material-symbols-outlined">people</span><span>Arkadaşsınız</span>`;
                btn.disabled = true;
                showToast('Arkadaşlık isteği kabul edildi!', 'success');

                if (badge && !badge.hidden) {
                    const currentCount = parseInt(badge.textContent, 10) || 0;
                    const newCount = Math.max(0, currentCount - 1);
                    if (newCount > 0) {
                        badge.textContent = newCount > 99 ? '99+' : newCount;
                    } else {
                        badge.hidden = true;
                    }
                }
            } else {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
                showToast(data.message || 'İstek kabul edilemedi.', 'error');
            }
        } catch (err) {
            console.error(err);
            btn.disabled = false;
            btn.innerHTML = originalHtml;
            showToast('Bir bağlantı hatası oluştu.', 'error');
        }
    });

    document.addEventListener('click', (e) => {
        if (isOpen && !dropdown.contains(e.target)) {
            closeDropdown();
        }
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && isOpen) {
            closeDropdown();
            toggleBtn.focus();
        }
    });
})();
