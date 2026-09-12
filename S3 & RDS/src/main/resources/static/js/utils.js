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

function setupDropZone(dropZoneId, inputId, onFile) {
    const zone = document.getElementById(dropZoneId);
    const input = document.getElementById(inputId);
    if (!zone || !input) return;
    zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('dragover'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('dragover'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('dragover');
        if (e.dataTransfer.files.length) { input.files = e.dataTransfer.files; onFile(e.dataTransfer.files); }
    });
    input.addEventListener('change', () => { if (input.files.length) onFile(input.files); });
}
