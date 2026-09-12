const BASE_URL = 'http://localhost:8080';
const MAX_RETRIES = 3;

let state = {
    file: null,
    key: null,
    uploadId: null,
    completedParts: [],
    cancelled: false,
    uploading: false,
};

// ─── Toast ────────────────────────────────────────────────────────────────────
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    const icons = { success: 'fa-circle-check', error: 'fa-circle-xmark', info: 'fa-circle-info' };
    toast.className = `toast ${type}`;
    toast.innerHTML = `<i class="fa-solid ${icons[type]}"></i> ${message}`;
    toast.classList.remove('hidden');
    clearTimeout(window._toastTimer);
    window._toastTimer = setTimeout(() => toast.classList.add('hidden'), 4000);
}

// ─── Format bytes ─────────────────────────────────────────────────────────────
function formatBytes(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
}

// ─── DOM refs ─────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {

    const dropZone      = document.getElementById('mpDropZone');
    const fileInput     = document.getElementById('mp-file');
    const dropContent   = document.getElementById('mpDropContent');
    const fileInfoBar   = document.getElementById('fileInfoBar');
    const fileInfoName  = document.getElementById('fileInfoName');
    const fileInfoMeta  = document.getElementById('fileInfoMeta');
    const removeFileBtn = document.getElementById('removeFileBtn');
    const startBtn      = document.getElementById('startBtn');
    const cancelBtn     = document.getElementById('cancelBtn');

    // ── Drop zone ──────────────────────────────────────────────────────────────
    dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.classList.add('dragover'); });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
    dropZone.addEventListener('drop', e => {
        e.preventDefault(); dropZone.classList.remove('dragover');
        const file = e.dataTransfer.files[0];
        if (file) { const dt = new DataTransfer(); dt.items.add(file); fileInput.files = dt.files; setFile(file); }
    });
    fileInput.addEventListener('change', () => { if (fileInput.files[0]) setFile(fileInput.files[0]); });

    function setFile(file) {
        state.file = file;
        const partSize = parseInt(document.getElementById('mp-partSize').value);
        const parts = Math.ceil(file.size / partSize);
        fileInfoName.textContent = file.name;
        fileInfoMeta.textContent = `${formatBytes(file.size)} · ${parts} part(s) with ${formatBytes(partSize)} each`;
        fileInfoBar.classList.remove('hidden');
        dropContent.classList.add('hidden');
        dropZone.classList.add('has-preview');
    }

    removeFileBtn.addEventListener('click', () => {
        state.file = null;
        fileInput.value = '';
        fileInfoBar.classList.add('hidden');
        dropContent.classList.remove('hidden');
        dropZone.classList.remove('has-preview');
    });

    // update part count when part size changes
    document.getElementById('mp-partSize').addEventListener('change', () => {
        if (state.file) setFile(state.file);
    });

    // ── Start ──────────────────────────────────────────────────────────────────
    startBtn.addEventListener('click', startUpload);
    cancelBtn.addEventListener('click', cancelUpload);
    document.getElementById('uploadAnotherBtn').addEventListener('click', resetAll);
    document.getElementById('copyResultKey').addEventListener('click', () => {
        const key = document.getElementById('result-key').textContent;
        navigator.clipboard.writeText(key).then(() => {
            const btn = document.getElementById('copyResultKey');
            btn.innerHTML = '<i class="fa-solid fa-check"></i> Copied!';
            setTimeout(() => btn.innerHTML = '<i class="fa-solid fa-copy"></i> Copy', 2000);
        });
    });
});

