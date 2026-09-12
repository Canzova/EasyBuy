const BASE_URL = 'http://localhost:8080';

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    const icons = { success: 'fa-circle-check', error: 'fa-circle-xmark', info: 'fa-circle-info' };
    toast.className = `toast ${type}`;
    toast.innerHTML = `<i class="fa-solid ${icons[type]}"></i> ${message}`;
    toast.classList.remove('hidden');
    clearTimeout(window._toastTimer);
    window._toastTimer = setTimeout(() => toast.classList.add('hidden'), 3500);
}

function animateCount(el, target, duration = 1000) {
    const start = 0;
    const step = Math.ceil(target / (duration / 16));
    let current = start;
    const timer = setInterval(() => {
        current = Math.min(current + step, target);
        el.textContent = current.toLocaleString();
        if (current >= target) clearInterval(timer);
    }, 16);
}

document.addEventListener('DOMContentLoaded', async () => {
    const usersEl  = document.getElementById('stat-users');
    const uploadsEl = document.getElementById('stat-uploads');
    const statusEl  = document.getElementById('stat-status');

    try {
        const res = await fetch(`${BASE_URL}/user/stats`);
        if (!res.ok) throw new Error();
        const data = await res.json();

        animateCount(usersEl, data.users);
        animateCount(uploadsEl, data.uploads);
        statusEl.textContent = 'Live';
        statusEl.style.color = 'var(--green)';

    } catch {
        usersEl.textContent  = 'N/A';
        uploadsEl.textContent = 'N/A';
        statusEl.textContent  = 'Offline';
        statusEl.style.color  = 'var(--red)';
    }
});
