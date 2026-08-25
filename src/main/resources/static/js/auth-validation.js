(() => {
    const form = document.querySelector('[data-auth-form]');
    if (!form) return;
    const type = form.dataset.authForm;
    const email = form.querySelector('#email');
    const password = form.querySelector('#password');
    const confirmation = form.querySelector('#passwordConfirmation');
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
    const feedback = (field, message, valid) => {
        const node = document.getElementById(`${field.id}-feedback`);
        if (!node) return;
        node.textContent = message;
        node.classList.toggle('is-valid', Boolean(valid && field.value));
        node.classList.toggle('is-invalid', Boolean(!valid && field.value));
        field.classList.toggle('is-valid', Boolean(valid && field.value));
        field.classList.toggle('is-invalid', Boolean(!valid && field.value));
    };
    const validateEmail = () => {
        const valid = emailPattern.test(email.value.trim());
        const message = valid || !email.value ? '' : 'Geçerli bir e-posta adresi girin.';
        email.setCustomValidity(message); feedback(email, message || (email.value ? 'E-posta adresi uygun.' : ''), valid); return valid;
    };
    const count = (expression, value) => (value.match(expression) || []).length;
    const passwordRules = (value) => ({ length: value.length >= 8, upper: count(/[A-Z]/g, value) >= 3, lower: count(/[a-z]/g, value) >= 3, digit: count(/\d/g, value) >= 3, special: count(/[^A-Za-z0-9\s]/g, value) >= 3 });
    const validatePassword = () => {
        if (type !== 'register') {
            const valid = password.value.trim().length > 0; const message = valid || !password.value ? '' : 'Şifre alanı zorunludur.';
            password.setCustomValidity(message); feedback(password, message, valid); return valid;
        }
        const rules = passwordRules(password.value);
        document.querySelectorAll('[data-rule]').forEach((item) => item.classList.toggle('is-met', rules[item.dataset.rule]));
        const valid = Object.values(rules).every(Boolean); const message = valid || !password.value ? '' : 'Şifre, tüm güvenlik kurallarını karşılamalıdır.';
        password.setCustomValidity(message); feedback(password, message || (password.value ? 'Güçlü şifre.' : ''), valid); return valid;
    };
    const validateConfirmation = () => {
        if (!confirmation) return true;
        const valid = confirmation.value.length > 0 && confirmation.value === password.value;
        const message = valid || !confirmation.value ? '' : 'Şifreler birbiriyle eşleşmiyor.';
        confirmation.setCustomValidity(message); feedback(confirmation, message || (confirmation.value ? 'Şifreler eşleşiyor.' : ''), valid); return valid;
    };
    const validate = () => [validateEmail(), validatePassword(), validateConfirmation()].every(Boolean);
    email.addEventListener('input', validateEmail);
    password.addEventListener('input', () => { validatePassword(); validateConfirmation(); });
    confirmation?.addEventListener('input', validateConfirmation);
    form.addEventListener('submit', (event) => { if (!validate()) { event.preventDefault(); form.querySelector(':invalid')?.focus(); } });
})();
