function eiShowMsg(html, kind) {
    const el = document.getElementById('energyImportMsg');
    if (!el) return;
    el.className = 'energy-msg' + (kind ? ' ' + kind : '');
    el.innerHTML = html || '';
}

function eiAirShowMsg(html, kind) {
    const el = document.getElementById('energyAirImportMsg');
    if (!el) return;
    el.className = 'energy-msg' + (kind ? ' ' + kind : '');
    el.innerHTML = html || '';
}

function eiSteamHourlyShowMsg(html, kind) {
    const el = document.getElementById('energySteamHourlyImportMsg');
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

async function eiImportAir(file) {
    const fd = new FormData();
    fd.append('file', file);
    const r = await fetch('/api/energy/air/import', { method: 'POST', body: fd });
    const text = await r.text();
    if (r.status === 413) {
        throw new Error('Файл слишком большой (HTTP 413).');
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

async function eiImportSteamHourly(file) {
    const fd = new FormData();
    fd.append('file', file);
    const r = await fetch('/api/energy/steam-hourly/import', { method: 'POST', body: fd });
    const text = await r.text();
    if (r.status === 413) {
        throw new Error('Файл слишком большой (HTTP 413).');
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

function eiFormatResult(result, title) {
    const parts = [
        `<strong>${title || 'Импорт ОГЭ завершён.'}</strong>`,
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

    const airFileInput = document.getElementById('eiAirFile');
    const airFileName = document.getElementById('eiAirFileName');
    const airSelectBtn = document.getElementById('eiAirSelectBtn');
    const airImportBtn = document.getElementById('eiAirImportBtn');
    let selectedAirFile = null;

    if (airSelectBtn && airFileInput && airImportBtn) {
        airSelectBtn.addEventListener('click', () => airFileInput.click());
        airFileInput.addEventListener('change', () => {
            selectedAirFile = airFileInput.files && airFileInput.files[0] ? airFileInput.files[0] : null;
            airFileName.textContent = selectedAirFile ? selectedAirFile.name : '';
        });
        airImportBtn.addEventListener('click', async () => {
            if (!selectedAirFile) {
                eiAirShowMsg('Сначала выберите файл .xlsx с листами СПГ742 / СПГ742_2.', 'warn');
                return;
            }
            airSelectBtn.disabled = true;
            airImportBtn.disabled = true;
            eiAirShowMsg('Идёт импорт воздуха…', '');
            try {
                const result = await eiImportAir(selectedAirFile);
                const written = result.valuesWritten ?? 0;
                if (written === 0) {
                    eiAirShowMsg(eiFormatResult(result, 'Импорт воздуха: записей не добавлено.'), 'warn');
                } else {
                    eiAirShowMsg(eiFormatResult(result, 'Импорт воздуха завершён.'), '');
                }
            } catch (e) {
                console.error(e);
                eiAirShowMsg(`Ошибка импорта воздуха: ${e && e.message ? e.message : e}`, 'err');
            } finally {
                airSelectBtn.disabled = false;
                airImportBtn.disabled = false;
            }
        });
    }

    const steamFileInput = document.getElementById('eiSteamHourlyFile');
    const steamFileName = document.getElementById('eiSteamHourlyFileName');
    const steamSelectBtn = document.getElementById('eiSteamHourlySelectBtn');
    const steamImportBtn = document.getElementById('eiSteamHourlyImportBtn');
    let selectedSteamFile = null;

    if (steamSelectBtn && steamFileInput && steamImportBtn) {
        steamSelectBtn.addEventListener('click', () => steamFileInput.click());
        steamFileInput.addEventListener('change', () => {
            selectedSteamFile = steamFileInput.files && steamFileInput.files[0] ? steamFileInput.files[0] : null;
            steamFileName.textContent = selectedSteamFile ? selectedSteamFile.name : '';
        });
        steamImportBtn.addEventListener('click', async () => {
            if (!selectedSteamFile) {
                eiSteamHourlyShowMsg('Сначала выберите файл .xlsx почасового архива пара.', 'warn');
                return;
            }
            steamSelectBtn.disabled = true;
            steamImportBtn.disabled = true;
            eiSteamHourlyShowMsg('Идёт импорт пара (почасовой)…', '');
            try {
                const result = await eiImportSteamHourly(selectedSteamFile);
                const written = result.valuesWritten ?? 0;
                if (written === 0) {
                    eiSteamHourlyShowMsg(eiFormatResult(result, 'Импорт пара: записей не добавлено.'), 'warn');
                } else {
                    eiSteamHourlyShowMsg(eiFormatResult(result, 'Импорт пара (почасовой) завершён.'), '');
                }
            } catch (e) {
                console.error(e);
                eiSteamHourlyShowMsg(`Ошибка импорта пара: ${e && e.message ? e.message : e}`, 'err');
            } finally {
                steamSelectBtn.disabled = false;
                steamImportBtn.disabled = false;
            }
        });
    }
});
