let zoomLevel = 1;
let currentData = [];

document.addEventListener('DOMContentLoaded', function() {
    initializeDatePickers();
    initializeSelect2();
    setupAreaFilterListener();
    setupZoomControls();
    loadInitialData();
    
    document.getElementById('apply-filters').addEventListener('click', function() {
        applyFilters();
    });
});

function setupZoomControls() {
    document.getElementById('zoom-in').addEventListener('click', function() {
        if (zoomLevel < 3) {
            zoomLevel += 0.25;
            updateZoom();
        }
    });
    
    document.getElementById('zoom-out').addEventListener('click', function() {
        if (zoomLevel > 0.25) {
            zoomLevel -= 0.25;
            updateZoom();
        }
    });
}

function updateZoom() {
    document.getElementById('zoom-level').textContent = Math.round(zoomLevel * 100) + '%';
    if (currentData.length > 0) {
        generateGanttChart(currentData);
    }
}

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
    
    // Обработка выбора "Всё" для множественных селектов
    $('#equipment, #failure-type, #status').on('select2:select', function(e) {
        const selectedValue = e.params.data.id;
        if (selectedValue === 'all') {
            $(this).val(['all']).trigger('change');
        } else {
            const currentValues = $(this).val() || [];
            if (currentValues.includes('all')) {
                const newValues = currentValues.filter(val => val !== 'all');
                newValues.push(selectedValue);
                $(this).val(newValues).trigger('change');
            }
        }
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
        
        // Устанавливаем значение по умолчанию
        equipmentSelect.val(['all']).trigger('change');
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
        
        // Устанавливаем значение по умолчанию
        select.val(['all']).trigger('change');
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
        
        // Устанавливаем значение по умолчанию
        select.val(['all']).trigger('change');
    } catch (error) {
        console.error('Ошибка загрузки статусов:', error);
    }
}

async function fetchDataFromDatabase(params = {}) {
    const url = new URL('/gantt/data', window.location.origin);
    Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== undefined && params[key] !== '' && params[key] !== 'all') {
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
    hideError(); // Очищаем предыдущие ошибки
    
    try {
        const equipmentVal = $('#equipment').val();
        const failureTypeVal = $('#failure-type').val();
        const statusVal = $('#status').val();
        
        const params = {
            dateFrom: document.getElementById('date-from').value,
            dateTo: document.getElementById('date-to').value,
            area: document.getElementById('area').value,
            equipment: equipmentVal && equipmentVal.length > 0 && !equipmentVal.includes('all') ? equipmentVal.join(',') : null,
            failureType: failureTypeVal && failureTypeVal.length > 0 && !failureTypeVal.includes('all') ? failureTypeVal.join(',') : null,
            status: statusVal && statusVal.length > 0 && !statusVal.includes('all') ? statusVal.join(',') : null
        };
        
        console.log('Применяем фильтры:', params);
        
        const data = await fetchDataFromDatabase(params);
        generateGanttChart(data);
        
        console.log('Фильтры применены успешно. Найдено записей:', data.length);
    } catch (error) {
        console.error('Ошибка применения фильтров:', error);
        showError('Ошибка применения фильтров: ' + error.message);
    } finally {
        showLoading(false);
    }
}

function generateGanttChart(data) {
    currentData = data;
    const dateFrom = document.getElementById('date-from').value;
    const dateTo = document.getElementById('date-to').value;
    
    const groupedData = groupDataByAreaAndMachine(data);
    
    generateTimeHeader(dateFrom, dateTo);
    generateGanttBody(groupedData, dateFrom, dateTo);
    generateAreaControls(groupedData);
    updateSummary(data);
}

function groupDataByAreaAndMachine(data) {
    const grouped = {};
    data.forEach(item => {
        const area = item.area || 'Other';
        if (!grouped[area]) {
            grouped[area] = {
                machines: {},
                collapsed: true
            };
        }
        if (!grouped[area].machines[item.machine_name]) {
            grouped[area].machines[item.machine_name] = [];
        }
        grouped[area].machines[item.machine_name].push(item);
    });
    return grouped;
}

