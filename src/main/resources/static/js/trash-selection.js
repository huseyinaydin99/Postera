(() => {
    const form = document.querySelector('[data-selection-form]');
    const toggle = document.querySelector('[data-selection-toggle]');
    if (!form || !toggle) return;

    const selectionBar = form.querySelector('[data-selection-bar]');
    const selectors = [...form.querySelectorAll('[data-message-selector]')];
    const checkboxes = [...form.querySelectorAll('[data-message-checkbox]')];
    const selectAll = form.querySelector('[data-select-all]');
    const deleteButton = form.querySelector('[data-delete-selected]');
    const selectedCount = form.querySelector('[data-selected-count]');
    const cancel = form.querySelector('[data-selection-cancel]');

    const updateSelection = () => {
        const count = checkboxes.filter((checkbox) => checkbox.checked).length;
        selectedCount.textContent = `${count} mesaj seçildi`;
        deleteButton.disabled = count === 0;
        selectAll.checked = count === checkboxes.length;
        selectAll.indeterminate = count > 0 && count < checkboxes.length;
    };

    const setSelectionMode = (active) => {
        selectionBar.hidden = !active;
        selectors.forEach((selector) => { selector.hidden = !active; });
        checkboxes.forEach((checkbox) => {
            checkbox.disabled = !active;
            if (!active) checkbox.checked = false;
        });
        selectAll.disabled = !active;
        if (!active) {
            selectAll.checked = false;
            selectAll.indeterminate = false;
        }
        toggle.hidden = active;
        updateSelection();
    };

    toggle.addEventListener('click', () => setSelectionMode(true));
    cancel.addEventListener('click', () => setSelectionMode(false));
    selectAll.addEventListener('change', () => {
        checkboxes.forEach((checkbox) => { checkbox.checked = selectAll.checked; });
        updateSelection();
    });
    checkboxes.forEach((checkbox) => checkbox.addEventListener('change', updateSelection));
})();
