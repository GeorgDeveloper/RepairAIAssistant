/**
 * Пар, почасовой архив — данные по потребителям (Qo/T/P/M).
 */
let eshChart = null;
let eshLastPivot = null;

function showMsg(html, kind) {
    const text = html || '';
    let el = document.getElementById('energySteamHourlyMsg');
    if (!text) {
        if (el) el.remove();
        return;
    }
    if (!el) {
        const container = document.querySelector('.main-content .container');
        const heading = container ? container.querySelector('h1') : null;
        if (!container || !heading) return;
        el = document.createElement('div');
        el.id = 'energySteamHourlyMsg';
        heading.insertAdjacentElement('afterend', el);
    }
    el.className = 'energy-msg' + (kind ? ' ' + kind : '');
    el.innerHTML = text;
}

function toIsoLocal(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}

function monthRangeForDate(ref) {
    const y = ref.getFullYear();
    const mo = ref.getMonth();
    return { from: toIsoLocal(new Date(y, mo, 1)), to: toIsoLocal(new Date(y, mo + 1, 0)) };
}

function setDefaultDateRange() {
    const { from, to } = monthRangeForDate(new Date());
    const df = document.getElementById('eshDateFrom');
    const dt = document.getElementById('eshDateTo');
    if (df) df.value = from;
    if (dt) dt.value = to;
}

async function setDateRangeFromData() {
    try {
        const range = await fetchJson('/api/energy/steam-hourly/range');
        if (range && !range.empty && range.from && range.to) {
            const df = document.getElementById('eshDateFrom');
            const dt = document.getElementById('eshDateTo');
            if (df) df.value = range.from;
            if (dt) dt.value = range.to;
            return true;
        }
    } catch (e) {
        console.warn('steam-hourly range', e);
    }
    return false;
}

function normalizeFactTime(v) {
    if (v == null) return '';
    if (typeof v === 'string') {
        return v.length >= 16 ? v.slice(0, 16).replace('T', ' ') : v;
    }
    if (Array.isArray(v) && v.length >= 5) {
        const y = v[0];
        const m = String(v[1]).padStart(2, '0');
        const d = String(v[2]).padStart(2, '0');
        const hh = String(v[3]).padStart(2, '0');
        const mm = String(v[4]).padStart(2, '0');
        return `${y}-${m}-${d} ${hh}:${mm}`;
    }
    return String(v);
}

async function fetchJson(url) {
    const r = await fetch(url);
    const text = await r.text();
    let body = null;
    try {
        body = text ? JSON.parse(text) : null;
    } catch (e) {
        if (!r.ok) throw new Error(`HTTP ${r.status}: ${text.slice(0, 200)}`);
        throw e;
    }
    if (!r.ok) {
        const msg = body && (body.error || body.message) ? (body.error || body.message) : `HTTP ${r.status}`;
        throw new Error(msg);
    }
    return body;
}

function pivotRows(rows) {
    const consumers = new Map();
    const times = new Set();
    for (const row of rows) {
        const t = normalizeFactTime(row.factTime);
        if (!t) continue;
        times.add(t);
        const code = row.consumerCode || '—';
        const label = row.consumerLabel || code;
        if (!consumers.has(code)) {
            consumers.set(code, { label, byTime: new Map() });
        }
        const c = consumers.get(code);
        if (label) c.label = label;
        c.byTime.set(t, {
            flow: row.flowValue,
            temp: row.tempC,
            pressure: row.pressureMpa,
            mass: row.massT,
        });
    }
    const sortedTimes = Array.from(times).sort();
    const consumerList = Array.from(consumers.entries()).map(([code, meta]) => ({
        code,
        label: meta.label,
        byTime: meta.byTime,
    }));
    consumerList.sort((a, b) => a.code.localeCompare(b.code, 'ru', { numeric: true }));
    return { times: sortedTimes, consumers: consumerList };
}

function fmtNum(v) {
    if (v == null || v === '') return '';
    return String(v);
}

function renderTable(times, consumers) {
    const thead = document.getElementById('eshThead');
    const tbody = document.getElementById('eshTbody');
    if (!thead || !tbody) return;
    thead.innerHTML = '';
    tbody.innerHTML = '';

    const trh = document.createElement('tr');
    const th0 = document.createElement('th');
    th0.textContent = 'Время';
    trh.appendChild(th0);
    for (const c of consumers) {
        for (const title of [`${c.label}: расход`, `${c.label}: T °C`, `${c.label}: P МПа`, `${c.label}: M т`]) {
            const th = document.createElement('th');
            th.textContent = title;
            trh.appendChild(th);
        }
    }
    thead.appendChild(trh);

    for (const t of times) {
        const tr = document.createElement('tr');
        const td0 = document.createElement('td');
        td0.textContent = t;
        tr.appendChild(td0);
        for (const c of consumers) {
            const slot = c.byTime.get(t) || {};
            for (const key of ['flow', 'temp', 'pressure', 'mass']) {
                const td = document.createElement('td');
                td.textContent = fmtNum(slot[key]);
                tr.appendChild(td);
            }
        }
        tbody.appendChild(tr);
    }
}

