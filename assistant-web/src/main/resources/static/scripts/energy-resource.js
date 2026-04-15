/**
 * Универсальная страница по виду энергии.
 * Ресурс и заголовок берутся из data-атрибутов body:
 * - data-energy-resource: WATER/GAS/ELECTRICITY/NITROGEN/STEAM/TOTAL
 * - data-energy-title: локализованный заголовок
 */
const ER_RESOURCE = (document.body.dataset.energyResource || 'WATER').toUpperCase();
const ER_TITLE = document.body.dataset.energyTitle || ER_RESOURCE;

let erChart = null;
let erColumnMap = null;

function showMsg(html, kind) {
    const el = document.getElementById('energyResourceMsg');
    if (!el) return;
    el.className = 'energy-msg' + (kind ? ' ' + kind : '');
    el.innerHTML = html || '';
}

function normalizeFactDate(d) {
    if (d == null) return '';
    if (typeof d === 'string') return d.slice(0, 10);
    if (Array.isArray(d) && d.length >= 3) {
        const y = d[0];
        const m = String(d[1]).padStart(2, '0');
        const day = String(d[2]).padStart(2, '0');
        return `${y}-${m}-${day}`;
    }
    return String(d).slice(0, 10);
}

function toIsoLocal(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}

function monthBounds(year, month) {
    const from = new Date(year, month - 1, 1);
    const to = new Date(year, month, 0);
    return { from: toIsoLocal(from), to: toIsoLocal(to) };
}

function fillYearSelect() {
    const sel = document.getElementById('erYear');
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

function getMetrics(mapRoot) {
    const map = mapRoot && mapRoot.map ? mapRoot.map : mapRoot;
    if (!map || !Array.isArray(map.metrics)) return [];
    return map.metrics;
}

async function fetchJson(url) {
    const r = await fetch(url);
    if (!r.ok) throw new Error(`HTTP ${r.status} ${url}`);
    return r.json();
}

async function loadColumnMap() {
    return fetchJson(`/api/energy/column-map/${ER_RESOURCE}`);
}

async function loadDaily(year, month) {
    const { from, to } = monthBounds(year, month);
    const q = new URLSearchParams({ resource: ER_RESOURCE, from, to });
    return fetchJson(`/api/energy/daily-values?${q.toString()}`);
}

async function importExcel(file) {
    const year = parseInt(document.getElementById('erYear').value, 10);
    const fd = new FormData();
    fd.append('file', file);
    fd.append('year', String(year));
    fd.append('resource', ER_RESOURCE);
    const r = await fetch('/api/energy/import', { method: 'POST', body: fd });
    const text = await r.text();
    if (r.status === 413) {
        throw new Error(
            'Файл слишком большой для сервера (HTTP 413). Перезапустите assistant-web после изменения лимитов multipart.'
        );
    }
    let body;
    try {
        body = text ? JSON.parse(text) : {};
    } catch (e) {
        throw new Error(`Ответ сервера не JSON (HTTP ${r.status}): ${text.slice(0, 200)}`);
    }
    if (!r.ok) {
        const err =
            body.error ||
            body.message ||
            (text && text.length && text.length < 400 ? text : null) ||
            `HTTP ${r.status}`;
        throw new Error(typeof err === 'string' ? err : JSON.stringify(err));
    }
    return body;
}

function formatImportResult(body) {
    const parts = [
        `<strong>Импорт ${ER_TITLE} завершён.</strong> Строк обработано: ${body.rowsScanned ?? '—'}, принято с датой: ${body.rowsAccepted ?? '—'}, значений записано: ${body.valuesWritten ?? '—'}.`,
    ];
    if (Array.isArray(body.warnings) && body.warnings.length) {
        parts.push('<br>Предупреждения:<ul>' + body.warnings.map((w) => `<li>${escapeHtml(String(w))}</li>`).join('') + '</ul>');
    }
    return parts.join('');
}

function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function pivotByDate(rows) {
    const byDate = new Map();
    for (const row of rows) {
        const d = normalizeFactDate(row.factDate);
        if (!d) continue;
        if (!byDate.has(d)) byDate.set(d, {});
        const slot = byDate.get(d);
        slot[row.metricId] = row.valueNumeric;
    }
    const dates = Array.from(byDate.keys()).sort();
    return { byDate, dates };
}

function renderTable(metricsDefs, byDate, dates) {
    const thead = document.getElementById('erThead');
    const tbody = document.getElementById('erTbody');
    if (!thead || !tbody) return;
    thead.innerHTML = '';
    tbody.innerHTML = '';
    const trh = document.createElement('tr');
    const th0 = document.createElement('th');
    th0.textContent = 'Дата';
    trh.appendChild(th0);
    for (const m of metricsDefs) {
        const th = document.createElement('th');
        th.textContent = m.label_ru || m.id;
        trh.appendChild(th);
    }
    thead.appendChild(trh);

    for (const d of dates) {
        const tr = document.createElement('tr');
        const td0 = document.createElement('td');
        td0.textContent = d;
        tr.appendChild(td0);
        const row = byDate.get(d) || {};
        for (const m of metricsDefs) {
            const td = document.createElement('td');
            const v = row[m.id];
            td.textContent = v == null ? '' : String(v);
            tr.appendChild(td);
        }
        tbody.appendChild(tr);
    }
}

const ER_COLORS = ['#0d6efd', '#198754', '#fd7e14', '#6f42c1', '#dc3545', '#20c997', '#6610f2'];

function renderChart(metricsDefs, byDate, dates) {
    const chartMetrics = metricsDefs.filter((m) => m.chart_default === true);
    const canvas = document.getElementById('erChart');
    if (!canvas || typeof Chart === 'undefined') return;

    const labels = dates.map((d) => d.slice(8, 10) + '.' + d.slice(5, 7));
    const datasets = chartMetrics.map((m, i) => ({
        label: m.label_ru || m.id,
        data: dates.map((d) => {
            const v = (byDate.get(d) || {})[m.id];
            return v == null ? null : Number(v);
        }),
        borderColor: ER_COLORS[i % ER_COLORS.length],
        backgroundColor: 'transparent',
        tension: 0.15,
        spanGaps: false,
        pointRadius: 2,
    }));

    const ctx = canvas.getContext('2d');
    if (erChart) {
        erChart.destroy();
    }
    erChart = new Chart(ctx, {
        type: 'line',
        data: { labels, datasets },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { position: 'bottom' },
                title: { display: true, text: `${ER_TITLE} — ключевые показатели` },
            },
            scales: {
                x: { title: { display: true, text: 'Сутки' } },
                y: { title: { display: true, text: 'Значение' } },
            },
        },
    });
}

