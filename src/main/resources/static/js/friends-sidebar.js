document.addEventListener('DOMContentLoaded', () => {
    const toggleBtn = document.querySelector('[data-friends-sidebar-toggle]');
    const closeBtn = document.querySelector('[data-friends-sidebar-close]');
    const sidebar = document.getElementById('friendsSidebar');
    const listContainer = document.getElementById('friendsSidebarList');
    const statusSelect = document.getElementById('presenceStatusSelect');

    if (!sidebar || !toggleBtn) return;

    const getCsrfHeaders = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
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

    toggleBtn.addEventListener('click', toggleSidebar);
    closeBtn.addEventListener('click', toggleSidebar);

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
                    <div class="friend-item">
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
