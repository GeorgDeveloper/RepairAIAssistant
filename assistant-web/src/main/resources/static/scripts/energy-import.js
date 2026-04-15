function eiShowMsg(html, kind) {
    const el = document.getElementById('energyImportMsg');
    if (!el) return;
    el.className = 'energy-msg' + (kind ? ' ' + kind : '');
    el.innerHTML = html || '';
}

function eiFillYearSelect() {
    const sel = document.getElementById('eiYear');
    if (!sel) return;
    const y = new Date().getFullYear();
    sel.innerHTML = '';
    for (let i = y - 2; i <= y + 3; i++) {
        const o = document.createElement('option');
        o.value = String(i);
        o.textContent = String(i);
        if (i === y) o.selected = true;
        sel.appendChild(o);
    }
}

function eiEscapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

async function eiImportSelected(file, year, resources) {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('year', String(year));
    if (Array.isArray(resources) && resources.length > 0 && resources.length < 6) {
        fd.append('resource', resources.join(','));
    }
    const r = await fetch('/api/energy/import', { method: 'POST', body: fd });
    const text = await r.text();
    if (r.status === 413) {
        throw new Error('Файл слишком большой (HTTP 413). Проверьте лимиты multipart и перезапустите assistant-web.');
    }
    let body = {};
    try {
        body = text ? JSON.parse(text) : {};
    } catch (e) {
        throw new Error(`Ответ сервера не JSON (HTTP ${r.status}): ${text.slice(0, 200)}`);
    }
    if (!r.ok) {
        throw new Error(body.error || body.message || `HTTP ${r.status}`);
    }
    return body;
}

function eiFormatResult(result) {
    const parts = [
        `<strong>Импорт ОГЭ завершён.</strong>`,
        `Строк обработано: <strong>${result.rowsScanned ?? '—'}</strong>,`,
        `принято с датой: <strong>${result.rowsAccepted ?? '—'}</strong>,`,
        `значений записано: <strong>${result.valuesWritten ?? '—'}</strong>.`
    ];
    if (Array.isArray(result.warnings) && result.warnings.length) {
        const warnings = result.warnings.map((w) => `<li>${eiEscapeHtml(String(w))}</li>`).join('');
        parts.push(`<br>Предупреждения:<ul>${warnings}</ul>`);
    }
    return parts.join(' ');
}

document.addEventListener('DOMContentLoaded', () => {
    eiFillYearSelect();

    const fileInput = document.getElementById('eiFile');
    const fileName = document.getElementById('eiFileName');
    const selectBtn = document.getElementById('eiSelectBtn');
    const importBtn = document.getElementById('eiImportBtn');
    const yearSel = document.getElementById('eiYear');

    let selectedFile = null;

    selectBtn.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', () => {
        selectedFile = fileInput.files && fileInput.files[0] ? fileInput.files[0] : null;
        fileName.textContent = selectedFile ? selectedFile.name : '';
    });

    importBtn.addEventListener('click', async () => {
        if (!selectedFile) {
            eiShowMsg('Сначала выберите файл .xlsx.', 'warn');
            return;
        }
        const year = parseInt(yearSel.value, 10);
        if (!year || year < 1990 || year > 2100) {
            eiShowMsg('Проверьте значение года.', 'warn');
            return;
        }

        selectBtn.disabled = true;
        importBtn.disabled = true;
        eiShowMsg('Идёт импорт всех показателей ОГЭ…', '');
        try {
            const selectedResources = Array.from(document.querySelectorAll('.ei-resource:checked'))
                .map((el) => el.value);
            if (selectedResources.length === 0) {
                eiShowMsg('Выберите хотя бы один раздел ОГЭ для импорта.', 'warn');
                return;
            }
            const result = await eiImportSelected(selectedFile, year, selectedResources);
            eiShowMsg(eiFormatResult(result), '');
        } catch (e) {
            console.error(e);
            eiShowMsg(`Ошибка импорта: ${e && e.message ? e.message : e}`, 'err');
        } finally {
            selectBtn.disabled = false;
            importBtn.disabled = false;
        }
    });
});