async function apply() {
    showMsg('', '');
    const year = parseInt(document.getElementById('erYear').value, 10);
    const month = parseInt(document.getElementById('erMonth').value, 10);
    try {
        if (!erColumnMap) {
            erColumnMap = await loadColumnMap();
        }
        const metricsDefs = getMetrics(erColumnMap);
        if (!metricsDefs.length) {
            showMsg('Не удалось прочитать карту колонок (metrics пусто).', 'warn');
            return;
        }
        const rows = await loadDaily(year, month);
        if (!rows.length) {
            showMsg(`Нет данных за выбранный месяц для ресурса ${ER_TITLE}. Загрузите файл .xlsx кнопкой «Импорт из Excel».`, 'warn');
            renderTable(metricsDefs, new Map(), []);
            if (erChart) {
                erChart.destroy();
                erChart = null;
            }
            return;
        }
        const { byDate, dates } = pivotByDate(rows);
        renderTable(metricsDefs, byDate, dates);
        renderChart(metricsDefs, byDate, dates);
    } catch (e) {
        console.error(e);
        showMsg('Ошибка загрузки: ' + (e && e.message ? e.message : e), 'err');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    fillYearSelect();
    const m = new Date().getMonth() + 1;
    const ms = document.getElementById('erMonth');
    if (ms) ms.value = String(m);
    document.getElementById('erApply').addEventListener('click', apply);

    const fileInput = document.getElementById('erFile');
    const importBtn = document.getElementById('erImportBtn');
    const fileNameEl = document.getElementById('erFileName');

    importBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', async () => {
        const file = fileInput.files && fileInput.files[0];
        fileInput.value = '';
        if (!file) return;
        if (fileNameEl) fileNameEl.textContent = file.name;
        importBtn.disabled = true;
        showMsg('Идёт загрузка и импорт…', '');
        try {
            const result = await importExcel(file);
            showMsg(formatImportResult(result), '');
            await apply();
        } catch (e) {
            console.error(e);
            showMsg('Ошибка импорта: ' + (e && e.message ? e.message : e), 'err');
        } finally {
            importBtn.disabled = false;
        }
    });

    apply();
});
