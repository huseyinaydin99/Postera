(() => {
    const forms = document.querySelectorAll('form');

    const showSwalWarning = (title, text) => {
        if (window.Swal) {
            window.Swal.fire({
                title: title,
                text: text,
                icon: 'warning',
                confirmButtonText: 'Tamam',
                customClass: {
                    popup: 'postera-swal-popup',
                    title: 'postera-swal-title',
                    htmlContainer: 'postera-swal-text',
                    confirmButton: 'postera-swal-btn postera-swal-confirm',
                    icon: 'postera-swal-icon'
                },
                buttonsStyling: false,
                width: '320px',
                padding: '1rem'
            });
        } else {
            alert(text);
        }
    };

    const formatBytes = (bytes) => {
        if (bytes < 1024) return bytes + ' B';
        const k = 1024;
        const sizes = ['KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k)) - 1;
        return (bytes / Math.pow(k, i + 1)).toFixed(1) + ' ' + sizes[i];
    };

    const progress = document.querySelector('[data-upload-progress]');
    const bar = document.querySelector('[data-upload-progress-bar]');
    const progressLabel = document.querySelector('[data-upload-progress-label]');

    forms.forEach((form) => {
        const imageInput = form.querySelector('[data-message-images]');
        const fileInput = form.querySelector('[data-message-file]');

        if (!imageInput && !fileInput) return;

        // Image Handling
        const imagePreviewContainer = form.querySelector('[data-image-preview-container]');
        const imageError = form.querySelector('[data-image-upload-error]');
        let selectedImages = [];
        let previewUrls = [];

        const clearPreviewUrls = () => {
            previewUrls.forEach((url) => URL.revokeObjectURL(url));
            previewUrls = [];
        };

        const syncImages = () => {
            if (!imageInput) return;
            const dt = new DataTransfer();
            selectedImages.forEach((file) => dt.items.add(file));
            imageInput.files = dt.files;
        };

        const validateImages = () => {
            if (!imageError) return true;
            if (selectedImages.length === 0) {
                imageError.hidden = true;
                imageError.textContent = '';
                return true;
            }
            const tooMany = selectedImages.length > 2;
            const tooLarge = selectedImages.some((file) => file.size > 5 * 1024 * 1024);
            const invalid = tooMany || tooLarge;
            imageError.hidden = !invalid;
            if (tooMany) {
                imageError.textContent = 'En fazla 2 görsel ekleyebilirsiniz.';
            } else if (tooLarge) {
                imageError.textContent = 'Her görsel en fazla 5 MB olabilir.';
            } else {
                imageError.textContent = '';
            }
            return !invalid;
        };

        const renderImagePreviews = () => {
            if (!imagePreviewContainer) return;
            clearPreviewUrls();
            imagePreviewContainer.innerHTML = '';

            selectedImages.forEach((file, index) => {
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
                    selectedImages.splice(index, 1);
                    if (selectedImages.length === 0 && imageInput) {
                        imageInput.value = '';
                    }
                    syncImages();
                    renderImagePreviews();
                    validateImages();
                });

                item.appendChild(img);
                item.appendChild(removeBtn);
                imagePreviewContainer.appendChild(item);
            });
        };

        if (imageInput) {
            imageInput.addEventListener('click', () => {
                imageInput.value = '';
            });

            imageInput.addEventListener('change', () => {
                const newFiles = Array.from(imageInput.files || []);
                if (!newFiles.length) return;
                selectedImages = [...selectedImages, ...newFiles];
                syncImages();
                renderImagePreviews();
                validateImages();
            });
        }

        // File Attachment Handling
        const filePreviewContainer = form.querySelector('[data-file-preview-container]');
        const fileOriginalName = form.querySelector('[data-file-original-name]');
        const fileSizeElem = form.querySelector('[data-file-size]');
        const fileAliasInput = form.querySelector('[data-file-alias-input]');
        const fileRemoveBtn = form.querySelector('[data-file-remove]');
        const fileError = form.querySelector('[data-file-upload-error]');
        let selectedFile = null;

        const MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB

        const renderFilePreview = () => {
            if (!filePreviewContainer || !selectedFile) return;
            if (fileOriginalName) fileOriginalName.textContent = selectedFile.name;
            if (fileSizeElem) fileSizeElem.textContent = formatBytes(selectedFile.size);
            if (fileAliasInput && !fileAliasInput.value.trim()) {
                const baseName = selectedFile.name.replace(/\.[^/.]+$/, '');
                fileAliasInput.value = baseName;
            }
            filePreviewContainer.hidden = false;
        };

        const clearFile = () => {
            selectedFile = null;
            if (fileInput) fileInput.value = '';
            if (fileAliasInput) fileAliasInput.value = '';
            if (filePreviewContainer) filePreviewContainer.hidden = true;
            if (fileError) {
                fileError.hidden = true;
                fileError.textContent = '';
            }
        };

        if (fileInput) {
            fileInput.addEventListener('click', (e) => {
                if (selectedFile) {
                    e.preventDefault();
                    showSwalWarning('Uyarı', 'Bir mesaja en fazla 1 dosya ekleyebilirsiniz. Yeni bir dosya seçmek için lütfen önce mevcut dosyayı kaldırın.');
                }
            });

            fileInput.addEventListener('change', () => {
                const files = Array.from(fileInput.files || []);
                if (!files.length) return;

                if (files.length > 1) {
                    fileInput.value = '';
                    showSwalWarning('Uyarı', 'Bir mesaja en fazla 1 dosya ekleyebilirsiniz.');
                    return;
                }

                const file = files[0];
                if (file.size > MAX_FILE_SIZE) {
                    fileInput.value = '';
                    showSwalWarning('Dosya Boyutu Sınırı Aşıldı', `Maksimum dosya boyutu 25 MB olabilir. Seçilen dosya boyutu: ${formatBytes(file.size)}.`);
                    return;
                }

                selectedFile = file;
                renderFilePreview();
            });

            fileRemoveBtn?.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                clearFile();
            });
        }

        // Form Submit with Progress Bar
        form.addEventListener('submit', (event) => {
            const hasImages = selectedImages.length > 0;
            const hasFile = Boolean(selectedFile);

            if (!hasImages && !hasFile) {
                return; // Normal form submission for text-only messages
            }

            if (hasImages && !validateImages()) {
                event.preventDefault();
                return;
            }

            event.preventDefault();
            if (progress && bar) {
                progress.hidden = false;
                bar.style.width = '0%';
                if (progressLabel) {
                    if (hasImages && hasFile) {
                        progressLabel.textContent = 'Dosya ve görseller yükleniyor…';
                    } else if (hasFile) {
                        progressLabel.textContent = 'Dosya yükleniyor…';
                    } else {
                        progressLabel.textContent = 'Görseller yükleniyor…';
                    }
                }
            }

            const formData = new FormData(form);
            const submitter = event.submitter;
            let actionUrl = form.getAttribute('action') || form.action;
            if (submitter && submitter.hasAttribute('formaction')) {
                actionUrl = submitter.getAttribute('formaction');
            }
            if (submitter && submitter.name) {
                formData.append(submitter.name, submitter.value);
            }

            const request = new XMLHttpRequest();
            request.open('POST', actionUrl);
            request.upload.onprogress = (progressEvent) => {
                if (progressEvent.lengthComputable && bar) {
                    bar.style.width = `${Math.round((progressEvent.loaded / progressEvent.total) * 100)}%`;
                }
            };
            request.onload = () => {
                if (request.status >= 200 && request.status < 400) {
                    if (request.responseURL && request.responseURL !== actionUrl && !request.responseURL.endsWith(actionUrl)) {
                        window.location.href = request.responseURL;
                    } else if (request.responseText && (request.responseText.includes('<!DOCTYPE') || request.responseText.includes('<html'))) {
                        document.open();
                        document.write(request.responseText);
                        document.close();
                    } else {
                        window.location.href = request.responseURL || window.location.href;
                    }
                } else {
                    const errElem = fileError || imageError;
                    if (errElem) {
                        errElem.hidden = false;
                        errElem.textContent = 'Yükleme sırasında hata oluştu. Lütfen tekrar deneyin.';
                    }
                    if (progress) progress.hidden = true;
                }
            };
            request.onerror = () => {
                const errElem = fileError || imageError;
                if (errElem) {
                    errElem.hidden = false;
                    errElem.textContent = 'Yükleme sırasında hata oluştu. Lütfen tekrar deneyin.';
                }
                if (progress) progress.hidden = true;
            };
            request.send(formData);
        });
    });
})();
