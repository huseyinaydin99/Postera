(() => {
    const dropdown = document.querySelector('[data-messages-dropdown]');
    if (!dropdown) return;

    const toggleBtn = dropdown.querySelector('[data-messages-dropdown-toggle]');
    const popover = dropdown.querySelector('[data-messages-popover]');
    const list = dropdown.querySelector('[data-messages-popover-list]');
    const loading = dropdown.querySelector('[data-messages-loading]');
    const empty = dropdown.querySelector('[data-messages-empty]');
    const badge = dropdown.querySelector('[data-messages-badge]');

    if (!toggleBtn || !popover || !list || !loading || !empty) return;

    let isOpen = false;
    let isLoading = false;
    let hasMore = true;
    let currentOffset = 0;
    let initialLoaded = false;

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

    const getSnippet = (item) => {
        if (item.preview && item.preview.trim().length > 0) {
            return item.preview.trim();
        }
        return 'İçerik';
    };

    const createMessageElement = (item) => {
        const link = document.createElement('a');
        link.href = `/messages/${item.id}`;
        link.className = `popover-message-item ${item.read ? 'is-read' : 'is-unread'}`;

        const avatar = document.createElement('img');
        avatar.className = 'popover-message-avatar';
        avatar.src = item.counterpartProfileImageUrl || '/images/default-avatar.svg';
        avatar.alt = item.counterpartName || 'Profil';

        const content = document.createElement('div');
        content.className = 'popover-message-content';

        const top = document.createElement('div');
        top.className = 'popover-message-top';

        const name = document.createElement('span');
        name.className = 'popover-message-name';
        name.textContent = item.counterpartName || 'İsimsiz';

        const time = document.createElement('time');
        time.className = 'popover-message-time';
        time.textContent = formatDate(item.sentAt);

        top.appendChild(name);
        top.appendChild(time);

        const snippet = document.createElement('p');
        snippet.className = 'popover-message-snippet';
        snippet.textContent = getSnippet(item);

        content.appendChild(top);
        content.appendChild(snippet);

        link.appendChild(avatar);
        link.appendChild(content);

        if (!item.read) {
            const unreadDot = document.createElement('span');
            unreadDot.className = 'popover-unread-dot';
            unreadDot.setAttribute('aria-label', 'Okunmamış mesaj');
            link.appendChild(unreadDot);
        }

        return link;
    };

    const loadMessages = (offset, limit, isInitial = false) => {
        if (isLoading) return;
        isLoading = true;
        loading.hidden = false;

        fetch(`/messages/api/recent?offset=${offset}&limit=${limit}`, {
            headers: {
                'Accept': 'application/json'
            }
        })
            .then((res) => {
                if (!res.ok) throw new Error('Mesajlar yüklenemedi');
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
                    badge.dataset.count = String(data.unreadCount);
                }

                if (isInitial && (!data.messages || data.messages.length === 0)) {
                    empty.hidden = false;
                } else {
                    empty.hidden = true;
                    if (data.messages && data.messages.length > 0) {
                        data.messages.forEach((item) => {
                            list.appendChild(createMessageElement(item));
                        });
                    }
                }

                currentOffset = data.nextOffset;
                hasMore = data.hasMore;
                initialLoaded = true;
            })
            .catch((err) => {
                console.error('Mesajlar yüklenirken hata oluştu:', err);
                if (isInitial && list.children.length === 0) {
                    empty.textContent = 'Mesajlar yüklenemedi.';
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
        const profileMenu = document.querySelector('.profile-menu[open]');
        if (profileMenu) profileMenu.removeAttribute('open');

        if (!initialLoaded) {
            list.innerHTML = '';
            currentOffset = 0;
            hasMore = true;
            loadMessages(0, 6, true);
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
            loadMessages(currentOffset, 3, false);
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
