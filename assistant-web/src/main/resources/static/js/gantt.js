document.addEventListener('DOMContentLoaded', function() {
    initializeDatePickers();
    initializeSelect2();
    setupAreaFilterListener();
    loadInitialData();
    
    document.getElementById('apply-filters').addEventListener('click', function() {
        applyFilters();
    });
});

function initializeDatePickers() {
    flatpickr.localize(flatpickr.l10ns.ru);
    
    const now = new Date();
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    
    flatpickr("#date-from", {
        enableTime: true,
        dateFormat: "Y-m-d H:i",
        defaultDate: yesterday.setHours(8, 0, 0, 0),
        time_24hr: true
    });
    
    flatpickr("#date-to", {
        enableTime: true,
        dateFormat: "Y-m-d H:i",
        defaultDate: now.setHours(8, 0, 0, 0),
        time_24hr: true
    });
}

function initializeSelect2() {
    $('#equipment, #failure-type, #status').select2({
        placeholder: "Выберите значения",
        allowClear: true,
        width: '100%'
    });
}

function setupAreaFilterListener() {
    document.getElementById('area').addEventListener('change', function() {
        loadEquipment(this.value);
    });
}

async function loadEquipment(area) {
    try {
        const response = await fetch(`/gantt/equipment?area=${area}`);
        const equipment = await response.json();
        
        const equipmentSelect = $('#equipment');
        equipmentSelect.empty();
        equipmentSelect.append(new Option('Всё оборудование', 'all'));
        
        equipment.forEach(item => {
            equipmentSelect.append(new Option(item.machine_name, item.machine_name));
        });
        
        equipmentSelect.trigger('change');
    } catch (error) {
        console.error('Ошибка загрузки оборудования:', error);
    }
}

async function loadInitialData() {
    showLoading(true);
    try {
        await Promise.all([
            loadEquipment('all'),
            loadFailureTypes(),
            loadStatuses()
        ]);
        
        const data = await fetchDataFromDatabase();
        generateGanttChart(data);
    } catch (error) {
        showError('Ошибка загрузки данных: ' + error.message);
        console.error('Error:', error);
    } finally {
        showLoading(false);
    }
}

async function loadFailureTypes() {
    try {
        const response = await fetch('/gantt/failure-types');
        const types = await response.json();
        
        const select = $('#failure-type');
        select.empty();
        select.append(new Option('Все типы', 'all'));
        
        types.forEach(item => {
            select.append(new Option(item.failure_type, item.failure_type));
        });
    } catch (error) {
        console.error('Ошибка загрузки типов поломок:', error);
    }
}

async function loadStatuses() {
    try {
        const response = await fetch('/gantt/statuses');
        const statuses = await response.json();
        
        const select = $('#status');
        select.empty();
        select.append(new Option('Все статусы', 'all'));
        
        statuses.forEach(item => {
            select.append(new Option(item.status, item.status));
        });
    } catch (error) {
        console.error('Ошибка загрузки статусов:', error);
    }
}

async function fetchDataFromDatabase(params = {}) {
    const url = new URL('/gantt/data', window.location.origin);
    Object.keys(params).forEach(key => {
        if (params[key] && params[key] !== 'all') {
            url.searchParams.append(key, params[key]);
        }
    });
    
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
}

async function applyFilters() {
    showLoading(true);
    try {
        const params = {
            dateFrom: document.getElementById('date-from').value,
            dateTo: document.getElementById('date-to').value,
            area: document.getElementById('area').value,
            equipment: $('#equipment').val()?.join(','),
            failureType: $('#failure-type').val()?.join(','),
            status: $('#status').val()?.join(',')
        };
        
        const data = await fetchDataFromDatabase(params);
        generateGanttChart(data);
    } catch (error) {
        showError('Ошибка применения фильтров: ' + error.message);
    } finally {
        showLoading(false);
    }
}

function generateGanttChart(data) {
    const dateFrom = document.getElementById('date-from').value;
    const dateTo = document.getElementById('date-to').value;
    
    const groupedData = groupDataByMachine(data);
    
    generateTimeHeader(dateFrom, dateTo);
    generateGanttBody(groupedData, dateFrom, dateTo);
    updateSummary(data);
}

function groupDataByMachine(data) {
    const grouped = {};
    data.forEach(item => {
        if (!grouped[item.machine_name]) {
            grouped[item.machine_name] = [];
        }
        grouped[item.machine_name].push(item);
    });
    return grouped;
}

function generateTimeHeader(dateFrom, dateTo) {
    const timeHeader = document.getElementById('time-header');
    timeHeader.innerHTML = '';
    
    const fromDate = new Date(dateFrom);
    const toDate = new Date(dateTo);
    const hoursDiff = Math.ceil((toDate - fromDate) / (1000 * 60 * 60));
    
    for (let i = 0; i < hoursDiff; i++) {
        const hourDate = new Date(fromDate);
        hourDate.setHours(fromDate.getHours() + i);
        
        const timeSlot = document.createElement('div');
        timeSlot.className = 'time-slot';
        timeSlot.textContent = hourDate.getHours().toString().padStart(2, '0') + ':00';
        timeHeader.appendChild(timeSlot);
    }
}

