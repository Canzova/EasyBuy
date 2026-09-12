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

document.addEventListener('DOMContentLoaded', () => {

    // Toggle label
    document.getElementById('up-isProfile').addEventListener('change', function () {
        document.getElementById('toggle-label').textContent = this.checked ? 'Yes' : 'No';
    });

    // Drop zone
    const zone        = document.getElementById('upDropZone');
    const input       = document.getElementById('up-files');
    const dropContent = document.getElementById('upDropContent');
    const chips       = document.getElementById('file-chips');

    zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('dragover'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('dragover'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('dragover');
        if (e.dataTransfer.files.length) {
            const dt = new DataTransfer();
            Array.from(e.dataTransfer.files).forEach(f => dt.items.add(f));
            input.files = dt.files;
            renderChips(e.dataTransfer.files);
        }
    });
    input.addEventListener('change', () => { if (input.files.length) renderChips(input.files); });

    function renderChips(files) {
        chips.innerHTML = Array.from(files).map(f => `
            <div class="chip">
                <i class="fa-solid ${f.type.startsWith('video') ? 'fa-film' : 'fa-image'}"></i>
                ${f.name}
            </div>`).join('');
        dropContent.querySelector('p').textContent = `${files.length} file(s) selected`;
    }

    // Submit — calls POST /user/create with full userRequestDTO + images
    document.getElementById('uploadForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const username = document.getElementById('up-username').value.trim();
        const name     = document.getElementById('up-name').value.trim();
        const password = document.getElementById('up-password').value;

        if (!username) { showToast('Username is required', 'error'); return; }
        if (!name)     { showToast('Full name is required', 'error'); return; }
        if (!password || password.length < 6) { showToast('Password must be at least 6 characters', 'error'); return; }
        if (!input.files.length) { showToast('Please select at least one file', 'error'); return; }

        const btn      = document.getElementById('upSubmitBtn');
        const icon     = document.getElementById('upSubmitIcon');
        const text     = document.getElementById('upSubmitText');

        btn.disabled = true;
        icon.className = 'fa-solid fa-spinner fa-spin';
        text.textContent = ' Uploading...';

        try {
            const formData = new FormData();
            const userRequestDTO = { username, name, password };
            formData.append('userRequestDTO', new Blob([JSON.stringify(userRequestDTO)], { type: 'application/json' }));
            formData.append('images', input.files[0]);

            const res = await fetch(`${BASE_URL}/user/create`, { method: 'POST', body: formData });
            if (!res.ok) {
                const errText = await res.text();
                throw new Error(errText || `Error ${res.status}`);
            }

            icon.className = 'fa-solid fa-circle-check';
            text.textContent = ' Upload Successful!';
            showToast('Upload successful!', 'success');
            chips.innerHTML = '';
            dropContent.querySelector('p').innerHTML = 'Drag & drop or <span>browse</span>';
            document.getElementById('uploadForm').reset();
            // restore button after 2s
            setTimeout(() => {
                btn.disabled = false;
                icon.className = 'fa-solid fa-cloud-arrow-up';
                text.textContent = ' Upload';
            }, 2000);

        } catch (err) {
            showToast(err.message || 'Upload failed', 'error');
            btn.disabled = false;
            icon.className = 'fa-solid fa-cloud-arrow-up';
            text.textContent = ' Upload';
        }
    });
});