function generateTimeHeader(dateFrom, dateTo) {
    const timeHeader = document.getElementById('time-header');
    timeHeader.innerHTML = '';
    
    const fromDate = new Date(dateFrom);
    const toDate = new Date(dateTo);
    
    // Определяем шаг времени в зависимости от масштаба
    let stepMinutes, stepLabel;
    if (zoomLevel >= 2.5) {
        stepMinutes = 5; // 5 минут при максимальном увеличении
        stepLabel = (date) => `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    } else if (zoomLevel >= 2) {
        stepMinutes = 15; // 15 минут
        stepLabel = (date) => `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    } else if (zoomLevel >= 1.5) {
        stepMinutes = 30; // 30 минут
        stepLabel = (date) => `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    } else if (zoomLevel >= 1) {
        stepMinutes = 60; // 1 час
        stepLabel = (date) => `${date.getHours().toString().padStart(2, '0')}:00`;
    } else if (zoomLevel >= 0.5) {
        stepMinutes = 120; // 2 часа
        stepLabel = (date) => `${date.getHours().toString().padStart(2, '0')}:00`;
    } else {
        stepMinutes = 180; // 3 часа при максимальном уменьшении
        stepLabel = (date) => `${date.getHours().toString().padStart(2, '0')}:00`;
    }
    
    // Используем ту же формулу расчета ширины, что и для полос
    const baseWidth = 60;
    const minuteWidth = (baseWidth * zoomLevel) / 60; // Ширина одной минуты (должна совпадать с расчетом в generateGanttBody)
    const slotWidth = stepMinutes * minuteWidth; // Ширина слота = количество минут * ширина минуты
    
    // Вычисляем общее количество минут в диапазоне
    const totalMinutes = (toDate - fromDate) / (1000 * 60);
    
    // Находим первый слот, который начинается до или в момент fromDate
    // Округляем fromDate до ближайшего шага вниз
    const fromMinutes = fromDate.getHours() * 60 + fromDate.getMinutes();
    const firstSlotMinutes = Math.floor(fromMinutes / stepMinutes) * stepMinutes;
    const firstSlotDate = new Date(fromDate);
    firstSlotDate.setHours(Math.floor(firstSlotMinutes / 60), firstSlotMinutes % 60, 0, 0);
    
    // Если первый слот раньше fromDate, берем следующий
    if (firstSlotDate < fromDate) {
        firstSlotDate.setMinutes(firstSlotDate.getMinutes() + stepMinutes);
    }
    
    // Создаем слоты
    let currentSlotDate = new Date(firstSlotDate);
    
    // Добавляем начальный слот, если первый слот не совпадает с fromDate
    if (firstSlotDate > fromDate) {
        const preSlotMinutes = (firstSlotDate - fromDate) / (1000 * 60);
        const preSlotWidth = preSlotMinutes * minuteWidth;
        if (preSlotWidth > 0.1) { // Минимальная ширина для отображения
            const preSlot = document.createElement('div');
            preSlot.className = 'time-slot';
            preSlot.style.minWidth = preSlotWidth + 'px';
            preSlot.style.width = preSlotWidth + 'px';
            preSlot.textContent = stepLabel(fromDate);
            timeHeader.appendChild(preSlot);
        }
    }
    
    // Создаем основные слоты с фиксированным шагом
    while (currentSlotDate <= toDate) {
        const timeSlot = document.createElement('div');
        timeSlot.className = 'time-slot';
        timeSlot.style.minWidth = slotWidth + 'px';
        timeSlot.style.width = slotWidth + 'px';
        timeSlot.textContent = stepLabel(currentSlotDate);
        timeHeader.appendChild(timeSlot);
        
        const nextSlotDate = new Date(currentSlotDate);
        nextSlotDate.setMinutes(nextSlotDate.getMinutes() + stepMinutes);
        currentSlotDate = nextSlotDate;
    }
    
    // Обновляем минимальную ширину контейнера временных слотов
    // Используем точный расчет на основе минут
    const timeSlotsContainer = document.querySelector('.time-slots');
    const calculatedWidth = totalMinutes * minuteWidth;
    timeSlotsContainer.style.minWidth = calculatedWidth + 'px';
}

function generateGanttBody(groupedData, dateFrom, dateTo) {
    const ganttBody = document.getElementById('gantt-body');
    ganttBody.innerHTML = '';
    
    const fromDate = new Date(dateFrom);
    const toDate = new Date(dateTo);
    const totalMinutes = (toDate - fromDate) / (1000 * 60);
    const baseWidth = 60;
    const minuteWidth = (baseWidth * zoomLevel) / 60; // Ширина одной минуты (должна совпадать с generateTimeHeader)
    
    // Устанавливаем минимальную ширину для body, чтобы соответствовать header
    const calculatedWidth = totalMinutes * minuteWidth;
    ganttBody.style.minWidth = calculatedWidth + 'px';
    
    // Сортируем участки по алфавиту
    const sortedAreas = Object.keys(groupedData).sort();
    
    sortedAreas.forEach(area => {
        const areaData = groupedData[area];
        
        // Создаем заголовок участка
        const areaHeader = createAreaHeader(area, areaData);
        ganttBody.appendChild(areaHeader);
        
        // Создаем строки для машин в этом участке
        const sortedMachines = Object.keys(areaData.machines).sort();
        sortedMachines.forEach(machine => {
            const machineRow = createMachineRow(machine, area);
            machineRow.style.display = areaData.collapsed ? 'none' : 'flex';
            
            // Устанавливаем минимальную ширину для строки машины
            machineRow.style.minWidth = calculatedWidth + 'px';
            
            areaData.machines[machine].forEach(repair => {
                createRepairBar(repair, machineRow, fromDate, minuteWidth);
            });
            ganttBody.appendChild(machineRow);
        });
    });
}

function createAreaHeader(area, areaData) {
    const header = document.createElement('div');
    header.className = 'area-header';
    if (areaData.collapsed) {
        header.classList.add('collapsed');
    }
    header.setAttribute('data-area', area);
    
    const name = document.createElement('div');
    name.className = 'area-name';
    
    const toggleIcon = document.createElement('span');
    toggleIcon.className = 'toggle-icon';
    toggleIcon.textContent = areaData.collapsed ? '▶' : '▼';
    toggleIcon.style.marginRight = '8px';
    toggleIcon.style.cursor = 'pointer';
    
    const areaTitle = document.createElement('span');
    areaTitle.textContent = getAreaDisplayName(area);
    
    name.appendChild(toggleIcon);
    name.appendChild(areaTitle);
    header.appendChild(name);
    
    // Если участок свернут, добавляем поломки в заголовок
    if (areaData.collapsed) {
        addCollapsedAreaBars(header, areaData, area);
    }
    
    // Добавляем обработчик клика для сворачивания/разворачивания
    toggleIcon.addEventListener('click', function(e) {
        e.stopPropagation();
        toggleAreaCollapse(area);
    });
    
    return header;
}

function createMachineRow(machine, area) {
    const row = document.createElement('div');
    row.className = 'machine-row';
    row.setAttribute('data-area', area);
    
    const name = document.createElement('div');
    name.className = 'machine-name';
    name.textContent = machine;
    name.style.paddingLeft = '30px'; // Отступ для машин под участком
    row.appendChild(name);
    
    return row;
}

function createRepairBar(repair, machineRow, fromDate, minuteWidth) {
    const repairStart = new Date(repair.start_bd_t1);
    const repairEnd = new Date(repair.stop_bd_t4);
    
    const startOffsetMinutes = (repairStart - fromDate) / (1000 * 60);
    const repairDurationMinutes = (repairEnd - repairStart) / (1000 * 60);
    
    // Ширина колонки с именем машины (должна совпадать с CSS)
    const machineNameWidth = 250;
    
    // Позиция полосы: смещение от начала временной шкалы + ширина колонки имени
    const left = machineNameWidth + (startOffsetMinutes * minuteWidth);
    const width = Math.max(repairDurationMinutes * minuteWidth, 3);
    
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
    if (!type) return '#2ecc71';
    
    const normalizedType = type.trim();
    switch (normalizedType) {
        case 'Механика': return '#3498db';
        case 'Электроника': return '#e74c3c';
        case 'Электрика': return '#f39c12';
        default: 
            console.log('Неизвестный тип поломки:', type);
            return '#2ecc71';
    }
}

function getAreaDisplayName(area) {
    const areaNames = {
        'BuildingArea': 'Участок сборки',
        'CuringArea': 'Участок вулканизации',
        'SemifinishingArea': 'Участок полуфабрикатов',
        'FinishigArea': 'Участок заключительных операций',
        'NewMixingArea': 'Участок смешения'
    };
    return areaNames[area] || area;
}

function addCollapsedAreaBars(header, areaData, area) {
    const dateFrom = document.getElementById('date-from').value;
    const dateTo = document.getElementById('date-to').value;
    const fromDate = new Date(dateFrom);
    const toDate = new Date(dateTo);
    const baseWidth = 60;
    const minuteWidth = (baseWidth * zoomLevel) / 60; // Используем ту же формулу
    
    // Устанавливаем минимальную ширину для заголовка участка
    const totalMinutes = (toDate - fromDate) / (1000 * 60);
    const calculatedWidth = totalMinutes * minuteWidth;
    header.style.minWidth = calculatedWidth + 'px';
    
    // Собираем все поломки участка
    const allRepairs = [];
    Object.values(areaData.machines).forEach(machineRepairs => {
        allRepairs.push(...machineRepairs);
    });
    
    // Создаем полоски для каждой поломки
    allRepairs.forEach(repair => {
        createRepairBar(repair, header, fromDate, minuteWidth);
    });
}

function generateAreaControls(groupedData) {
    const areaControls = document.getElementById('area-controls');
    areaControls.innerHTML = '';
    
    const sortedAreas = Object.keys(groupedData).sort();
    
    sortedAreas.forEach(area => {
        const areaData = groupedData[area];
        const button = document.createElement('div');
        button.className = 'area-control-button';
        if (areaData.collapsed) {
            button.classList.add('collapsed');
        }
        
        const icon = document.createElement('span');
        icon.className = 'area-control-icon';
        icon.textContent = areaData.collapsed ? '▶' : '▼';
        
        const label = document.createElement('span');
        label.textContent = getAreaDisplayName(area);
        
        button.appendChild(icon);
        button.appendChild(label);
        
        button.addEventListener('click', function() {
            toggleAreaCollapse(area);
            // Обновляем состояние кнопки
            const isCollapsed = areaData.collapsed;
            button.classList.toggle('collapsed', !isCollapsed);
            icon.textContent = isCollapsed ? '▼' : '▶';
        });
        
        areaControls.appendChild(button);
    });
}

function toggleAreaCollapse(area) {
    // Находим все строки машин для этого участка
    const machineRows = document.querySelectorAll(`.machine-row[data-area="${area}"]`);
    const areaHeader = document.querySelector(`.area-header[data-area="${area}"]`);
    const toggleIcon = areaHeader.querySelector('.toggle-icon');
    
    // Переключаем состояние
    const isCollapsed = machineRows[0] && machineRows[0].style.display === 'none';
    
    machineRows.forEach(row => {
        row.style.display = isCollapsed ? 'flex' : 'none';
    });
    
    toggleIcon.textContent = isCollapsed ? '▼' : '▶';
    
    // Обновляем стиль заголовка
    if (isCollapsed) {
        areaHeader.classList.remove('collapsed');
        // Удаляем полоски поломок из заголовка
        const existingBars = areaHeader.querySelectorAll('.repair-bar');
        existingBars.forEach(bar => bar.remove());
    } else {
        areaHeader.classList.add('collapsed');
        // Добавляем полоски поломок в заголовок
        if (currentData.length > 0) {
            const groupedData = groupDataByAreaAndMachine(currentData);
            addCollapsedAreaBars(areaHeader, groupedData[area], area);
        }
    }
    
    // Обновляем состояние в данных
    if (currentData.length > 0) {
        const groupedData = groupDataByAreaAndMachine(currentData);
        groupedData[area].collapsed = !isCollapsed;
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

function hideError() {
    const errorDiv = document.getElementById('error');
    errorDiv.style.display = 'none';
}