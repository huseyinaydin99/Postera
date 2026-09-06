document.addEventListener('DOMContentLoaded', () => {
    const toggleBtn = document.querySelector('[data-blocked-users-toggle]');
    const closeBtn = document.querySelector('[data-blocked-users-close]');
    const modal = document.getElementById('blockedUsersModal');
    const backdrop = document.getElementById('blockedUsersBackdrop');
    const listContainer = document.getElementById('blockedUsersList');

    if (!toggleBtn || !modal) return;

    const openModal = () => {
        backdrop.style.opacity = '1';
        backdrop.style.pointerEvents = 'auto';
        modal.classList.add('ped-visible');
    };

    const closeModal = () => {
        backdrop.style.opacity = '0';
        backdrop.style.pointerEvents = 'none';
        modal.classList.remove('ped-visible');
    };

    const getCsrfHeaders = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        return token && header ? { [header]: token } : {};
    };

    const fetchBlockedUsers = () => {
        listContainer.innerHTML = '<div class="blocked-users-loading" style="text-align:center;padding:1rem;color:var(--ink-500);">Yükleniyor…</div>';
        fetch('/api/blocked-users', { headers: { 'Accept': 'application/json' } })
            .then(res => res.json())
            .then(users => {
                if (!users || users.length === 0) {
                    listContainer.innerHTML = '<div class="blocked-users-empty" style="padding:1rem;text-align:center;color:var(--ink-500);">Engellediğiniz kimse yok.</div>';
                    return;
                }
                listContainer.innerHTML = users.map(u => `
                    <div class="blocked-user-item" style="display:flex;align-items:center;justify-content:space-between;padding:0.75rem;border:1px solid var(--line);border-radius:8px;background:var(--paper-raised);margin-bottom:0.5rem;">
                        <div style="display:flex;align-items:center;gap:0.75rem;">
                            <img src="${u.profileImageUrl || '/images/default-avatar.svg'}" alt="Avatar" style="width:40px;height:40px;border-radius:50%;object-fit:cover;">
                            <span style="font-weight:500;color:var(--ink-800);">${u.fullName}</span>
                        </div>
                        <button type="button" class="btn-unblock ped-fmt-btn" data-unblock-id="${u.id}" data-unblock-name="${u.fullName}" style="width:auto;padding:0 0.75rem;font-weight:500;">Engeli Kaldır</button>
                    </div>
                `).join('');

                listContainer.querySelectorAll('.btn-unblock').forEach(btn => {
                    btn.addEventListener('click', () => {
                        const id = btn.getAttribute('data-unblock-id');
                        const name = btn.getAttribute('data-unblock-name');
                        
                        Swal.fire({
                            title: 'Engeli Kaldır',
                            text: `${name} kişisinin engelini kaldırmak istediğinize emin misiniz?`,
                            icon: 'question',
                            showCancelButton: true,
                            confirmButtonColor: '#28a745',
                            cancelButtonColor: '#d33',
                            confirmButtonText: 'Evet, kaldır',
                            cancelButtonText: 'İptal'
                        }).then((result) => {
                            if (result.isConfirmed) {
                                fetch(`/api/blocked-users/${id}/unblock`, {
                                    method: 'POST',
                                    headers: getCsrfHeaders()
                                }).then(res => {
                                    if (res.ok) {
                                        fetchBlockedUsers(); // Refresh list
                                        Swal.fire('Başarılı', 'Engel kaldırıldı.', 'success');
                                    } else {
                                        Swal.fire('Hata', 'Bir hata oluştu.', 'error');
                                    }
                                });
                            }
                        });
                    });
                });
            })
            .catch(err => console.error(err));
    };

    toggleBtn.addEventListener('click', (e) => {
        e.preventDefault();
        openModal();
        fetchBlockedUsers();
    });

    closeBtn.addEventListener('click', () => {
        closeModal();
    });
    
    backdrop.addEventListener('click', () => {
        closeModal();
    });
});
