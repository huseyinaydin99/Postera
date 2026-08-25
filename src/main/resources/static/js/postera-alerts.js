(() => {
    const options = (text) => ({ title: 'İşlemi onaylıyor musunuz?', text, icon: 'warning', showCancelButton: true, confirmButtonText: 'Evet', cancelButtonText: 'Hayır', confirmButtonColor: '#b45309', cancelButtonColor: '#5b6b85' });
    const toast = (title, icon = 'info') => window.Swal?.fire({toast: true, position: 'bottom-end', icon, title, showConfirmButton: false, timer: 3000, timerProgressBar: true});
    document.addEventListener('submit', async (event) => {
        const form = event.target.closest('form[data-confirm-action]');
        if (!form || form.dataset.confirmed === 'true') return;
        event.preventDefault();
        if (!window.Swal) { if (window.confirm(form.dataset.confirmAction)) { form.dataset.confirmed = 'true'; form.requestSubmit(); } return; }
        const result = await window.Swal.fire(options(form.dataset.confirmAction));
        if (result.isConfirmed) { form.dataset.confirmed = 'true'; form.requestSubmit(); }
        else if (result.dismiss === window.Swal.DismissReason.cancel) toast('İşlem iptal edildi.');
    });
    const sentTo = new URLSearchParams(window.location.search).get('sentTo');
    if (sentTo) toast(`Mesajınız ${sentTo} kişisine gönderildi.`, 'success');
})();
