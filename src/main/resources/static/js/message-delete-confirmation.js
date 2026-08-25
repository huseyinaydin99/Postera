(() => {
    document.addEventListener('click', async (event) => {
        const button = event.target.closest('[data-delete-form]');
        if (!button) return;
        const form = document.getElementById(button.dataset.deleteForm);
        if (!form) return;
        const options = { title: 'Mesaj silinsin mi?', text: 'Mesaj çöp kutusuna taşınacak.', icon: 'warning', showCancelButton: true, confirmButtonText: 'Evet', cancelButtonText: 'Hayır', confirmButtonColor: '#b45309', cancelButtonColor: '#5b6b85' };
        if (!window.Swal) { if (window.confirm('Mesaj çöp kutusuna taşınsın mı?')) form.submit(); return; }
        const result = await window.Swal.fire(options);
        if (result.isConfirmed) form.submit();
        else if (result.dismiss === window.Swal.DismissReason.cancel) window.Swal.fire({toast: true, position: 'bottom-end', icon: 'info', title: 'Silme işlemi iptal edildi.', showConfirmButton: false, timer: 2600, timerProgressBar: true});
    });
})();