function generateGanttBody(groupedData, dateFrom, dateTo) {
    const ganttBody = document.getElementById('gantt-body');
    ganttBody.innerHTML = '';
    
    const fromDate = new Date(dateFrom);
    const toDate = new Date(dateTo);
    const totalHours = (toDate - fromDate) / (1000 * 60 * 60);
    const containerWidth = document.querySelector('.time-slots').offsetWidth;
    const hourWidth = containerWidth / totalHours;
    
    Object.keys(groupedData).sort().forEach(machine => {
        const machineRow = createMachineRow(machine);
        groupedData[machine].forEach(repair => {
            createRepairBar(repair, machineRow, fromDate, hourWidth);
        });
        ganttBody.appendChild(machineRow);
    });
}

function createMachineRow(machine) {
    const row = document.createElement('div');
    row.className = 'machine-row';
    
    const name = document.createElement('div');
    name.className = 'machine-name';
    name.textContent = machine;
    row.appendChild(name);
    
    return row;
}

function createRepairBar(repair, machineRow, fromDate, hourWidth) {
    const repairStart = new Date(repair.start_bd_t1);
    const repairEnd = new Date(repair.stop_bd_t4);
    
    const startOffset = (repairStart - fromDate) / (1000 * 60 * 60);
    const repairDuration = (repairEnd - repairStart) / (1000 * 60 * 60);
    
    const left = startOffset * hourWidth;
    const width = Math.max(repairDuration * hourWidth, 5);
    
    if (width > 0) {
        const bar = document.createElement('div');
        bar.className = 'repair-bar';
        bar.style.left = left + 'px';
        bar.style.width = width + 'px';
        bar.style.backgroundColor = getColorByFailureType(repair.failure_type);
        
        bar.setAttribute('data-machine', repair.machine_name);
        bar.setAttribute('data-start', repair.start_bd_t1);
        bar.setAttribute('data-end', repair.stop_bd_t4);
        bar.setAttribute('data-duration', repair.machine_downtime);
        bar.setAttribute('data-type', repair.failure_type);
        bar.setAttribute('data-reason', repair.comments);
        bar.setAttribute('data-status', repair.status);
        
        bar.addEventListener('mousemove', showTooltip);
        bar.addEventListener('mouseleave', hideTooltip);
        
        machineRow.appendChild(bar);
    }
}

function getColorByFailureType(type) {
    switch (type) {
        case 'Механика': return '#3498db';
        case 'Электроника':
        case 'Электрика': return '#e74c3c';
        default: return '#2ecc71';
    }
}

function showTooltip(e) {
    let tooltip = document.querySelector('.tooltip') || createTooltip();
    
    const machine = this.getAttribute('data-machine');
    const start = this.getAttribute('data-start');
    const end = this.getAttribute('data-end');
    const duration = this.getAttribute('data-duration');
    const type = this.getAttribute('data-type');
    const reason = this.getAttribute('data-reason');
    const status = this.getAttribute('data-status');
    
    tooltip.innerHTML = `
        <strong>${machine}</strong><br>
        Начало: ${formatDateTime(start)}<br>
        Окончание: ${formatDateTime(end)}<br>
        Длительность: ${duration}<br>
        Тип: ${type}<br>
        Статус: ${status}<br>
        Причина: ${reason}
    `;
    
    tooltip.style.display = 'block';
    tooltip.style.left = (e.pageX + 10) + 'px';
    tooltip.style.top = (e.pageY + 10) + 'px';
}

function createTooltip() {
    const tooltip = document.createElement('div');
    tooltip.className = 'tooltip';
    document.body.appendChild(tooltip);
    return tooltip;
}

function hideTooltip() {
    const tooltip = document.querySelector('.tooltip');
    if (tooltip) tooltip.style.display = 'none';
}

function formatDateTime(dateTimeStr) {
    const date = new Date(dateTimeStr);
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;
}

function updateSummary(data) {
    document.getElementById('total-repairs').textContent = data.length;
    
    let totalMinutes = 0;
    data.forEach(item => {
        if (item.machine_downtime) {
            const [hours, minutes] = item.machine_downtime.split(':');
            totalMinutes += parseInt(hours) * 60 + parseInt(minutes);
        }
    });
    
    const totalHours = Math.floor(totalMinutes / 60);
    const remainingMinutes = totalMinutes % 60;
    document.getElementById('total-downtime').textContent = 
        `${totalHours.toString().padStart(2, '0')}:${remainingMinutes.toString().padStart(2, '0')}:00`;
    
    const avgMinutes = data.length > 0 ? Math.round(totalMinutes / data.length) : 0;
    const avgHours = Math.floor(avgMinutes / 60);
    const avgRemainingMinutes = avgMinutes % 60;
    document.getElementById('avg-repair-time').textContent = 
        `${avgHours.toString().padStart(2, '0')}:${avgRemainingMinutes.toString().padStart(2, '0')}:00`;
    
    if (data.length > 0) {
        const failureCounts = {};
        data.forEach(item => {
            if (item.failure_type) {
                failureCounts[item.failure_type] = (failureCounts[item.failure_type] || 0) + 1;
            }
        });
        
        let mostCommon = '';
        let maxCount = 0;
        for (const [type, count] of Object.entries(failureCounts)) {
            if (count > maxCount) {
                mostCommon = type;
                maxCount = count;
            }
        }
        
        document.getElementById('most-common-failure').textContent = 
            `${mostCommon} (${maxCount} раз)`;
    } else {
        document.getElementById('most-common-failure').textContent = '-';
    }
}

function showLoading(show) {
    document.getElementById('loading').style.display = show ? 'block' : 'none';
}

function showError(message) {
    const errorDiv = document.getElementById('error');
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
    setTimeout(() => errorDiv.style.display = 'none', 5000);
}