(() => {
    const editor = document.querySelector('[data-rte-editor]');
    const input  = document.querySelector('[data-rte-input]');
    const form   = editor?.closest('form') || document.querySelector('#reply-form');
    if (!editor || !input || !form) return;

    const sync = () => { input.value = editor.innerHTML; };

    let savedRange = null;
    const saveRange = () => {
        const sel = window.getSelection();
        if (sel.rangeCount && editor.contains(sel.anchorNode))
            savedRange = sel.getRangeAt(0).cloneRange();
    };
    const restoreRange = () => {
        if (!savedRange) { editor.focus(); return; }
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(savedRange);
    };

    const closePopovers = () =>
        form.querySelectorAll('.rte-popover').forEach(p => { p.hidden = true; });

    // ── Formatting commands ───────────────────────────────────────────────────
    form.querySelectorAll('[data-rte-command]').forEach(btn => {
        btn.addEventListener('mousedown', e => {
            e.preventDefault();
            document.execCommand(btn.dataset.rteCommand, false, btn.dataset.rteValue || null);
            sync();
        });
    });

    // ── Toggle popovers ───────────────────────────────────────────────────────
    form.querySelectorAll('[data-rte-toggle]').forEach(btn => {
        btn.addEventListener('mousedown', e => {
            e.preventDefault();
            const pop = document.getElementById(btn.dataset.rteToggle);
            if (!pop) return;
            const willOpen = pop.hidden;
            closePopovers();
            pop.hidden = !willOpen;
        });
    });

    // ── Emoji insertion ───────────────────────────────────────────────────────
    form.querySelectorAll('[data-rte-emoji]').forEach(btn => {
        btn.addEventListener('mousedown', e => {
            e.preventDefault();
            restoreRange();
            document.execCommand('insertText', false, btn.dataset.rteEmoji);
            sync();
            closePopovers();
        });
    });

    // ── GIF insertion ─────────────────────────────────────────────────────────
    form.querySelectorAll('[data-rte-gif]').forEach(btn => {
        btn.addEventListener('mousedown', e => {
            e.preventDefault();
            restoreRange();
            document.execCommand('insertHTML', false,
                `<img class="rich-gif" src="${btn.dataset.rteGif}" alt="GIF">`);
            sync();
            closePopovers();
        });
    });

    // ── Link insertion ────────────────────────────────────────────────────────
    form.querySelector('[data-rte-add-link]')?.addEventListener('mousedown', e => {
        e.preventDefault();
        const urlInput = form.querySelector('[data-rte-link-url]');
        if (!urlInput || !/^https?:\/\//i.test(urlInput.value)) return;
        restoreRange();
        const selectedText = window.getSelection().toString();
        if (selectedText) {
            document.execCommand('createLink', false, urlInput.value);
        } else {
            document.execCommand('insertHTML', false,
                `<a href="${urlInput.value}" target="_blank" rel="noopener noreferrer">${urlInput.value}</a>`);
        }
        urlInput.value = '';
        sync();
        closePopovers();
    });

    // ── Close on outside click ────────────────────────────────────────────────
    document.addEventListener('mousedown', e => {
        if (!e.target.closest('.rte-popover') && !e.target.closest('[data-rte-toggle]'))
            closePopovers();
    });

    editor.addEventListener('keyup',   saveRange);
    editor.addEventListener('mouseup', saveRange);
    editor.addEventListener('blur',    saveRange);
    editor.addEventListener('input',   sync);
    form.addEventListener('submit',    sync);
})();
