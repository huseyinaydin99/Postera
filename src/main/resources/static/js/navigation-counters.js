(() => {
    const counters = [
        { key: 'friendRequests', badge: '[data-friends-badge]', button: '[data-friends-dropdown-toggle]', label: 'Arkadaşlık İstekleri' },
        { key: 'unreadMessages', badge: '[data-messages-badge]', button: '[data-messages-dropdown-toggle]', label: 'Mesajlar' },
        { key: 'unreadNotifications', badge: '[data-notifications-badge]', button: '[data-notifications-dropdown-toggle]', label: 'Bildirimler' }
    ];
    const intervalMs = 15000;
    let inFlight = false;

    const formatCount = (count) => count > 99 ? '99+' : String(count);

    const render = ({badge, button, label}, count) => {
        const node = document.querySelector(badge);
        if (!node) return;
        const previous = Number.parseInt(node.dataset.count || node.textContent, 10) || 0;
        node.dataset.count = String(count);
        node.textContent = formatCount(count);
        node.hidden = count === 0;

        const buttonNode = document.querySelector(button);
        if (buttonNode) {
            buttonNode.setAttribute('aria-label', count > 0 ? `${label}, ${count} yeni` : label);
        }
        if (count > previous) {
            node.classList.remove('is-updated');
            void node.offsetWidth;
            node.classList.add('is-updated');
        }
    };

    const refresh = async () => {
        if (inFlight || document.hidden) return;
        inFlight = true;
        try {
            const response = await fetch('/api/navigation/counters', {
                headers: { Accept: 'application/json' },
                cache: 'no-store'
            });
            if (!response.ok) return;
            const data = await response.json();
            counters.forEach((counter) => render(counter, Math.max(0, Number(data[counter.key]) || 0)));
            document.dispatchEvent(new CustomEvent('postera:navigation-counters-updated', { detail: data }));
        } catch (_) {
            // Geçici bağlantı sorununda önceki sayaç değerini korur.
        } finally {
            inFlight = false;
        }
    };

    document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
    window.PosteraNavigationCounters = { refresh };
    refresh();
    window.setInterval(refresh, intervalMs);
})();
