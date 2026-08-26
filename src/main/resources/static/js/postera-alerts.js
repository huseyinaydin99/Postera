(() => {
    const options = (text) => ({
        title: 'Emin misiniz?',
        text,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Evet, Sil',
        cancelButtonText: 'Vazgeç',
        customClass: {
            popup: 'postera-swal-popup',
            title: 'postera-swal-title',
            htmlContainer: 'postera-swal-text',
            confirmButton: 'postera-swal-btn postera-swal-confirm',
            cancelButton: 'postera-swal-btn postera-swal-cancel',
            icon: 'postera-swal-icon'
        },
        buttonsStyling: false,
        width: '320px',
        padding: '1rem'
    });
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
    const shared = new URLSearchParams(window.location.search).get('shared');
    if (shared !== null) toast('Paylaşımınız başarıyla yayınlandı!', 'success');
    const deleted = new URLSearchParams(window.location.search).get('deleted');
    if (deleted !== null) toast('Paylaşım başarıyla silindi.', 'success');
})();