// ─── Main upload flow ─────────────────────────────────────────────────────────
async function startUpload() {
    const userName = document.getElementById('mp-userName').value.trim();
    if (!userName)    { showToast('Username is required', 'error'); return; }
    if (!state.file)  { showToast('Please select a file', 'error'); return; }

    state.cancelled = false;
    state.uploading = true;
    state.completedParts = [];

    const partSize   = parseInt(document.getElementById('mp-partSize').value);
    const totalParts = Math.ceil(state.file.size / partSize);

    // Show progress panel, hide form actions
    document.getElementById('panel-progress').classList.remove('hidden');
    document.getElementById('startBtn').classList.add('hidden');
    document.getElementById('cancelBtn').classList.remove('hidden');
    document.getElementById('panel-form').querySelector('.drop-zone').style.pointerEvents = 'none';

    setOverallStatus('<i class="fa-solid fa-spinner fa-spin"></i> Initiating upload...');
    setProgress(0, totalParts);

    try {
        // ── Step 1: Initiate ───────────────────────────────────────────────────
        const initRes = await fetch(`${BASE_URL}/api/multipart/initiate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fileName: state.file.name, userName }),
        });
        if (!initRes.ok) throw new Error(`Initiate failed: ${initRes.status}`);
        const initData = await initRes.json();
        state.key      = initData.key;
        state.uploadId = initData.uploadId;

        if (state.cancelled) { await doAbort(); return; }

        // ── Step 2: Upload each part ───────────────────────────────────────────
        setOverallStatus('<i class="fa-solid fa-spinner fa-spin"></i> Uploading parts...');
        buildPartCards(totalParts);

        for (let i = 0; i < totalParts; i++) {
            if (state.cancelled) { await doAbort(); return; }

            const partNumber = i + 1;
            const start = i * partSize;
            const end   = Math.min(start + partSize, state.file.size);
            const chunk = state.file.slice(start, end);

            setPartStatus(partNumber, 'uploading', `Uploading... (${formatBytes(chunk.size)})`);

            let etag = null;
            let lastErr = null;

            // Retry loop
            for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                if (state.cancelled) { await doAbort(); return; }
                try {
                    // Get presigned URL
                    const urlRes = await fetch(
                        `${BASE_URL}/api/multipart/presign?key=${encodeURIComponent(state.key)}&uploadId=${encodeURIComponent(state.uploadId)}&partNumber=${partNumber}`
                    );
                    if (!urlRes.ok) throw new Error(`Presign failed (${urlRes.status})`);
                    const presignedUrl = await urlRes.text();

                    // PUT chunk to S3
                    const putRes = await fetch(presignedUrl, { method: 'PUT', body: chunk });
                    if (!putRes.ok) throw new Error(`S3 PUT failed (${putRes.status})`);

                    etag = putRes.headers.get('ETag') || `etag-part-${partNumber}`;
                    lastErr = null;
                    break; // success

                } catch (err) {
                    lastErr = err;
                    if (attempt < MAX_RETRIES) {
                        setPartStatus(partNumber, 'retrying', `Network error — Retrying (${attempt}/${MAX_RETRIES})...`);
                        await sleep(1000 * attempt); // backoff
                    }
                }
            }

            if (lastErr) {
                setPartStatus(partNumber, 'error', `Failed after ${MAX_RETRIES} retries`);
                throw new Error(`Part ${partNumber} failed: ${lastErr.message}`);
            }

            state.completedParts.push({ partNumber, eTag: etag });
            setPartStatus(partNumber, 'done', etag);
            setProgress(partNumber, totalParts);
        }

        if (state.cancelled) { await doAbort(); return; }

        // ── Step 3: Complete ───────────────────────────────────────────────────
        setOverallStatus('<i class="fa-solid fa-spinner fa-spin"></i> Combining all parts...');

        const completeRes = await fetch(`${BASE_URL}/api/multipart/complete`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                key: state.key,
                uploadId: state.uploadId,
                completedParts: state.completedParts,
            }),
        });
        if (!completeRes.ok) throw new Error(`Complete failed: ${completeRes.status}`);

        // ── Done ───────────────────────────────────────────────────────────────
        setOverallStatus('<i class="fa-solid fa-circle-check" style="color:var(--green)"></i> Upload complete!');
        setProgress(totalParts, totalParts);
        showResult(state.key);
        showToast('File uploaded successfully!', 'success');

    } catch (err) {
        if (!state.cancelled) {
            setOverallStatus(`<i class="fa-solid fa-circle-xmark" style="color:var(--red)"></i> Upload failed`);
            showToast(err.message || 'Upload failed', 'error');
            // Show start button again for retry
            document.getElementById('startBtn').classList.remove('hidden');
            document.getElementById('startBtn').innerHTML = '<i class="fa-solid fa-rotate-right"></i> Retry Upload';
        }
    } finally {
        state.uploading = false;
        document.getElementById('cancelBtn').classList.add('hidden');
        document.getElementById('panel-form').querySelector('.drop-zone').style.pointerEvents = '';
    }
}

// ─── Cancel ───────────────────────────────────────────────────────────────────
async function cancelUpload() {
    if (!state.uploading) return;
    state.cancelled = true;
    document.getElementById('cancelBtn').disabled = true;
    document.getElementById('cancelBtn').innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Cancelling...';
    showToast('Cancelling upload...', 'info');
}

async function doAbort() {
    setOverallStatus('<i class="fa-solid fa-ban" style="color:var(--red)"></i> Upload cancelled');
    try {
        await fetch(`${BASE_URL}/api/multipart/abort?key=${encodeURIComponent(state.key)}&uploadId=${encodeURIComponent(state.uploadId)}`, { method: 'DELETE' });
    } catch (_) {}
    showToast('Upload cancelled and aborted', 'info');
    document.getElementById('startBtn').classList.remove('hidden');
    document.getElementById('startBtn').innerHTML = '<i class="fa-solid fa-rocket"></i> Start Upload';
    document.getElementById('cancelBtn').disabled = false;
    document.getElementById('cancelBtn').innerHTML = '<i class="fa-solid fa-ban"></i> Cancel Upload';
    document.getElementById('cancelBtn').classList.add('hidden');
}

// ─── Reset ────────────────────────────────────────────────────────────────────
function resetAll() {
    state = { file: null, key: null, uploadId: null, completedParts: [], cancelled: false, uploading: false };
    document.getElementById('panel-progress').classList.add('hidden');
    document.getElementById('panel-result').classList.add('hidden');
    document.getElementById('parts-cards').innerHTML = '';
    document.getElementById('startBtn').classList.remove('hidden');
    document.getElementById('startBtn').innerHTML = '<i class="fa-solid fa-rocket"></i> Start Upload';
    document.getElementById('cancelBtn').classList.add('hidden');
    document.getElementById('mp-file').value = '';
    document.getElementById('fileInfoBar').classList.add('hidden');
    document.getElementById('mpDropContent').classList.remove('hidden');
    document.getElementById('mpDropZone').classList.remove('has-preview');
    document.getElementById('mp-userName').value = '';
    document.getElementById('progress-fill').style.width = '0%';
}

// ─── UI helpers ───────────────────────────────────────────────────────────────
function setOverallStatus(html) {
    document.getElementById('overallStatus').innerHTML = html;
}

function setProgress(done, total) {
    const pct = total === 0 ? 0 : Math.round((done / total) * 100);
    document.getElementById('progress-fill').style.width = `${pct}%`;
    document.getElementById('overallPct').textContent = `${pct}%`;
    document.getElementById('progressPartsText').textContent = `${done} / ${total} parts`;
    document.getElementById('progressSizeText').textContent = state.file
        ? `${formatBytes(Math.min(done * parseInt(document.getElementById('mp-partSize').value), state.file.size))} / ${formatBytes(state.file.size)}`
        : '';
}

function buildPartCards(total) {
    const container = document.getElementById('parts-cards');
    container.innerHTML = '';
    for (let i = 1; i <= total; i++) {
        const card = document.createElement('div');
        card.className = 'part-card';
        card.id = `part-card-${i}`;
        card.innerHTML = `
            <div class="part-card-left">
                <span class="part-num">Part ${i}</span>
                <span class="part-status-text" id="part-text-${i}">Waiting...</span>
            </div>
            <span class="part-badge badge-waiting" id="part-badge-${i}">
                <i class="fa-solid fa-clock"></i> Waiting
            </span>`;
        container.appendChild(card);
    }
}

function setPartStatus(partNumber, status, text) {
    const badge = document.getElementById(`part-badge-${partNumber}`);
    const textEl = document.getElementById(`part-text-${partNumber}`);
    const card  = document.getElementById(`part-card-${partNumber}`);
    if (!badge || !textEl) return;

    textEl.textContent = text;

    const map = {
        uploading: { cls: 'badge-pending',  icon: 'fa-spinner fa-spin', label: 'Uploading' },
        retrying:  { cls: 'badge-retrying', icon: 'fa-rotate-right',    label: 'Retrying'  },
        done:      { cls: 'badge-success',  icon: 'fa-check',           label: 'Done'      },
        error:     { cls: 'badge-error',    icon: 'fa-xmark',           label: 'Failed'    },
        waiting:   { cls: 'badge-waiting',  icon: 'fa-clock',           label: 'Waiting'   },
    };
    const m = map[status] || map.waiting;
    badge.className = `part-badge ${m.cls}`;
    badge.innerHTML = `<i class="fa-solid ${m.icon}"></i> ${m.label}`;
    if (status === 'uploading') card.classList.add('part-card-active');
    else card.classList.remove('part-card-active');
}

function showResult(key) {
    document.getElementById('panel-result').classList.remove('hidden');
    document.getElementById('result-key').textContent = key;
    document.getElementById('panel-result').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }
