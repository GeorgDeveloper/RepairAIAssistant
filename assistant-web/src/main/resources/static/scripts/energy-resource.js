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
    const text = html || '';
    let el = document.getElementById('energyResourceMsg');
    if (!text) {
        if (el) el.remove();
        return;
    }
    if (!el) {
        const container = document.querySelector('.main-content .container');
        const heading = container ? container.querySelector('h1') : null;
        if (!container || !heading) return;
        el = document.createElement('div');
        el.id = 'energyResourceMsg';
        heading.insertAdjacentElement('afterend', el);
    }
    el.className = 'energy-msg' + (kind ? ' ' + kind : '');
    el.innerHTML = text;
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
    const factMetricIds = metricsDefs
        .filter((m) => {
            const row2 = (m.row2_kind || '').toLowerCase();
            const label = (m.label_ru || '').toLowerCase();
            return row2.includes('факт') || label.includes('факт');
        })
        .map((m) => m.id);
    const hasFactMetrics = factMetricIds.length > 0;
    const isFilledFactValue = (v) => {
        if (v == null) return false;
        if (typeof v === 'number') return v !== 0;
        if (typeof v === 'string') {
            const t = v.trim();
            if (!t) return false;
            const n = Number(t.replace(',', '.'));
            return Number.isNaN(n) ? true : n !== 0;
        }
        return true;
    };
    const steamVisibilityMetricId = 'steam9_gcals_per_tonne';
    const hasSteamVisibilityMetric = metricsDefs.some((m) => m.id === steamVisibilityMetricId);
    const visibleDates = dates.filter((d) => {
        const row = byDate.get(d) || {};
        if (ER_RESOURCE === 'STEAM' && hasSteamVisibilityMetric) {
            return isFilledFactValue(row[steamVisibilityMetricId]);
        }
        if (!hasFactMetrics) return true;
        return factMetricIds.some((metricId) => isFilledFactValue(row[metricId]));
    });
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

    for (const d of visibleDates) {
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
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        usePointStyle: true,
                        pointStyle: 'line',
                        boxWidth: 40,
                        boxHeight: 3,
                        padding: 12,
                    },
                },
                title: { display: true, text: `${ER_TITLE} — ключевые показатели` },
            },
            scales: {
                x: { title: { display: true, text: 'Сутки' } },
                y: { title: { display: true, text: 'Значение' } },
            },
        },
    });
    // Ensure crisp first render after layout settles.
    requestAnimationFrame(() => {
        if (erChart) erChart.resize();
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
            showMsg(`Нет данных за выбранный месяц для ресурса ${ER_TITLE}.`, 'warn');
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

    requestAnimationFrame(() => apply());
});
