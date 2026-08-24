(() => {
    const form = document.querySelector('#reply-form');
    const input = document.querySelector('[data-message-images]');
    const error = document.querySelector('[data-image-upload-error]');
    const progress = document.querySelector('[data-upload-progress]');
    const bar = document.querySelector('[data-upload-progress-bar]');
    if (!form || !input || !error || !progress || !bar) return;
    const validate = () => {
        const invalid = [...input.files].length > 2 || [...input.files].some((file) => file.size > 5 * 1024 * 1024);
        error.hidden = !invalid;
        error.textContent = [...input.files].length > 2 ? 'En fazla 2 görsel ekleyebilirsiniz.' : 'Her görsel en fazla 5 MB olabilir.';
        return !invalid;
    };
    input.addEventListener('change', validate);
    form.addEventListener('submit', (event) => {
        if (!input.files.length || !validate()) { if (!validate()) event.preventDefault(); return; }
        event.preventDefault(); progress.hidden = false; bar.style.width = '0%';
        const request = new XMLHttpRequest();
        request.open('POST', form.action);
        request.upload.onprogress = (progressEvent) => { if (progressEvent.lengthComputable) bar.style.width = `${Math.round(progressEvent.loaded / progressEvent.total * 100)}%`; };
        request.onload = () => { window.location.assign(request.responseURL || window.location.href); };
        request.onerror = () => { error.hidden = false; error.textContent = 'Görseller yüklenemedi. Lütfen yeniden deneyin.'; progress.hidden = true; };
        request.send(new FormData(form));
    });
})();
