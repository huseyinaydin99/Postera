(() => {
    const form = document.querySelector('[data-message-search]');
    const input = document.querySelector('[data-message-search-input]');
    const suggestions = document.querySelector('[data-message-search-suggestions]');
    if (!form || !input || !suggestions) return;
    let request;
    const close = () => { suggestions.hidden = true; suggestions.replaceChildren(); };
    input.addEventListener('input', async () => {
        const query = input.value.trim();
        if (query.length < 2) return close();
        request?.abort();
        request = new AbortController();
        try {
            const response = await fetch(`/messages/search/suggestions?q=${encodeURIComponent(query)}`, {signal: request.signal});
            if (!response.ok) return close();
            const items = await response.json();
            suggestions.replaceChildren(...items.map((item) => {
                const link = document.createElement('a');
                link.href = `/messages/${item.id}`;
                const subject = document.createElement('strong'); subject.textContent = item.subject;
                const meta = document.createElement('span'); meta.textContent = `${item.counterpartName} · ${item.counterpartEmail}`;
                const preview = document.createElement('small'); preview.textContent = item.bodyPreview;
                link.append(subject, meta, preview);
                return link;
            }));
            suggestions.hidden = items.length === 0;
        } catch (error) {
            if (error.name !== 'AbortError') close();
        }
    });
    document.addEventListener('click', (event) => { if (!form.contains(event.target)) close(); });
})();
