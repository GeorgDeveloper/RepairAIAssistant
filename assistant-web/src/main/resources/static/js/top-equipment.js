document.addEventListener('DOMContentLoaded', async function() {
    const equipmentApi = '/top-equipment';
    const causesApi = '/top-causes';

    const legendEl = document.getElementById('legend');
    const loadingEl = document.getElementById('loading');
    const errorEl = document.getElementById('error');

    let chart = null;
    let drillChart = null;
    let drillLevel = 0; // 0 none, 1 machines, 2 mechanisms, 3 events
    let currentCause = null;
    let currentMachine = null;
    let selectedMachines = new Set();
    let equipmentsCache = [];

    await Promise.all([loadWeeks(), loadAreas(), loadFailureTypes()]);
    applyFilters();

    async function loadWeeks() {
        const data = await fetch(equipmentApi + '/weeks').then(r=>r.json());
        const sel = document.getElementById('week');
        sel.innerHTML = '<option value="all">Все</option>' + data.map(w=>`<option value="${w.week_number}">${w.week_number}</option>`).join('');
    }
    
    async function loadAreas() {
        const data = await fetch(equipmentApi + '/areas').then(r=>r.json());
        const sel = document.getElementById('area');
        sel.innerHTML = '<option value="all">Все</option>' + data.map(a=>`<option value="${a.area}">${a.area}</option>`).join('');
    }
    
    async function loadFailureTypes() {
        const data = await fetch(equipmentApi + '/failure-types').then(r=>r.json());
        const sel = document.getElementById('failureType');
        sel.innerHTML = '<option value="all">Все</option>' + data.map(a=>`<option value="${a.failure_type}">${a.failure_type}</option>`).join('');
    }

    function getCommonParams() {
        const dateFrom = document.getElementById('dateFrom').value || '';
        const dateTo = document.getElementById('dateTo').value || '';
        const week = document.getElementById('week').value || 'all';
        const area = document.getElementById('area').value || 'all';
        const failureType = document.getElementById('failureType').value || 'all';
        return { dateFrom, dateTo, week, area, failureType };
    }

    async function loadLegend() {
        const {dateFrom, dateTo, week, area, failureType} = getCommonParams();
        const params = new URLSearchParams({ dateFrom, dateTo, week, area, failureType, limit: 30 });
        const data = await fetch(equipmentApi + '/data?' + params.toString()).then(r=>r.json());
        equipmentsCache = data;
        if (selectedMachines.size === 0) {
            data.forEach(d => selectedMachines.add(d.machine_name));
        }
        renderLegend(data);
    }

    function renderLegend(items) {
        legendEl.innerHTML = '';
        items.forEach((it, idx) => {
            const color = Chart.helpers.color(Chart.defaults.color).alpha(0.5).rgbString();
            const row = document.createElement('div');
            row.className = 'legend-item' + (selectedMachines.has(it.machine_name) ? '' : ' disabled');
            row.innerHTML = `
                <div class="legend-left">
                    <span class="legend-color" style="background:#${(idx*47%255).toString(16).padStart(2,'0')}a3${(idx*91%255).toString(16).padStart(2,'0')}"></span>
                    <span>${it.machine_name}</span>
                </div>
                <div style="display:flex; gap:10px; font-variant-numeric: tabular-nums;">
                    <span>${(it.total_downtime_hours||0).toFixed(1)} ч</span>
                    <span>|</span>
                    <span>${it.failure_count||0}</span>
                </div>`;
            row.onclick = () => {
                if (selectedMachines.has(it.machine_name)) selectedMachines.delete(it.machine_name); else selectedMachines.add(it.machine_name);
                row.classList.toggle('disabled');
                applyFilters();
            };
            legendEl.appendChild(row);
        });
    }

    function getSelectedEquipment() {
        const arr = equipmentsCache.filter(e => selectedMachines.has(e.machine_name));
        arr.sort((a,b)=> Number(b.total_downtime_hours||0) - Number(a.total_downtime_hours||0));
        return arr.slice(0,30);
    }

    async function applyFilters() {
        showLoading();
        hideError();
        try {
            await loadLegend();
            const data = getSelectedEquipment();
            renderChart(data);
        } catch (e) {
            showError('Error loading data');
            console.error(e);
        } finally {
            hideLoading();
        }
    }

    function renderChart(data) {
        const ctx = document.getElementById('causeChart').getContext('2d');
        if (chart) chart.destroy();
        chart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: data.map(d=>d.machine_name),
                datasets: [
                    { label: 'Σ простоя, ч', data: data.map(d=>Number(d.total_downtime_hours||0)), backgroundColor: 'rgba(231,76,60,0.6)' },
                    { label: 'Кол-во вызовов', data: data.map(d=>Number(d.failure_count||0)), backgroundColor: 'rgba(52,152,219,0.6)' }
                ]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                onClick: (_evt, elems) => {
                    if (!elems?.length) return;
                    const idx = elems[0].index;
                    const machine = chart.data.labels[idx];
                    openDrilldownMachine(machine);
                },
                plugins: { legend: { display: true } },
                scales: { x: { beginAtZero: true } }
            }
        });
    }

    function openModal() { document.getElementById('drillModal').style.display = 'flex'; }
    function closeModal() { document.getElementById('drillModal').style.display = 'none'; drillLevel=0; currentCause=null; currentMachine=null; if (drillChart) {drillChart.destroy(); drillChart=null;} }

    async function openDrilldownMachine(machine) {
        currentMachine = machine;
        drillLevel = 1;
        document.getElementById('modalTitle').innerText = `Оборудование: ${machine}`;
        document.getElementById('backBtn').style.display = 'none';
        openModal();
        const {dateFrom, dateTo, week, area} = getCommonParams();
        const params = new URLSearchParams({ machine, dateFrom, dateTo, week, area });
        const data = await fetch(equipmentApi + '/drilldown/causes?' + params.toString()).then(r=>r.json());
        renderDrillChart('Причины', data, item => {
            currentCause = item.label;
            drillToMechanisms();
        });
    }

    async function drillToMechanisms() {
        drillLevel = 2;
        document.getElementById('backBtn').style.display = 'inline-block';
        const {dateFrom, dateTo, week, area} = getCommonParams();
        const params = new URLSearchParams({ machine: currentMachine, cause: currentCause, dateFrom, dateTo, week, area });
        const data = await fetch(equipmentApi + '/drilldown/mechanisms?' + params.toString()).then(r=>r.json());
        renderDrillChart('Узлы/механизмы', data, item => drillToEvents(item.label));
    }

    async function drillToEvents(mechanism) {
        drillLevel = 3;
        const {dateFrom, dateTo, week, area} = getCommonParams();
        const params = new URLSearchParams({ machine: currentMachine, cause: currentCause, mechanism, dateFrom, dateTo, week, area });
        const events = await fetch(equipmentApi + '/drilldown/events?' + params.toString()).then(r=>r.json());
        renderEventsTable(`События: ${mechanism}`, events);
        document.getElementById('backBtn').style.display = 'inline-block';
    }

    function renderDrillChart(title, rows, onBarClick) {
        const ctxEl = ensureDrillCanvas();
        if (drillChart) drillChart.destroy();
        const labels = rows.map(r => r.cause || r.mechanism_node || r.machine_name);
        const values = rows.map(r => Number(r.total_downtime_hours || 0));
        document.getElementById('modalTitle').innerText = title + ` — ${currentMachine || ''} ${currentCause? ' / ' + currentCause : ''}`;
        drillChart = new Chart(ctxEl, {
            type: 'bar',
            data: { labels, datasets: [{ label: 'Σ простоя, ч', data: values, backgroundColor: 'rgba(231,76,60,0.6)' }] },
            options: { indexAxis: 'y', maintainAspectRatio:false, onClick: (_e, els)=>{ if(els?.length){ const i=els[0].index; onBarClick({ label: labels[i], value: values[i] }); } }, plugins:{ legend:{ display:false }}, scales:{ x:{ beginAtZero:true }}}
        });
    }

    function ensureDrillCanvas() {
        const body = document.querySelector('.modal-body');
        let canvas = document.getElementById('drillChart');
        if (!canvas) {
            body.innerHTML = '<canvas id="drillChart" class="chart-canvas"></canvas>';
            canvas = document.getElementById('drillChart');
        }
        return canvas.getContext('2d');
    }

    function renderEventsTable(title, events) {
        const container = document.querySelector('.modal-body');
        if (drillChart) { drillChart.destroy(); drillChart = null; }
        container.innerHTML = `
            <div style="display:flex; flex-direction:column; height:100%;">
                <div style="font-weight:600; margin-bottom:8px; flex-shrink:0;">${title}</div>
                <div style="flex:1; overflow-y:auto; border:1px solid #eee; border-radius:6px; min-height:0;">
                    <table style="width:100%; border-collapse: separate; border-spacing:0; font-size: 14px;">
                        <thead style="position: sticky; top: 0; background: #fafafa; z-index: 1;">
                            <tr style="text-align:left; border-bottom:1px solid #eee;">
                                <th style="padding:8px; position:sticky; top:0; background:#fafafa;">Код события</th>
                                <th style="padding:8px; position:sticky; top:0; background:#fafafa;">Время простоя</th>
                                <th style="padding:8px; position:sticky; top:0; background:#fafafa;">Комментарии</th>
                                <th style="padding:8px; position:sticky; top:0; background:#fafafa;">Причина</th>
                                <th style="padding:8px; position:sticky; top:0; background:#fafafa;">Дата</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${events.map(e => `
                                <tr style="border-bottom:1px dashed #eee;">
                                    <td style="padding:8px; white-space:nowrap;">${e.code ?? ''}</td>
                                    <td style="padding:8px; white-space:nowrap;">${e.machine_downtime ?? ''}</td>
                                    <td style="padding:8px;">${(e.comments ?? '').toString()}</td>
                                    <td style="padding:8px; white-space:nowrap;">${e.cause ?? ''}</td>
                                    <td style="padding:8px; white-space:nowrap;">${e.start_bd_t1 ?? ''}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>`;
    }

    // Event listeners
    document.getElementById('closeBtn').addEventListener('click', closeModal);
    document.getElementById('backBtn').addEventListener('click', () => {
        if (drillLevel === 3) { drillToMechanisms(); }
        else if (drillLevel === 2) { openDrilldownMachine(currentMachine); }
        else closeModal();
    });
    document.getElementById('applyBtn').addEventListener('click', () => { selectedMachines.clear(); applyFilters(); });
});