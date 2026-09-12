const BASE_URL = 'http://localhost:8080';
let _profileImageUrl = null;

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    const icons = { success: 'fa-circle-check', error: 'fa-circle-xmark', info: 'fa-circle-info' };
    toast.className = `toast ${type}`;
    toast.innerHTML = `<i class="fa-solid ${icons[type]}"></i> ${message}`;
    toast.classList.remove('hidden');
    clearTimeout(window._toastTimer);
    window._toastTimer = setTimeout(() => toast.classList.add('hidden'), 3500);
}

function openLightbox() {
    if (!_profileImageUrl) return;
    document.getElementById('lightbox-img').src = _profileImageUrl;
    document.getElementById('lightbox').classList.remove('hidden');
}

function closeLightbox() {
    document.getElementById('lightbox').classList.add('hidden');
    document.getElementById('lightbox-img').src = '';
}

function copyProfileUrl() {
    if (_profileImageUrl) {
        navigator.clipboard.writeText(_profileImageUrl)
            .then(() => showToast('URL copied!', 'success'));
    }
}

document.addEventListener('DOMContentLoaded', async () => {

    // Wire lightbox close button and backdrop click
    document.getElementById('lightboxClose').addEventListener('click', closeLightbox);
    document.getElementById('lightbox').addEventListener('click', function (e) {
        if (e.target === this) closeLightbox();
    });

    // Wire copy URL button
    document.getElementById('copyUrlBtn').addEventListener('click', copyProfileUrl);

    // Wire copy key button
    document.getElementById('copyKeyBtn').addEventListener('click', () => {
        const key = document.getElementById('profileKey').textContent;
        if (key && key !== '—') {
            navigator.clipboard.writeText(key).then(() => {
                const btn = document.getElementById('copyKeyBtn');
                btn.innerHTML = '<i class="fa-solid fa-check"></i> Copied!';
                setTimeout(() => btn.innerHTML = '<i class="fa-solid fa-copy"></i> Copy', 2000);
            });
        }
    });

    const raw = localStorage.getItem('lastCreatedUser');

    if (!raw) {
        document.getElementById('profileWrapper').classList.add('hidden');
        document.getElementById('emptyState').classList.remove('hidden');
        return;
    }

    const user = JSON.parse(raw);
    document.getElementById('profileWrapper').classList.remove('hidden');
    document.getElementById('emptyState').classList.add('hidden');

    // Fill profile info
    document.getElementById('profileName').textContent     = user.name || '—';
    document.getElementById('profileUsername').textContent = `@${user.username}`;
    document.getElementById('profileBio').textContent      = user.bio || 'No bio provided.';
    document.getElementById('profileId').textContent       = user.id;
    document.getElementById('profileKey').textContent      = user.profilePicture || '—';

    if (!user.profilePicture) return;

    try {
        const res = await fetch(`${BASE_URL}/user/get/image`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify([user.profilePicture]),
        });
        if (!res.ok) throw new Error(`Error ${res.status}`);
        const urls = await res.json();
        _profileImageUrl = urls[0];

        // Avatar
        const avatar = document.getElementById('profileAvatar');
        avatar.innerHTML = `<img src="${_profileImageUrl}" alt="avatar"/>`;
        avatar.style.cursor = 'pointer';
        avatar.addEventListener('click', openLightbox);

        // Full image section
        document.getElementById('profileImageSection').classList.remove('hidden');
        const fullImg = document.getElementById('profileImageFull');
        fullImg.src = _profileImageUrl;
        fullImg.style.cursor = 'pointer';
        fullImg.addEventListener('click', openLightbox);
        const link = document.getElementById('profileImageLink');
        link.href = _profileImageUrl;
        link.classList.remove('hidden');

    } catch (err) {
        showToast('Could not load profile picture', 'error');
    }
});
