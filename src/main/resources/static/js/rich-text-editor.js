(() => {
    const editor = document.querySelector('[data-rte-editor]');
    const input = document.querySelector('[data-rte-input]');
    const form = document.querySelector('#reply-form');
    if (!editor || !input || !form) return;

    const sync = () => { input.value = editor.innerHTML; };
    document.querySelectorAll('[data-rte-command]').forEach((button) => {
        button.addEventListener('click', () => {
            editor.focus();
            document.execCommand(button.dataset.rteCommand, false, button.dataset.rteValue || null);
            sync();
        });
    });
    document.querySelectorAll('[data-rte-emoji]').forEach((button) => {
        button.addEventListener('click', () => {
            editor.focus();
            document.execCommand('insertText', false, button.dataset.rteEmoji);
            sync();
        });
    });
    editor.addEventListener('input', sync);
    form.addEventListener('submit', sync);
})();
