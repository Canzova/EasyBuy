const BASE_URL = 'http://localhost:8080';
let _urls = [];

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    const icons = { success: 'fa-circle-check', error: 'fa-circle-xmark', info: 'fa-circle-info' };
    toast.className = `toast ${type}`;
    toast.innerHTML = `<i class="fa-solid ${icons[type]}"></i> ${message}`;
    toast.classList.remove('hidden');
    clearTimeout(window._toastTimer);
    window._toastTimer = setTimeout(() => toast.classList.add('hidden'), 3500);
}

async function fetchImages() {
    const raw = document.getElementById('objectKeys').value.trim();
    if (!raw) { showToast('Please enter at least one object key', 'error'); return; }

    const keys = raw.split('\n').map(k => k.trim()).filter(Boolean);

    try {
        const res = await fetch(`${BASE_URL}/user/get/image`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(keys),
        });
        if (!res.ok) throw new Error(`Error ${res.status}`);
        _urls = await res.json();

        renderGallery(_urls, keys);
        renderUrlList(_urls);
        showToast(`Fetched ${_urls.length} presigned URL(s)`, 'success');
    } catch (err) {
        showToast(err.message || 'Failed to fetch images', 'error');
    }
}

function renderGallery(urls, keys) {
    const section = document.getElementById('gallery-section');
    const gallery = document.getElementById('image-gallery');
    section.classList.remove('hidden');

    gallery.innerHTML = urls.map((url, i) => {
        const isImage = keys[i] && /\.(jpg|jpeg|png|gif|webp)$/i.test(keys[i]);
        return `
        <div class="gallery-item" data-index="${i}">
            ${isImage
                ? `<img src="${url}" alt="S3 Image" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex'"/>`
                : ''
            }
            <div class="gallery-no-img" style="${isImage ? 'display:none' : ''}">
                <i class="fa-solid fa-file"></i>
            </div>
            <div class="gallery-item-footer">
                <span>${keys[i] || `Item ${i + 1}`}</span><br/>
                <a href="${url}" target="_blank" onclick="event.stopPropagation()">
                    Open <i class="fa-solid fa-arrow-up-right-from-square"></i>
                </a>
            </div>
        </div>`;
    }).join('');

    // attach click via JS not inline onclick to avoid URL escaping issues
    gallery.querySelectorAll('.gallery-item').forEach(el => {
        el.addEventListener('click', () => {
            const idx = parseInt(el.dataset.index);
            openLightbox(_urls[idx]);
        });
    });
}

function renderUrlList(urls) {
    const section = document.getElementById('url-list-section');
    const list    = document.getElementById('url-list');
    section.classList.remove('hidden');

    list.innerHTML = urls.map((url, i) => `
        <li>
            <a href="${url}" target="_blank">${url.substring(0, 80)}...</a>
            <button class="copy-btn" data-index="${i}">
                <i class="fa-solid fa-copy"></i> Copy
            </button>
        </li>`).join('');

    list.querySelectorAll('.copy-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const url = _urls[parseInt(btn.dataset.index)];
            navigator.clipboard.writeText(url).then(() => {
                btn.innerHTML = '<i class="fa-solid fa-check"></i> Copied!';
                setTimeout(() => btn.innerHTML = '<i class="fa-solid fa-copy"></i> Copy', 2000);
            });
        });
    });
}

function openLightbox(url) {
    document.getElementById('lightbox-img').src = url;
    document.getElementById('lightbox').classList.remove('hidden');
}

function closeLightbox() {
    document.getElementById('lightbox').classList.add('hidden');
    document.getElementById('lightbox-img').src = '';
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('lightbox').addEventListener('click', function (e) {
        if (e.target === this) closeLightbox();
    });
});
