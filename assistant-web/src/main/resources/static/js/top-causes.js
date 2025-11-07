document.addEventListener('DOMContentLoaded', async function() {
    const apiBase = '/top-causes';
    let chart;
    let drillChart;
    let drillLevel = 'machines';
    let cachedMachines = null;
    let cachedMechanisms = null;
    let currentCause = '';
    let currentMachine = '';
    const hiddenCauses = new Set();
    let baseChartData = { labels: [], downtime: [], counts: [], colors: [] };

    // Функция для получения контекста canvas с правильной настройкой для высоких DPI
    function getChartContext(canvasId) {
        const canvas = document.getElementById(canvasId);
        // Chart.js сам управляет размерами canvas, но нужно убедиться, что он правильно обрабатывает devicePixelRatio
        return canvas.getContext('2d');
    }

    await Promise.all([loadAreas(), loadFailureTypes()]);
    await loadMachines();
    await applyFilters();
    document.getElementById('area').addEventListener('change', loadMachines);

    async function loadAreas() {
        const res = await fetch(`${apiBase}/areas`);
        const data = await res.json();
        const el = document.getElementById('area');
        el.innerHTML = '<option value="all">Все</option>';
        data.forEach(a => el.innerHTML += `<option value="${a.area}">${a.area}</option>`);
    }

    async function loadMachines() {
        const area = document.getElementById('area').value || 'all';
        const res = await fetch(`${apiBase}/machines?area=${encodeURIComponent(area)}`);
        const data = await res.json();
        const el = document.getElementById('machine');
        el.innerHTML = '<option value="all">Всё оборудование</option>';
        data.forEach(m => el.innerHTML += `<option value="${m.machine_name}">${m.machine_name}</option>`);
    }

    async function loadFailureTypes() {
        const res = await fetch(`${apiBase}/failure-types`);
        const data = await res.json();
        const el = document.getElementById('failureType');
        el.innerHTML = '<option value="all">Все</option>';
        data.forEach(t => el.innerHTML += `<option value="${t.failure_type}">${t.failure_type}</option>`);
    }

    async function applyFilters() {
        const params = new URLSearchParams();
        const df = document.getElementById('dateFrom').value;
        const dt = document.getElementById('dateTo').value;
        const a = document.getElementById('area').value;
        const m = document.getElementById('machine').value;
        const ft = document.getElementById('failureType').value;
        if (df) params.append('dateFrom', df);
        if (dt) params.append('dateTo', dt);
        if (a && a !== 'all') params.append('area', a);
        if (m && m !== 'all') params.append('machine', m);
        if (ft && ft !== 'all') params.append('failureType', ft);
        params.append('limit', 30);

        const res = await fetch(`${apiBase}/data?${params.toString()}`);
        const data = await res.json();
        renderChart(data);
        renderLegend(data);
    }

    function renderChart(data) {
        baseChartData = {
            labels: data.map(d => d.cause),
            downtime: data.map(d => Number(d.total_downtime_hours).toFixed(2)),
            counts: data.map(d => d.failure_count),
            colors: data.map((_, i) => `hsl(${(i*37)%360} 70% 55%)`)
        };
        updateMainChart();
    }

    function getFilteredChart() {
        const labels = [];
        const downtime = [];
        const counts = [];
        const colors = [];
        baseChartData.labels.forEach((label, i) => {
            if (!hiddenCauses.has(label)) {
                labels.push(label);
                downtime.push(baseChartData.downtime[i]);
                counts.push(baseChartData.counts[i]);
                colors.push(baseChartData.colors[i]);
            }
        });
        return { labels, downtime, counts, colors };
    }

    function updateMainChart() {
        const filtered = getFilteredChart();
        const ctx = getChartContext('topChart');
        if (chart) chart.destroy();
        chart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: filtered.labels,
                datasets: [
                    { label: 'Время простоя (ч)', data: filtered.downtime, backgroundColor: filtered.colors },
                    { label: 'Кол-во заявок', data: filtered.counts, backgroundColor: 'rgba(52, 152, 219, 0.45)' }
                ]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                devicePixelRatio: window.devicePixelRatio || 1,
                plugins: { 
                    legend: { 
                        display: true,
                        labels: {
                            font: {
                                size: 12,
                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                            }
                        }
                    } 
                },
                scales: { 
                    x: { 
                        beginAtZero: true,
                        ticks: {
                            font: {
                                size: 11,
                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                            }
                        }
                    },
                    y: {
                        ticks: {
                            font: {
                                size: 11,
                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                            }
                        }
                    }
                }
            }
        });

        document.getElementById('topChart').onclick = async (evt) => {
            const points = chart.getElementsAtEventForMode(evt, 'nearest', { intersect: true }, true);
            if (!points.length) return;
            const idx = points[0].index;
            const cause = filtered.labels[idx];
            await openDrilldown(cause);
        };
    }

    function renderLegend(data) {
        const container = document.getElementById('legend');
        container.innerHTML = '';
        data.forEach((d, i) => {
            const color = baseChartData.colors[i] || `hsl(${(i*37)%360} 70% 55%)`;
            const row = document.createElement('div');
            row.className = 'legend-item' + (hiddenCauses.has(d.cause) ? ' disabled' : '');
            row.style.cursor = 'pointer';
            row.onclick = () => toggleLegendItem(d.cause);
            row.innerHTML = `<div class="legend-left"><span class="legend-color" style="background:${color}"></span><span>${d.cause}</span></div><div>${Number(d.total_downtime_hours).toFixed(2)} ч | ${d.failure_count}</div>`;
            container.appendChild(row);
        });
        updateToggleButton();
    }
    
    function updateToggleButton() {
        const button = document.getElementById('toggleAllLegend');
        const allHidden = hiddenCauses.size === baseChartData.labels.length;
        button.textContent = allHidden ? 'Включить все' : 'Отключить все';
    }

    function toggleLegendItem(cause){
        if (hiddenCauses.has(cause)) hiddenCauses.delete(cause); else hiddenCauses.add(cause);
        updateMainChart();
        const data = baseChartData.labels.map((label, i) => ({
            cause: label,
            total_downtime_hours: baseChartData.downtime[i],
            failure_count: baseChartData.counts[i]
        }));
        renderLegend(data);
    }

    function openModal(title){
        document.getElementById('drillTitle').textContent = title;
        document.getElementById('drillModal').style.display = 'flex';
    }
    
    function closeModal(){
        document.getElementById('drillModal').style.display = 'none';
        if (drillChart) { drillChart.destroy(); drillChart = null; }
        drillLevel = 'machines';
        cachedMachines = null;
        cachedMechanisms = null;
        currentCause = '';
        currentMachine = '';
    }

    function backModal(){
        const body = document.querySelector('#drillModal .modal-body');
        if (drillLevel === 'events' && cachedMechanisms){
            body.innerHTML = '<canvas id="drillChart"></canvas>';
            renderDrillChart(`Узел: ${currentMachine}`, cachedMechanisms.labels, cachedMechanisms.downtime, cachedMechanisms.counts, cachedMechanisms.onBarClick);
            drillLevel = 'mechanisms';
            document.getElementById('backBtn').style.display = 'inline-block';
            return;
        }
        if (drillLevel === 'mechanisms' && cachedMachines){
            body.innerHTML = '<canvas id="drillChart"></canvas>';
            renderDrillChart('По оборудованию', cachedMachines.labels, cachedMachines.downtime, cachedMachines.counts, cachedMachines.onBarClick);
            drillLevel = 'machines';
            document.getElementById('backBtn').style.display = 'none';
            return;
        }
        closeModal();
    }

    async function openDrilldown(cause){
        openModal(`Причина: ${cause}`);
        currentCause = cause;
        const params = new URLSearchParams();
        const df = document.getElementById('dateFrom').value;
        const dt = document.getElementById('dateTo').value;
        const a = document.getElementById('area').value;
        if (df) params.append('dateFrom', df);
        if (dt) params.append('dateTo', dt);
        if (a && a !== 'all') params.append('area', a);
        params.append('cause', cause);

        const machines = await (await fetch(`${apiBase}/drilldown/machines?${params}`)).json();
        const labels = machines.map(m=>m.machine_name);
        const downtime = machines.map(m=>Number(m.total_downtime_hours).toFixed(2));
        const counts = machines.map(m=>m.failure_count);
        const onBarClick = async (machineName)=>{
            const p2 = new URLSearchParams(params);
            p2.append('machine', machineName);
            const nodes = await (await fetch(`${apiBase}/drilldown/mechanisms?${p2}`)).json();
            const mechLabels = nodes.map(n=>n.mechanism_node);
            const mechDowntime = nodes.map(n=>Number(n.total_downtime_hours).toFixed(2));
            const mechCounts = nodes.map(n=>n.failure_count);
            const mechOnClick = async (mechanismName)=>{
                const p3 = new URLSearchParams(p2);
                p3.append('mechanism', mechanismName);
                const events = await (await fetch(`${apiBase}/drilldown/events?${p3}`)).json();
                renderEventsTable(mechanismName, events);
                drillLevel = 'events';
                document.getElementById('backBtn').style.display = 'inline-block';
            };
            renderDrillChart(`Узел: ${machineName}`, mechLabels, mechDowntime, mechCounts, mechOnClick);
            cachedMechanisms = { labels: mechLabels, downtime: mechDowntime, counts: mechCounts, onBarClick: mechOnClick };
            currentMachine = machineName;
            drillLevel = 'mechanisms';
            document.getElementById('backBtn').style.display = 'inline-block';
        };
        cachedMachines = { labels, downtime, counts, onBarClick };
        renderDrillChart('По оборудованию', labels, downtime, counts, onBarClick);
        drillLevel = 'machines';
        document.getElementById('backBtn').style.display = 'none';
    }

    function renderDrillChart(title, labels, downtime, counts, onBarClick){
        const ctx = getChartContext('drillChart');
        if (drillChart) drillChart.destroy();
        drillChart = new Chart(ctx, {
            type: 'bar',
            data: { labels, datasets: [
                { label: 'Время простоя (ч)', data: downtime, backgroundColor: 'rgba(231, 76, 60, .8)' },
                { label: 'Кол-во заявок', data: counts, backgroundColor: 'rgba(52, 152, 219, 0.45)' }
            ]},
            options: { 
                indexAxis: 'y', 
                responsive: true, 
                maintainAspectRatio: false,
                devicePixelRatio: window.devicePixelRatio || 1,
                plugins: {
                    legend: {
                        labels: {
                            font: {
                                size: 12,
                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        ticks: {
                            font: {
                                size: 11,
                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                            }
                        }
                    },
                    y: {
                        ticks: {
                            font: {
                                size: 11,
                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                            }
                        }
                    }
                }
            }
        });
        if (onBarClick){
            document.getElementById('drillChart').onclick = (evt)=>{
                const points = drillChart.getElementsAtEventForMode(evt, 'nearest', { intersect: true }, true);
                if (!points.length) return;
                const idx = points[0].index;
                onBarClick(labels[idx]);
            };
        } else {
            document.getElementById('drillChart').onclick = null;
        }
    }

    function renderEventsTable(mechanismName, events){
        const container = document.querySelector('#drillChart').parentElement;
        if (drillChart) { drillChart.destroy(); drillChart = null; }
        container.innerHTML = `
            <div style="display:flex; flex-direction:column; height:100%;">
                <div style="font-weight:600; margin-bottom:8px; flex-shrink:0;">События: ${mechanismName}</div>
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
            </div>
        `;
    }

    function toggleAllLegend() {
        const button = document.getElementById('toggleAllLegend');
        const allHidden = hiddenCauses.size === baseChartData.labels.length;
        
        if (allHidden) {
            hiddenCauses.clear();
            button.textContent = 'Отключить все';
        } else {
            baseChartData.labels.forEach(label => hiddenCauses.add(label));
            button.textContent = 'Включить все';
        }
        
        updateMainChart();
        const data = baseChartData.labels.map((label, i) => ({
            cause: label,
            total_downtime_hours: baseChartData.downtime[i],
            failure_count: baseChartData.counts[i]
        }));
        renderLegend(data);
    }

    // Event listeners
    document.getElementById('applyBtn').addEventListener('click', applyFilters);
    document.getElementById('closeBtn').addEventListener('click', closeModal);
    document.getElementById('backBtn').addEventListener('click', backModal);
    document.getElementById('toggleAllLegend').addEventListener('click', toggleAllLegend);
});