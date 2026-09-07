document.addEventListener('DOMContentLoaded', () => {
    const toggleBtn = document.querySelector('[data-friends-sidebar-toggle]');
    const closeBtn = document.querySelector('[data-friends-sidebar-close]');
    const sidebar = document.getElementById('friendsSidebar');
    const listContainer = document.getElementById('friendsSidebarList');
    const statusSelect = document.getElementById('presenceStatusSelect');

    if (!sidebar || !toggleBtn) return;

    const getCsrfHeaders = () => {
        let token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        let header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
        if (!token) {
            token = document.querySelector('input[name="_csrf"]')?.value;
        }
        if (!token) {
            const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
            if (match) token = decodeURIComponent(match[1]);
        }
        return token && header ? { [header]: token } : {};
    };

    let fetchInterval = null;

    const toggleSidebar = () => {
        const isHidden = sidebar.getAttribute('aria-hidden') === 'true';
        sidebar.setAttribute('aria-hidden', !isHidden);
        toggleBtn.setAttribute('aria-expanded', isHidden);
        
        if (isHidden) {
            fetchFriends();
            fetchOwnStatus();
            fetchInterval = setInterval(fetchFriends, 30000); // 30s
        } else {
            clearInterval(fetchInterval);
        }
    };

    toggleBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleSidebar();
    });
    closeBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleSidebar();
    });

    document.addEventListener('click', (e) => {
        if (sidebar.getAttribute('aria-hidden') === 'false') {
            if (!sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
                toggleSidebar();
            }
        }
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && sidebar.getAttribute('aria-hidden') === 'false') {
            toggleSidebar();
        }
    });

    const formatLastSeen = (lastSeenAt) => {
        if (!lastSeenAt) return 'Uzun zaman önce';
        const date = new Date(lastSeenAt);
        const diffMs = Date.now() - date.getTime();
        const diffMins = Math.floor(diffMs / 60000);
        if (diffMins < 60) return `${diffMins} dk önce`;
        const diffHours = Math.floor(diffMins / 60);
        if (diffHours < 24) return `${diffHours} saat önce`;
        return date.toLocaleDateString('tr-TR');
    };

    const fetchFriends = () => {
        fetch('/api/friends/sidebar', {
            headers: { 'Accept': 'application/json' }
        })
        .then(res => res.json())
        .then(friends => {
            if (!friends || friends.length === 0) {
                listContainer.innerHTML = '<div class="friends-sidebar-loading">Listeniz boş.</div>';
                return;
            }
            listContainer.innerHTML = friends.map(f => {
                const presenceText = f.isOnline ? f.presenceStatusLabel : formatLastSeen(f.lastSeenAt);
                return `
                    <div class="friend-item" data-friend-id="${f.id}" data-friend-name="${f.fullName}" data-friend-avatar="${f.profileImageUrl || ''}" data-friend-online="${f.isOnline}" data-friend-status="${f.presenceStatusLabel || ''}">
                        <div class="friend-item-avatar-wrapper">
                            <img src="${f.profileImageUrl || '/images/default-avatar.svg'}" class="friend-item-avatar" alt="Avatar">
                            <div class="friend-item-status-dot ${f.isOnline ? 'online' : 'offline'}"></div>
                        </div>
                        <div class="friend-item-info">
                            <span class="friend-item-name">${f.fullName}</span>
                            <span class="friend-item-presence">${presenceText}</span>
                        </div>
                    </div>
                `;
            }).join('');

            // Add click listeners to friend items to open Chat Dock
            listContainer.querySelectorAll('.friend-item').forEach(item => {
                item.addEventListener('click', () => {
                    const friend = {
                        id: Number(item.getAttribute('data-friend-id')),
                        fullName: item.getAttribute('data-friend-name'),
                        profileImageUrl: item.getAttribute('data-friend-avatar') || null,
                        isOnline: item.getAttribute('data-friend-online') === 'true',
                        presenceStatusLabel: item.getAttribute('data-friend-status') || 'Müsait'
                    };
                    if (window.ChatDock) {
                        window.ChatDock.openChat(friend);
                    }
                });
            });
        })
        .catch(err => console.error('Friends sidebar fetch error:', err));
    };

    const fetchOwnStatus = () => {
        fetch('/api/presence/status', {
            headers: { 'Accept': 'application/json' }
        })
        .then(res => res.json())
        .then(data => {
            if (data && data.status) {
                statusSelect.value = data.status;
            }
        })
        .catch(err => console.error(err));
    };

    statusSelect.addEventListener('change', (e) => {
        const newStatus = e.target.value;
        fetch('/api/presence/status?status=' + encodeURIComponent(newStatus), {
            method: 'POST',
            headers: getCsrfHeaders()
        }).catch(err => console.error(err));
    });
});
