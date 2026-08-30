(() => {
    const group = document.querySelector('[data-verification-code]');
    const valueInput = document.querySelector('[data-verification-code-value]');
    if (!group || !valueInput) return;

    const fields = [...group.querySelectorAll('.verification-digit')];
    const digitsOnly = (value) => value.replace(/\D/g, '').slice(0, fields.length);
    const sync = () => { valueInput.value = fields.map(field => field.value).join(''); };
    const fillFrom = (value, startIndex = 0) => {
        const digits = digitsOnly(value);
        if (!digits) return;
        fields.slice(startIndex).forEach((field, index) => { field.value = digits[index] || ''; });
        sync();
        fields[Math.min(startIndex + digits.length, fields.length - 1)].focus();
    };

    fields.forEach((field, index) => {
        field.addEventListener('input', () => {
            const digits = digitsOnly(field.value);
            if (digits.length > 1) fillFrom(digits, index);
            else {
                field.value = digits;
                sync();
                if (digits && index < fields.length - 1) fields[index + 1].focus();
            }
        });
        field.addEventListener('keydown', (event) => {
            if (event.key === 'Backspace' && !field.value && index > 0) {
                fields[index - 1].focus();
                fields[index - 1].select();
            }
            if (event.key === 'ArrowLeft' && index > 0) fields[index - 1].focus();
            if (event.key === 'ArrowRight' && index < fields.length - 1) fields[index + 1].focus();
        });
        field.addEventListener('paste', (event) => {
            event.preventDefault();
            fillFrom(event.clipboardData?.getData('text') || '', index);
        });
    });

    group.closest('form')?.addEventListener('submit', (event) => {
        sync();
        if (!/^\d{6}$/.test(valueInput.value)) {
            event.preventDefault();
            group.classList.add('has-error');
            fields.find(field => !field.value)?.focus();
        }
    });
})();
