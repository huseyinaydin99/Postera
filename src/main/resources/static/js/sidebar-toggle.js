(() => {
    const button = document.querySelector('[data-sidebar-toggle]');
    if (!button) return;

    const update = () => {
        const open = !document.body.classList.contains('sidebar-collapsed');
        button.setAttribute('aria-expanded', String(open));
        button.setAttribute('aria-label', open ? 'Menüyü daralt' : 'Menüyü aç');
    };
    button.addEventListener('click', () => {
        document.body.classList.toggle('sidebar-collapsed');
        update();
    });
    update();
})();