const ESH_COLORS = ['#0d6efd', '#198754', '#fd7e14', '#6f42c1', '#dc3545', '#20c997', '#6610f2', '#0dcaf0'];

function chartAxisLabels(times) {
    if (!times.length) return [];
    const firstDay = times[0].slice(0, 10);
    const lastDay = times[times.length - 1].slice(0, 10);
    const singleDay = firstDay === lastDay;
    return times.map((t) => {
        const dd = t.slice(8, 10);
        const mm = t.slice(5, 7);
        const hhmm = t.slice(11, 16);
        if (singleDay) return hhmm;
        return `${dd}.${mm} ${hhmm}`;
    });
}

function metricMeta(metric) {
    switch (metric) {
        case 'temp':
            return { key: 'temp', title: 'Температура, °C', axis: '°C' };
        case 'pressure':
            return { key: 'pressure', title: 'Давление, МПа', axis: 'МПа' };
        case 'mass':
            return { key: 'mass', title: 'Масса, т', axis: 'т' };
        default:
            return { key: 'flow', title: 'Расход', axis: 'значение' };
    }
}

function renderChart(times, consumers) {
    const canvas = document.getElementById('eshChart');
    if (!canvas || typeof Chart === 'undefined') return;
    const metric = (document.getElementById('eshMetric') || {}).value || 'flow';
    const meta = metricMeta(metric);
    const labels = chartAxisLabels(times);
    const datasets = consumers.map((c, i) => ({
        label: c.label,
        data: times.map((t) => {
            const v = (c.byTime.get(t) || {})[meta.key];
            return v == null ? null : Number(v);
        }),
        borderColor: ESH_COLORS[i % ESH_COLORS.length],
        backgroundColor: 'transparent',
        tension: 0.15,
        spanGaps: false,
        pointRadius: 1.5,
    }));

    const ctx = canvas.getContext('2d');
    if (eshChart) eshChart.destroy();
    eshChart = new Chart(ctx, {
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
                        padding: 10,
                    },
                },
                title: { display: true, text: `Пар — ${meta.title} по потребителям` },
            },
            scales: {
                x: { title: { display: true, text: 'Время' } },
                y: { title: { display: true, text: meta.axis } },
            },
        },
    });
    requestAnimationFrame(() => {
        if (eshChart) eshChart.resize();
    });
}

function readDateRange() {
    const from = (document.getElementById('eshDateFrom') || {}).value || '';
    const to = (document.getElementById('eshDateTo') || {}).value || '';
    return { from, to };
}

async function apply() {
    showMsg('', '');
    const { from, to } = readDateRange();
    if (!from || !to) {
        showMsg('Укажите дату с и дату по.', 'warn');
        return;
    }
    if (to < from) {
        showMsg('Дата «по» не может быть раньше даты «с».', 'warn');
        return;
    }
    try {
        const q = new URLSearchParams({ from, to });
        const rows = await fetchJson(`/api/energy/steam-hourly/hourly-values?${q.toString()}`);
        if (!rows.length) {
            const hasData = await setDateRangeFromData();
            if (hasData) {
                const again = readDateRange();
                const q2 = new URLSearchParams({ from: again.from, to: again.to });
                const rows2 = await fetchJson(`/api/energy/steam-hourly/hourly-values?${q2.toString()}`);
                if (rows2.length) {
                    eshLastPivot = pivotRows(rows2);
                    renderTable(eshLastPivot.times, eshLastPivot.consumers);
                    renderChart(eshLastPivot.times, eshLastPivot.consumers);
                    showMsg(`Показан весь доступный период: ${again.from} — ${again.to}.`, '');
                    return;
                }
            }
            showMsg('Нет данных за выбранный период. Импортируйте почасовой архив пара.', 'warn');
            renderTable([], []);
            eshLastPivot = null;
            if (eshChart) {
                eshChart.destroy();
                eshChart = null;
            }
            return;
        }
        eshLastPivot = pivotRows(rows);
        renderTable(eshLastPivot.times, eshLastPivot.consumers);
        renderChart(eshLastPivot.times, eshLastPivot.consumers);
    } catch (e) {
        console.error(e);
        showMsg('Ошибка загрузки: ' + (e && e.message ? e.message : e), 'err');
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    setDefaultDateRange();
    await setDateRangeFromData();
    document.getElementById('eshApply').addEventListener('click', apply);
    document.getElementById('eshMetric').addEventListener('change', () => {
        if (eshLastPivot) {
            renderChart(eshLastPivot.times, eshLastPivot.consumers);
        }
    });
    requestAnimationFrame(() => apply());
});
