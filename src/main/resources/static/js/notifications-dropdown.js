(() => {
    const dropdown = document.querySelector('[data-notifications-dropdown]');
    if (!dropdown) return;

    const toggleBtn = dropdown.querySelector('[data-notifications-dropdown-toggle]');
    const popover = dropdown.querySelector('[data-notifications-popover]');
    const list = dropdown.querySelector('[data-notifications-popover-list]');
    const loading = dropdown.querySelector('[data-notifications-loading]');
    const empty = dropdown.querySelector('[data-notifications-empty]');
    const badge = dropdown.querySelector('[data-notifications-badge]');
    const markAllBtn = dropdown.querySelector('[data-mark-all-read]');

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

    const escapeHtml = (unsafe) => {
        if (!unsafe) return '';
        return String(unsafe)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    };

    const createNotificationElement = (item) => {
        const row = document.createElement('a');
        row.href = item.targetUrl || '#';
        row.className = `popover-notification-item ${item.isRead ? 'is-read' : 'is-unread'}`;
        row.dataset.notificationId = item.id;
        row.dataset.isRead = item.isRead ? 'true' : 'false';

        // Avatar Wrapper with badge icon
        const avatarWrapper = document.createElement('div');
        avatarWrapper.className = 'popover-notification-avatar-wrapper';

        const avatar = document.createElement('img');
        avatar.className = 'popover-notification-avatar';
        avatar.src = item.actorProfileImageUrl || '/images/default-avatar.svg';
        avatar.alt = item.actorName || 'Profil';

        const iconBadge = document.createElement('span');
        iconBadge.className = 'popover-notification-icon-badge';
        iconBadge.innerHTML = `<span class="material-symbols-outlined">people</span>`;

        avatarWrapper.appendChild(avatar);
        avatarWrapper.appendChild(iconBadge);

        // Content
        const content = document.createElement('div');
        content.className = 'popover-notification-content';

        const text = document.createElement('p');
        text.className = 'popover-notification-text';
        text.innerHTML = `<strong>${escapeHtml(item.actorName)}</strong> arkadaşlık isteğinizi kabul etti.`;

        const time = document.createElement('time');
        time.className = 'popover-notification-time';
        time.textContent = formatDate(item.createdAt);

        content.appendChild(text);
        content.appendChild(time);

        row.appendChild(avatarWrapper);
        row.appendChild(content);

        if (!item.isRead) {
            const unreadDot = document.createElement('span');
            unreadDot.className = 'popover-unread-dot';
            unreadDot.setAttribute('aria-label', 'Okunmamış bildirim');
            row.appendChild(unreadDot);
        }

        // Click handler: Mark as read & navigate
        row.addEventListener('click', async (e) => {
            if (row.dataset.isRead === 'false') {
                row.dataset.isRead = 'true';
                row.classList.remove('is-unread');
                row.classList.add('is-read');
                const dot = row.querySelector('.popover-unread-dot');
                if (dot) dot.remove();

                // Decrement badge
                if (badge && !badge.hidden) {
                    const count = parseInt(badge.textContent, 10) || 0;
                    const newCount = Math.max(0, count - 1);
                    if (newCount > 0) {
                        badge.textContent = newCount > 99 ? '99+' : newCount;
                    } else {
                        badge.hidden = true;
                    }
                }

                try {
                    fetch(`/notifications/api/${item.id}/read`, {
                        method: 'POST',
                        headers: {
                            ...getCsrfHeaders(),
                            'Accept': 'application/json'
                        }
                    });
                } catch (err) {
                    console.error(err);
                }
            }

            if (!item.targetUrl || item.targetUrl === '#') {
                e.preventDefault();
            }
        });

        return row;
    };

    const loadNotifications = (offset, limit, isInitial = false) => {
        if (isLoading) return;
        isLoading = true;
        loading.hidden = false;

        fetch(`/notifications/api/recent?offset=${offset}&limit=${limit}`, {
            headers: {
                'Accept': 'application/json'
            }
        })
            .then((res) => {
                if (!res.ok) throw new Error('Bildirimler yüklenemedi');
                return res.json();
            })
            .then((data) => {
                if (badge) {
                    if (data.unreadCount > 0) {
                        badge.textContent = data.unreadCount > 99 ? '99+' : data.unreadCount;
                        badge.hidden = false;
                    } else {
                        badge.hidden = true;
                    }
                }

                if (isInitial && (!data.notifications || data.notifications.length === 0)) {
                    empty.hidden = false;
                } else {
                    empty.hidden = true;
                    if (data.notifications && data.notifications.length > 0) {
                        data.notifications.forEach((item) => {
                            list.appendChild(createNotificationElement(item));
                        });
                    }
                }

                currentOffset = data.nextOffset;
                hasMore = data.hasMore;
                initialLoaded = true;
            })
            .catch((err) => {
                console.error('Bildirimler yüklenirken hata:', err);
                if (isInitial && list.children.length === 0) {
                    empty.textContent = 'Bildirimler yüklenemedi.';
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

        // Diğer popoverları kapat
        const friendsPopover = document.querySelector('[data-friends-popover]');
        const friendsToggle = document.querySelector('[data-friends-dropdown-toggle]');
        if (friendsPopover && friendsToggle) {
            friendsPopover.hidden = true;
            friendsToggle.setAttribute('aria-expanded', 'false');
        }

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
            loadNotifications(0, 6, true);
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
            loadNotifications(currentOffset, 3, false);
        }
    });

    if (markAllBtn) {
        markAllBtn.addEventListener('click', async (e) => {
            e.stopPropagation();
            try {
                const res = await fetch('/notifications/api/mark-all-read', {
                    method: 'POST',
                    headers: {
                        ...getCsrfHeaders(),
                        'Accept': 'application/json'
                    }
                });
                if (res.ok) {
                    list.querySelectorAll('.popover-notification-item').forEach((item) => {
                        item.classList.remove('is-unread');
                        item.classList.add('is-read');
                        item.dataset.isRead = 'true';
                        const dot = item.querySelector('.popover-unread-dot');
                        if (dot) dot.remove();
                    });
                    if (badge) badge.hidden = true;
                }
            } catch (err) {
                console.error(err);
            }
        });
    }

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
