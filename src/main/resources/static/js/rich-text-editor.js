(() => {
    const editor = document.querySelector('[data-rte-editor]');
    const input = document.querySelector('[data-rte-input]');
    const form = document.querySelector('#reply-form');
    if (!editor || !input || !form) return;

    const sync = () => { input.value = editor.innerHTML; };
    let savedRange;
    const saveRange = () => {
        const selection = window.getSelection();
        if (selection.rangeCount && editor.contains(selection.anchorNode)) savedRange = selection.getRangeAt(0).cloneRange();
    };
    const restoreRange = () => {
        if (!savedRange) return;
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(savedRange);
    };
    const closePopovers = () => document.querySelectorAll('.rte-popover').forEach((popover) => { popover.hidden = true; });
    document.querySelectorAll('[data-rte-command]').forEach((button) => {
        button.addEventListener('click', () => {
            editor.focus();
            restoreRange();
            document.execCommand(button.dataset.rteCommand, false, button.dataset.rteValue || null);
            sync();
        });
    });
    document.querySelectorAll('[data-rte-toggle]').forEach((button) => {
        button.addEventListener('click', () => {
            const popover = document.getElementById(button.dataset.rteToggle);
            const willOpen = popover.hidden;
            closePopovers();
            popover.hidden = !willOpen;
        });
    });
    document.querySelectorAll('[data-rte-emoji]').forEach((button) => {
        button.addEventListener('click', () => {
            editor.focus();
            restoreRange();
            document.execCommand('insertText', false, button.dataset.rteEmoji);
            sync();
            closePopovers();
        });
    });
    document.querySelectorAll('[data-rte-gif]').forEach((button) => {
        button.addEventListener('click', () => {
            editor.focus();
            restoreRange();
            document.execCommand('insertHTML', false, `<img src="${button.dataset.rteGif}">`);
            sync();
            closePopovers();
        });
    });
    document.querySelector('[data-rte-add-link]')?.addEventListener('click', () => {
        const urlInput = document.querySelector('[data-rte-link-url]');
        if (!/^https?:\/\//i.test(urlInput.value)) return;
        editor.focus();
        restoreRange();
        const selectedText = window.getSelection().toString();
        if (selectedText) {
            document.execCommand('createLink', false, urlInput.value);
        } else {
            document.execCommand('insertHTML', false, `<a href="${urlInput.value}">${urlInput.value}</a>`);
        }
        urlInput.value = '';
        sync();
        closePopovers();
    });
    editor.addEventListener('keyup', saveRange);
    editor.addEventListener('mouseup', saveRange);
    editor.addEventListener('input', sync);
    form.addEventListener('submit', sync);
})();
