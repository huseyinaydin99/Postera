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

    const reactionLabels = { LIKE: 'Beğen', LAUGH: 'Gülme', ANGRY: 'Kızgınlık', SURPRISED: 'Şaşkınlık', SUPPORT: 'Yanındayım', HEART: 'Kalp' };
    const reactionPicker = () => `<div class="post-reaction-picker" role="menu" aria-label="Emoji tepkileri">
        <button type="button" data-post-reaction="LAUGH" title="Gülme" aria-label="Gülme">😂</button><button type="button" data-post-reaction="ANGRY" title="Kızgınlık" aria-label="Kızgınlık">😠</button><button type="button" data-post-reaction="SURPRISED" title="Şaşkınlık" aria-label="Şaşkınlık">😮</button><button type="button" data-post-reaction="SUPPORT" title="Yanındayım" aria-label="Yanındayım">🤝</button><button type="button" data-post-reaction="HEART" title="Kalp" aria-label="Kalp">❤️</button></div>`;
    const reactionSummary = (reactions = []) => reactions.length ? `<div class="post-reaction-summary" data-post-reaction-summary>${reactions.map((reaction) => `<span class="post-reaction-count" title="${escapeHtml(reaction.label)}"><span>${reaction.emoji}</span><span>${reaction.count}</span></span>`).join('')}</div>` : '<div class="post-reaction-summary" data-post-reaction-summary hidden></div>';

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
            ${reactionSummary(post.reactions)}
            <div class="post-card-actions">
                <div class="post-reaction-control" data-post-id="${post.id}">
                <button type="button" class="post-action-btn post-like-btn ${post.currentUserReaction === 'LIKE' ? 'is-active' : ''}" data-post-reaction="LIKE" title="Beğen">
                    <span class="material-symbols-outlined">thumb_up</span>
                    <span>Beğen</span>
                </button>
                ${reactionPicker()}
                </div>
                <button type="button" class="post-action-btn" title="Yorum Yap (Sonraki aşamada aktif edilecek)">
                    <span class="material-symbols-outlined">chat_bubble</span>
                    <span>Yorum Yaz</span>
                </button>
            </div>
        `;

        return article;
    };

    const csrfHeaders = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    };

    const updateReactionView = (control, data) => {
        const summary = control.closest('.timeline-post-card')?.querySelector('[data-post-reaction-summary]');
        if (summary) {
            summary.hidden = !data.reactions?.length;
            summary.innerHTML = (data.reactions || []).map((reaction) => `<span class="post-reaction-count" title="${escapeHtml(reaction.label)}"><span>${reaction.emoji}</span><span>${reaction.count}</span></span>`).join('');
        }
        const likeButton = control.querySelector('[data-post-reaction="LIKE"]');
        if (likeButton) likeButton.classList.toggle('is-active', data.currentUserReaction === 'LIKE');
        control.querySelectorAll('[data-post-reaction]').forEach((button) => button.classList.toggle('is-selected', button.dataset.postReaction === data.currentUserReaction));
    };

    feedContainer.addEventListener('click', async (event) => {
        const button = event.target.closest('[data-post-reaction]');
        if (!button) return;
        const control = button.closest('[data-post-id]');
        if (!control || button.disabled) return;
        button.disabled = true;
        try {
            const response = await fetch(`/timeline/api/posts/${control.dataset.postId}/reactions`, {
                method: 'POST', headers: { ...csrfHeaders(), 'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json' },
                body: new URLSearchParams({ reaction: button.dataset.postReaction })
            });
            if (!response.ok) throw new Error('Tepki kaydedilemedi.');
            updateReactionView(control, await response.json());
        } catch (error) {
            window.PosteraAlerts?.toast?.('Tepkiniz kaydedilemedi. Lütfen tekrar deneyin.', 'error');
        } finally {
            button.disabled = false;
        }
    });

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
