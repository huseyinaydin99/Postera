(() => {
    const feedContainer = document.querySelector('[data-home-feed]');
    if (!feedContainer) return;

    const loadingIndicator = document.querySelector('[data-feed-loading]');
    const emptyState = document.querySelector('[data-feed-empty]');

    let hasMore = feedContainer.dataset.hasMore === 'true';
    let currentOffset = parseInt(feedContainer.dataset.nextOffset, 10) || 6;
    let isLoading = false;

    const formatPostDate = (isoString) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        if (isNaN(date.getTime())) return '';
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${day}.${month}.${year} ${hours}:${minutes}`;
    };

    const escapeHtml = (unsafe) => {
        if (!unsafe) return '';
        return String(unsafe)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    };

    const getCsrfToken = () => {
        return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    };

    const getCsrfParameterName = () => {
        return '_csrf';
    };

    const createPostElement = (post) => {
        const article = document.createElement('article');
        article.className = 'timeline-post-card';
        article.id = `post-${post.id}`;

        const formattedDate = formatPostDate(post.createdAt);
        const formattedUpdatedDate = post.updatedAt ? formatPostDate(post.updatedAt) : '';

        // Header
        let menuHtml = '';
        if (post.ownedByCurrentUser) {
            const csrfToken = getCsrfToken();
            menuHtml = `
                <details class="post-menu">
                    <summary aria-label="Paylaşım seçenekleri">
                        <span class="material-symbols-outlined">more_horiz</span>
                    </summary>
                    <div class="post-menu-panel">
                        <button type="button" class="post-menu-item" disabled title="Güncelleme işlemi sonraki aşamada eklenecektir.">
                            <span class="material-symbols-outlined">edit</span> Düzenle
                        </button>
                        <form action="/timeline/${post.id}/delete?redirectUrl=/" method="post" data-confirm-action="İlgili post silinsin mi?">
                            <input type="hidden" name="${getCsrfParameterName()}" value="${csrfToken}">
                            <button type="submit" class="post-menu-item post-menu-danger">
                                <span class="material-symbols-outlined">delete</span> Sil
                            </button>
                        </form>
                    </div>
                </details>
            `;
        }

        const editedBadgeHtml = post.updatedAt
            ? `<span class="post-edited-badge" title="Son düzenleme: ${formattedUpdatedDate}">(Düzenlendi)</span>`
            : '';

        // Images
        let imagesHtml = '';
        if (post.imageUrls && post.imageUrls.length > 0) {
            const gridClass = post.imageUrls.length === 1 ? 'post-images-single' : 'post-images-grid';
            const imgs = post.imageUrls.map((url) => `<img src="${url}" alt="Paylaşım görseli" loading="lazy">`).join('');
            imagesHtml = `<div class="post-images ${gridClass}">${imgs}</div>`;
        }

        // Body Text
        let contentHtml = '';
        if (post.content && post.content.trim().length > 0) {
            contentHtml = `<div class="rich-message-body post-body-text">${post.content}</div>`;
        }

        article.innerHTML = `
            <div class="post-card-header">
                <div class="post-author-wrapper">
                    <img class="post-author-avatar"
                         src="${post.authorProfileImageUrl || '/images/default-avatar.svg'}"
                         alt="${escapeHtml(post.authorName)} profil fotoğrafı">
                    <div class="post-author-meta">
                        <span class="post-author-name">${escapeHtml(post.authorName)}</span>
                        <div class="post-time-meta">
                            <time datetime="${post.createdAt}">${formattedDate}</time>
                            ${editedBadgeHtml}
                        </div>
                    </div>
                </div>
                ${menuHtml}
            </div>
            ${contentHtml}
            ${imagesHtml}
            <div class="post-card-actions">
                <button type="button" class="post-action-btn" title="Beğen (Sonraki aşamada aktif edilecek)">
                    <span class="material-symbols-outlined">thumb_up</span>
                    <span>Beğen</span>
                </button>
                <button type="button" class="post-action-btn" title="Yorum Yap (Sonraki aşamada aktif edilecek)">
                    <span class="material-symbols-outlined">chat_bubble</span>
                    <span>Yorum Yaz</span>
                </button>
            </div>
        `;

        return article;
    };

    const loadMorePosts = async () => {
        if (isLoading || !hasMore) return;
        isLoading = true;
        if (loadingIndicator) loadingIndicator.hidden = false;

        try {
            const response = await fetch(`/api/feed?offset=${currentOffset}&limit=3`, {
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) throw new Error('Paylaşımlar yüklenemedi');
            const data = await response.json();

            if (data.posts && data.posts.length > 0) {
                if (emptyState) emptyState.remove();

                data.posts.forEach((post) => {
                    // Avoid duplicate post
                    if (!document.getElementById(`post-${post.id}`)) {
                        const postElement = createPostElement(post);
                        feedContainer.appendChild(postElement);
                    }
                });
            }

            currentOffset = data.nextOffset;
            hasMore = data.hasMore;
            feedContainer.dataset.nextOffset = currentOffset;
            feedContainer.dataset.hasMore = hasMore;
        } catch (err) {
            console.error('Akış yüklenirken hata oluştu:', err);
        } finally {
            isLoading = false;
            if (loadingIndicator) loadingIndicator.hidden = true;
        }
    };

    window.addEventListener('scroll', () => {
        if (!hasMore || isLoading) return;
        const scrollPosition = window.innerHeight + window.scrollY;
        const threshold = document.documentElement.scrollHeight - 350;
        if (scrollPosition >= threshold) {
            loadMorePosts();
        }
    }, { passive: true });
})();
