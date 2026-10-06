/**
 * Страница «Воздух»: почасовые P1 / V1 по двум корпусам (СПГ742 и СПГ742_2).
 */
let eaChart = null;

function showMsg(html, kind) {
    const text = html || '';
    let el = document.getElementById('energyAirMsg');
    if (!text) {
        if (el) el.remove();
        return;
    }
    if (!el) {
        const container = document.querySelector('.main-content .container');
        const heading = container ? container.querySelector('h1') : null;
        if (!container || !heading) return;
        el = document.createElement('div');
        el.id = 'energyAirMsg';
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
    const df = document.getElementById('eaDateFrom');
    const dt = document.getElementById('eaDateTo');
    if (df) df.value = from;
    if (dt) dt.value = to;
}

async function setDateRangeFromData() {
    try {
        const range = await fetchJson('/api/energy/air/range');
        if (range && !range.empty && range.from && range.to) {
            const df = document.getElementById('eaDateFrom');
            const dt = document.getElementById('eaDateTo');
            if (df) df.value = range.from;
            if (dt) dt.value = range.to;
            return true;
        }
    } catch (e) {
        console.warn('air range', e);
    }
    return false;
}

function normalizeFactTime(v) {
    if (v == null) return '';
    if (typeof v === 'string') {
        // "2026-01-19T12:00:00" or "2026-01-19T12:00:00.000"
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
    /** @type {Map<string, {label: string, byTime: Map<string, {p: any, v: any}>}>} */
    const buildings = new Map();
    const times = new Set();

    for (const row of rows) {
        const t = normalizeFactTime(row.factTime);
        if (!t) continue;
        times.add(t);
        const code = row.buildingCode || '—';
        const label = row.buildingLabel || code;
        if (!buildings.has(code)) {
            buildings.set(code, { label, byTime: new Map() });
        }
        const b = buildings.get(code);
        if (!b.label && label) b.label = label;
        b.byTime.set(t, { p: row.pressureMpa, v: row.volumeM3 });
    }

    const sortedTimes = Array.from(times).sort();
    const buildingList = Array.from(buildings.entries()).map(([code, meta]) => ({
        code,
        label: meta.label,
        byTime: meta.byTime,
    }));
    buildingList.sort((a, b) => a.code.localeCompare(b.code, 'ru'));
    return { times: sortedTimes, buildings: buildingList };
}

function fmtNum(v) {
    if (v == null || v === '') return '';
    return String(v);
}

function renderTable(times, buildings) {
    const thead = document.getElementById('eaThead');
    const tbody = document.getElementById('eaTbody');
    if (!thead || !tbody) return;
    thead.innerHTML = '';
    tbody.innerHTML = '';

    const trh = document.createElement('tr');
    const th0 = document.createElement('th');
    th0.textContent = 'Время';
    trh.appendChild(th0);
    for (const b of buildings) {
        const thP = document.createElement('th');
        thP.textContent = `P1 ${b.label}, МПа`;
        trh.appendChild(thP);
        const thV = document.createElement('th');
        thV.textContent = `V1 ${b.label}, м³`;
        trh.appendChild(thV);
    }
    thead.appendChild(trh);

    for (const t of times) {
        const tr = document.createElement('tr');
        const td0 = document.createElement('td');
        td0.textContent = t;
        tr.appendChild(td0);
        for (const b of buildings) {
            const slot = b.byTime.get(t) || {};
            const tdP = document.createElement('td');
            tdP.textContent = fmtNum(slot.p);
            tr.appendChild(tdP);
            const tdV = document.createElement('td');
            tdV.textContent = fmtNum(slot.v);
            tr.appendChild(tdV);
        }
        tbody.appendChild(tr);
    }
}

const EA_COLORS = {
    p: ['#0d6efd', '#6610f2'],
    v: ['#198754', '#fd7e14'],
};

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

function renderChart(times, buildings) {
    const canvas = document.getElementById('eaChart');
    if (!canvas || typeof Chart === 'undefined') return;

    const labels = chartAxisLabels(times);
    const datasets = [];
    buildings.forEach((b, i) => {
        datasets.push({
            label: `P1 ${b.label}, МПа`,
            data: times.map((t) => {
                const v = (b.byTime.get(t) || {}).p;
                return v == null ? null : Number(v);
            }),
            borderColor: EA_COLORS.p[i % EA_COLORS.p.length],
            backgroundColor: 'transparent',
            yAxisID: 'yP',
            tension: 0.15,
            spanGaps: false,
            pointRadius: 2,
        });
        datasets.push({
            label: `V1 ${b.label}, м³`,
            data: times.map((t) => {
                const v = (b.byTime.get(t) || {}).v;
                return v == null ? null : Number(v);
            }),
            borderColor: EA_COLORS.v[i % EA_COLORS.v.length],
            backgroundColor: 'transparent',
            yAxisID: 'yV',
            tension: 0.15,
            spanGaps: false,
            pointRadius: 2,
        });
    });

    const ctx = canvas.getContext('2d');
    if (eaChart) eaChart.destroy();
    eaChart = new Chart(ctx, {
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
                title: { display: true, text: 'Воздух — P1 и V1 по корпусам' },
            },
            scales: {
                x: { title: { display: true, text: 'Время' } },
                yP: {
                    type: 'linear',
                    position: 'left',
                    title: { display: true, text: 'P1, МПа' },
                },
                yV: {
                    type: 'linear',
                    position: 'right',
                    title: { display: true, text: 'V1, м³' },
                    grid: { drawOnChartArea: false },
                },
            },
        },
    });
    requestAnimationFrame(() => {
        if (eaChart) eaChart.resize();
    });
}

function readDateRange() {
    const from = (document.getElementById('eaDateFrom') || {}).value || '';
    const to = (document.getElementById('eaDateTo') || {}).value || '';
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
        const rows = await fetchJson(`/api/energy/air/hourly-values?${q.toString()}`);
        if (!rows.length) {
            const hasData = await setDateRangeFromData();
            if (hasData) {
                const again = readDateRange();
                const q2 = new URLSearchParams({ from: again.from, to: again.to });
                const rows2 = await fetchJson(`/api/energy/air/hourly-values?${q2.toString()}`);
                if (rows2.length) {
                    const pivoted = pivotRows(rows2);
                    renderTable(pivoted.times, pivoted.buildings);
                    renderChart(pivoted.times, pivoted.buildings);
                    showMsg(`Показан весь доступный период: ${again.from} — ${again.to}.`, '');
                    return;
                }
            }
            showMsg('Нет данных за выбранный период. Импортируйте Excel с листами СПГ742 и СПГ742_2.', 'warn');
            renderTable([], []);
            if (eaChart) {
                eaChart.destroy();
                eaChart = null;
            }
            return;
        }
        const { times, buildings } = pivotRows(rows);
        renderTable(times, buildings);
        renderChart(times, buildings);
    } catch (e) {
        console.error(e);
        showMsg('Ошибка загрузки: ' + (e && e.message ? e.message : e), 'err');
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    setDefaultDateRange();
    await setDateRangeFromData();
    document.getElementById('eaApply').addEventListener('click', apply);
    requestAnimationFrame(() => apply());
});
