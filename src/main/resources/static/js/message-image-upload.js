(() => {
    const form = document.querySelector('#reply-form');
    const input = document.querySelector('[data-message-images]');
    const previewContainer = document.querySelector('[data-image-preview-container]');
    const error = document.querySelector('[data-image-upload-error]');
    const progress = document.querySelector('[data-upload-progress]');
    const bar = document.querySelector('[data-upload-progress-bar]');
    if (!form || !input || !error || !progress || !bar) return;

    let selectedFiles = [];
    let previewUrls = [];

    const clearPreviewUrls = () => {
        previewUrls.forEach((url) => URL.revokeObjectURL(url));
        previewUrls = [];
    };

    const syncFiles = () => {
        const dt = new DataTransfer();
        selectedFiles.forEach((file) => dt.items.add(file));
        input.files = dt.files;
    };

    const validate = () => {
        if (selectedFiles.length === 0) {
            error.hidden = true;
            error.textContent = '';
            return true;
        }
        const tooMany = selectedFiles.length > 2;
        const tooLarge = selectedFiles.some((file) => file.size > 5 * 1024 * 1024);
        const invalid = tooMany || tooLarge;
        error.hidden = !invalid;
        if (tooMany) {
            error.textContent = 'En fazla 2 görsel ekleyebilirsiniz.';
        } else if (tooLarge) {
            error.textContent = 'Her görsel en fazla 5 MB olabilir.';
        } else {
            error.textContent = '';
        }
        return !invalid;
    };

    const renderPreviews = () => {
        if (!previewContainer) return;
        clearPreviewUrls();
        previewContainer.innerHTML = '';

        selectedFiles.forEach((file, index) => {
            const url = URL.createObjectURL(file);
            previewUrls.push(url);

            const item = document.createElement('div');
            item.className = 'image-preview-item';

            const img = document.createElement('img');
            img.src = url;
            img.alt = file.name || `Seçilen görsel ${index + 1}`;

            const removeBtn = document.createElement('button');
            removeBtn.type = 'button';
            removeBtn.className = 'image-preview-remove';
            removeBtn.title = 'Görseli kaldır';
            removeBtn.setAttribute('aria-label', `${file.name || 'Görseli'} kaldır`);
            removeBtn.textContent = '×';
            removeBtn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                selectedFiles.splice(index, 1);
                if (selectedFiles.length === 0) {
                    input.value = '';
                }
                syncFiles();
                renderPreviews();
                validate();
            });

            item.appendChild(img);
            item.appendChild(removeBtn);
            previewContainer.appendChild(item);
        });
    };

    input.addEventListener('click', () => {
        input.value = '';
    });

    input.addEventListener('change', () => {
        const newFiles = Array.from(input.files);
        if (!newFiles.length) return;
        selectedFiles = [...selectedFiles, ...newFiles];
        syncFiles();
        renderPreviews();
        validate();
    });

    form.addEventListener('submit', (event) => {
        if (!selectedFiles.length || !validate()) {
            if (!validate()) event.preventDefault();
            return;
        }
        event.preventDefault();
        progress.hidden = false;
        bar.style.width = '0%';
        const request = new XMLHttpRequest();
        request.open('POST', form.action);
        request.upload.onprogress = (progressEvent) => {
            if (progressEvent.lengthComputable) {
                bar.style.width = `${Math.round((progressEvent.loaded / progressEvent.total) * 100)}%`;
            }
        };
        request.onload = () => {
            window.location.assign(request.responseURL || window.location.href);
        };
        request.onerror = () => {
            error.hidden = false;
            error.textContent = 'Görseller yüklenemedi. Lütfen yeniden deneyin.';
            progress.hidden = true;
        };
        request.send(new FormData(form));
    });
})();
