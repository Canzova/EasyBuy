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

    // Bio char counter
    document.getElementById('bio').addEventListener('input', function () {
        document.getElementById('bio-count').textContent = `${this.value.length} / 5000`;
    });

    // Password toggle
    window.togglePassword = function () {
        const input = document.getElementById('password');
        const icon = document.getElementById('eye-icon');
        if (input.type === 'password') {
            input.type = 'text';
            icon.className = 'fa-solid fa-eye-slash';
        } else {
            input.type = 'password';
            icon.className = 'fa-solid fa-eye';
        }
    };

    // Drop zone
    const dropZone    = document.getElementById('dropZone');
    const fileInput   = document.getElementById('images');
    const preview     = document.getElementById('imagePreview');
    const previewWrap = document.getElementById('previewWrap');
    const dropContent = document.getElementById('dropContent');
    const removeBtn   = document.getElementById('removePreview');

    dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.classList.add('dragover'); });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
    dropZone.addEventListener('drop', e => {
        e.preventDefault();
        dropZone.classList.remove('dragover');
        const file = e.dataTransfer.files[0];
        if (file) {
            // assign dropped file to input via DataTransfer
            const dt = new DataTransfer();
            dt.items.add(file);
            fileInput.files = dt.files;
            showPreview(file);
        }
    });

    fileInput.addEventListener('change', () => {
        if (fileInput.files[0]) showPreview(fileInput.files[0]);
    });

    removeBtn.addEventListener('click', () => {
        fileInput.value = '';
        preview.src = '';
        previewWrap.classList.add('hidden');
        dropZone.classList.remove('has-preview');
        dropContent.querySelector('p').innerHTML = 'Drag & drop or <span>browse</span>';
    });

    function showPreview(file) {
        const reader = new FileReader();
        reader.onload = e => {
            preview.src = e.target.result;
            previewWrap.classList.remove('hidden');
            dropZone.classList.add('has-preview');
            dropContent.querySelector('p').innerHTML =
                `<i class="fa-solid fa-check" style="color:var(--green)"></i> ${file.name}`;
        };
        reader.readAsDataURL(file);
    }

    // Validation
    function validate() {
        let valid = true;
        const fields = [
            { id: 'username', errId: 'err-username', min: 3, max: 20, label: 'Username' },
            { id: 'name',     errId: 'err-name',     label: 'Full name' },
            { id: 'password', errId: 'err-password', min: 6, label: 'Password' },
        ];
        fields.forEach(f => {
            const el  = document.getElementById(f.id);
            const err = document.getElementById(f.errId);
            const val = el.value.trim();
            el.classList.remove('error');
            err.textContent = '';
            if (!val) {
                err.textContent = `${f.label} is required`;
                el.classList.add('error'); valid = false;
            } else if (f.min && val.length < f.min) {
                err.textContent = `${f.label} must be at least ${f.min} characters`;
                el.classList.add('error'); valid = false;
            } else if (f.max && val.length > f.max) {
                err.textContent = `${f.label} must be at most ${f.max} characters`;
                el.classList.add('error'); valid = false;
            }
        });
        return valid;
    }

    // Submit
    document.getElementById('createUserForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!validate()) return;

        const submitBtn  = document.getElementById('submitBtn');
        const submitText = document.getElementById('submitText');
        const submitIcon = document.getElementById('submitIcon');

        submitBtn.disabled = true;
        submitIcon.className = 'fa-solid fa-spinner fa-spin';
        submitText.textContent = ' Creating...';

        try {
            const formData = new FormData();
            const userRequestDTO = {
                username: document.getElementById('username').value.trim(),
                name:     document.getElementById('name').value.trim(),
                bio:      document.getElementById('bio').value.trim(),
                password: document.getElementById('password').value,
            };
            formData.append('userRequestDTO', new Blob([JSON.stringify(userRequestDTO)], { type: 'application/json' }));
            const imageFile = fileInput.files[0];
            if (imageFile) formData.append('images', imageFile);

            const res = await fetch(`${BASE_URL}/user/create`, { method: 'POST', body: formData });
            if (!res.ok) {
                const errText = await res.text();
                throw new Error(errText || `Server error ${res.status}`);
            }

            const data = await res.json();
            localStorage.setItem('lastCreatedUser', JSON.stringify(data));
            submitIcon.className = 'fa-solid fa-circle-check';
            submitText.textContent = ' User Created! Redirecting...';
            showToast('User created successfully!', 'success');
            setTimeout(() => { window.location.href = 'profile.html'; }, 1500);

        } catch (err) {
            showToast(err.message || 'Failed to create user', 'error');
            submitBtn.disabled = false;
            submitIcon.className = 'fa-solid fa-user-plus';
            submitText.textContent = ' Create User';
        }
    });
});
