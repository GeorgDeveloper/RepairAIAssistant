let mainChart = null;
let currentMode = 'downtime';
let chartType = 'line';
let chartData = null;
let showLabels = false;
let drillChart = null;
let drillLevel = 0; // 0 none, 1 equipment, 2 causes, 3 mechanisms
let currentFailureType = null;
let currentPeriod = null;
let currentYear = null; // Год из dataset.label
let currentMachine = null;
let currentCause = null;
let currentParams = null;

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadInitialData();
    
    // Обработчики для модального окна drill-down
    const closeBtn = document.getElementById('closeBtn');
    const backBtn = document.getElementById('backBtn');
    
    if (closeBtn) {
        closeBtn.addEventListener('click', closeModal);
    }
    
    if (backBtn) {
        backBtn.addEventListener('click', () => {
            if (drillLevel === 4) {
                drillToMechanisms();
            } else if (drillLevel === 3) {
                drillToCauses();
            } else if (drillLevel === 2) {
                openDrilldownEquipment(currentFailureType, currentPeriod, currentYear);
            } else {
                closeModal();
            }
        });
    }
});

function setupEventListeners() {
    // Настройка множественного выбора для года
    setupMultiSelect('year', function() {
        loadMonths();
        updateWeekFilterState();
    });
    
    // Настройка множественного выбора для месяца
    setupMultiSelect('month', function() {
        updateWeekFilterState();
        loadWeeks();
    });
    
    // Настройка множественного выбора для недели
    setupMultiSelect('week');
    
    // Настройка множественного выбора для участка
    setupMultiSelect('area', function() {
        loadEquipment(getSelectedValues('area'));
    });
    
    // Настройка множественного выбора для оборудования
    setupMultiSelect('equipment');
    
    document.getElementById('apply-filters').addEventListener('click', function() {
        applyFilters();
    });
    
    document.getElementById('toggleChart').addEventListener('click', function() {
        toggleChartMode();
    });
    
    document.getElementById('toggleChartType').addEventListener('click', function() {
        toggleChartType();
    });
    
    document.getElementById('toggleAllLegend').addEventListener('click', function() {
        toggleAllLegendItems();
    });
    
    document.getElementById('toggleLabels').addEventListener('click', function() {
        showLabels = !showLabels;
        document.getElementById('toggleLabels').textContent = `Значения: ${showLabels ? 'вкл' : 'выкл'}`;
        if (chartData) {
            createMainChart();
        }
    });
}

function setupMultiSelect(prefix, onChangeCallback) {
    const button = document.getElementById(prefix + '-button');
    const dropdown = document.getElementById(prefix + '-dropdown');
    const text = document.getElementById(prefix + '-text');
    const allCheckbox = document.getElementById(prefix + '-all');
    
    // Обработчик клика по кнопке
    button.addEventListener('click', function(e) {
        e.stopPropagation();
        toggleDropdown(prefix);
    });
    
    // Обработчик клика по чекбоксу "Все"
    allCheckbox.addEventListener('change', function() {
        const checkboxes = dropdown.querySelectorAll('input[type="checkbox"]:not(#' + prefix + '-all)');
        checkboxes.forEach(cb => {
            cb.checked = this.checked;
        });
        updateButtonText(prefix);
        if (onChangeCallback) onChangeCallback();
    });
    
    // Обработчики для остальных чекбоксов
    dropdown.addEventListener('change', function(e) {
        if (e.target.type === 'checkbox' && e.target.id !== prefix + '-all') {
            const checkedBoxes = dropdown.querySelectorAll('input[type="checkbox"]:checked:not(#' + prefix + '-all)');
            allCheckbox.checked = checkedBoxes.length === 0;
            updateButtonText(prefix);
            if (onChangeCallback) onChangeCallback();
        }
    });
    
    // Закрытие при клике вне элемента
    document.addEventListener('click', function(e) {
        if (!e.target.closest('#' + prefix + '-multiselect')) {
            closeDropdown(prefix);
        }
    });
}

function toggleDropdown(prefix) {
    const button = document.getElementById(prefix + '-button');
    const dropdown = document.getElementById(prefix + '-dropdown');
    
    // Закрыть все остальные выпадающие списки
    document.querySelectorAll('.multi-select-dropdown.open').forEach(dd => {
        if (dd.id !== prefix + '-dropdown') {
            dd.classList.remove('open');
            dd.previousElementSibling.classList.remove('open');
        }
    });
    
    dropdown.classList.toggle('open');
    button.classList.toggle('open');
}

function closeDropdown(prefix) {
    const button = document.getElementById(prefix + '-button');
    const dropdown = document.getElementById(prefix + '-dropdown');
    dropdown.classList.remove('open');
    button.classList.remove('open');
}

function updateButtonText(prefix) {
    const text = document.getElementById(prefix + '-text');
    const dropdown = document.getElementById(prefix + '-dropdown');
    const checkedBoxes = dropdown.querySelectorAll('input[type="checkbox"]:checked:not(#' + prefix + '-all)');
    
    if (checkedBoxes.length === 0) {
        const allCheckbox = document.getElementById(prefix + '-all');
        text.textContent = allCheckbox.checked ? 'Все' : 'Ничего не выбрано';
    } else if (checkedBoxes.length === 1) {
        text.textContent = checkedBoxes[0].nextElementSibling.textContent;
    } else {
        text.textContent = `Выбрано: ${checkedBoxes.length}`;
    }
}

function getSelectedValues(prefix) {
    const dropdown = document.getElementById(prefix + '-dropdown');
    const allCheckbox = document.getElementById(prefix + '-all');
    
    if (allCheckbox.checked) {
        return ['all'];
    }
    
    const checkedBoxes = dropdown.querySelectorAll('input[type="checkbox"]:checked:not(#' + prefix + '-all)');
    return Array.from(checkedBoxes).map(cb => cb.value);
}

function populateMultiSelect(prefix, data, valueField, allText) {
    const dropdown = document.getElementById(prefix + '-dropdown');
    const allCheckbox = document.getElementById(prefix + '-all');
    
    // Очищаем все опции кроме "Все"
    const existingOptions = dropdown.querySelectorAll('.multi-select-option:not(:first-child)');
    existingOptions.forEach(option => option.remove());
    
    // Добавляем новые опции
    data.forEach(item => {
        const option = document.createElement('div');
        option.className = 'multi-select-option';
        const value = item[valueField];
        const displayText = item[valueField];
        const safeId = `${prefix}-${value}`.replace(/[^a-zA-Z0-9-_]/g, '_');
        
        option.innerHTML = `
            <input type="checkbox" id="${safeId}" value="${value}">
            <label for="${safeId}">${displayText}</label>
        `;
        dropdown.appendChild(option);
    });
    
    // Обновляем текст кнопки
    updateButtonText(prefix);
}

async function loadInitialData() {
    showLoading(true);
    try {
        await Promise.all([
            loadYears(),
            loadAreas()
        ]);
        await loadEquipment(['all']);
        updateWeekFilterState();
        await applyFilters();
    } catch (error) {
        showError('Ошибка загрузки данных: ' + error.message);
    } finally {
        showLoading(false);
    }
}

async function loadYears() {
    try {
        const response = await fetch('/dynamics/years');
        const years = await response.json();
        
        populateMultiSelect('year', years, 'year', 'Все');
        
        // Устанавливаем текущий год по умолчанию
        const currentYear = new Date().getFullYear().toString();
        const safeId = `year-${currentYear}`.replace(/[^a-zA-Z0-9-_]/g, '_');
        const currentYearCheckbox = document.getElementById(safeId);
        if (currentYearCheckbox) {
            currentYearCheckbox.checked = true;
            document.getElementById('year-all').checked = false;
            updateButtonText('year');
        }
    } catch (error) {
        console.error('Ошибка загрузки годов:', error);
    }
}

async function loadMonths() {
    const years = getSelectedValues('year');
    if (years.includes('all')) return;
    
    try {
        const response = await fetch(`/dynamics/months?year=${years.join(',')}`);
        const months = await response.json();
        
        const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
        
        const monthsWithNames = months.map(item => ({
            month: item.month,
            monthName: monthNames[item.month - 1]
        }));
        
        populateMultiSelect('month', monthsWithNames, 'monthName', 'Все');
        updateWeekFilterState();
        loadWeeks();
    } catch (error) {
        console.error('Ошибка загрузки месяцев:', error);
    }
}

function updateWeekFilterState() {
    const years = getSelectedValues('year');
    const months = getSelectedValues('month');
    const weekButton = document.getElementById('week-button');
    const weekMultiselect = document.getElementById('week-multiselect');
    
    // Проверяем, выбрано ли больше одного года или больше одного месяца
    const hasMultipleYears = !years.includes('all') && years.length > 1;
    const hasMultipleMonths = !months.includes('all') && months.length > 1;
    const shouldDisable = hasMultipleYears || hasMultipleMonths;
    
    if (shouldDisable) {
        // Отключаем выбор недель
        weekButton.disabled = true;
        weekButton.style.opacity = '0.5';
        weekButton.style.cursor = 'not-allowed';
        weekMultiselect.style.pointerEvents = 'none';
        
        // Очищаем выбранные недели
        const weekDropdown = document.getElementById('week-dropdown');
        const weekAllCheckbox = document.getElementById('week-all');
        const weekCheckboxes = weekDropdown.querySelectorAll('input[type="checkbox"]:not(#week-all)');
        weekCheckboxes.forEach(cb => cb.checked = false);
        weekAllCheckbox.checked = true;
        updateButtonText('week');
        
        // Очищаем список недель
        const existingOptions = weekDropdown.querySelectorAll('.multi-select-option:not(:first-child)');
        existingOptions.forEach(option => option.remove());
    } else {
        // Включаем выбор недель
        weekButton.disabled = false;
        weekButton.style.opacity = '1';
        weekButton.style.cursor = 'pointer';
        weekMultiselect.style.pointerEvents = 'auto';
    }
}

async function loadWeeks() {
    const years = getSelectedValues('year');
    const months = getSelectedValues('month');
    
    // Проверяем, нужно ли отключить выбор недель
    const hasMultipleYears = !years.includes('all') && years.length > 1;
    const hasMultipleMonths = !months.includes('all') && months.length > 1;
    
    if (years.includes('all') || months.includes('all') || hasMultipleYears || hasMultipleMonths) {
        const dropdown = document.getElementById('week-dropdown');
        const existingOptions = dropdown.querySelectorAll('.multi-select-option:not(:first-child)');
        existingOptions.forEach(option => option.remove());
        updateButtonText('week');
        updateWeekFilterState();
        return;
    }
    
    try {
        // Преобразуем названия месяцев обратно в номера
        const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
        const monthNumbers = months.map(monthName => {
            const index = monthNames.indexOf(monthName);
            return index + 1;
        });
        
        const response = await fetch(`/dynamics/weeks?year=${years.join(',')}&month=${monthNumbers.join(',')}`);
        const weeks = await response.json();
        
        populateMultiSelect('week', weeks, 'week', 'Все');
        updateWeekFilterState();
    } catch (error) {
        console.error('Ошибка загрузки недель:', error);
    }
}

async function loadAreas() {
    try {
        const response = await fetch('/gantt/areas');
        const areas = await response.json();
        
        populateMultiSelect('area', areas, 'area', 'Все участки');
    } catch (error) {
        console.error('Ошибка загрузки участков:', error);
    }
}

async function loadEquipment(areas) {
    try {
        if (!areas || areas.includes('all')) {
            // Загружаем все оборудование
            const response = await fetch('/gantt/equipment');
            const equipment = await response.json();
            populateMultiSelect('equipment', equipment, 'machine_name', 'Всё оборудование');
        } else {
            // Загружаем оборудование для выбранных участков
            const equipmentPromises = areas.map(area => 
                fetch(`/gantt/equipment?area=${encodeURIComponent(area)}`).then(r => r.json())
            );
            const equipmentArrays = await Promise.all(equipmentPromises);
            const allEquipment = equipmentArrays.flat();
            const uniqueEquipment = allEquipment.filter((item, index, self) => 
                index === self.findIndex(t => t.machine_name === item.machine_name)
            );
            populateMultiSelect('equipment', uniqueEquipment, 'machine_name', 'Всё оборудование');
        }
    } catch (error) {
        console.error('Ошибка загрузки оборудования:', error);
    }
}

async function applyFilters() {
    showLoading(true);
    try {
        const months = getSelectedValues('month');
        const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
        
        let monthNumbers = months;
        if (!months.includes('all')) {
            monthNumbers = months.map(monthName => {
                const index = monthNames.indexOf(monthName);
                return index + 1;
            });
        }
        
        const params = {
            year: getSelectedValues('year'),
            month: monthNumbers,
            week: getSelectedValues('week'),
            area: getSelectedValues('area'),
            equipment: getSelectedValues('equipment')
        };
        
        console.log('Параметры фильтрации:', params);
        
        const data = await fetchDynamicsData(params);
        console.log('Полученные данные:', data);
        
        generateChart(data, params);
    } catch (error) {
        console.error('Ошибка применения фильтров:', error);
        showError('Ошибка применения фильтров: ' + error.message);
    } finally {
        showLoading(false);
    }
}

async function fetchDynamicsData(params) {
    const url = new URL('/dynamics/type/data', window.location.origin);
    Object.keys(params).forEach(key => {
        if (params[key] && params[key].length > 0 && !params[key].includes('all')) {
            params[key].forEach(value => {
                url.searchParams.append(key, value.toString());
            });
        }
    });
    
    console.log('Запрос к серверу:', url.toString());
    
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
}

// Функция для преобразования секунд в формат HH:MM:SS
function formatTimeFromSeconds(seconds) {
    if (!seconds || seconds === 0) return '0:00:00';
    
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

// Функция для преобразования времени в секунды для расчета
function timeToSeconds(timeStr) {
    if (!timeStr) return 0;
    const parts = timeStr.split(':');
    if (parts.length === 3) {
        return parseInt(parts[0]) * 3600 + parseInt(parts[1]) * 60 + parseInt(parts[2]);
    }
    return 0;
}

function generateChart(data, params) {
    const labels = generateLabels(data, params);
    chartData = {
        labels: labels,
        downtimeDatasets: generateDowntimeDatasets(data, labels),
        countDatasets: generateCountDatasets(data, labels),
        params: params
    };
    
    createMainChart();
    createCustomLegend(chartData.downtimeDatasets, data);
}

function createMainChart() {
    const ctx = document.getElementById('mainChart').getContext('2d');
    
    if (mainChart) {
        mainChart.destroy();
    }
    
    const datasets = currentMode === 'downtime' ? chartData.downtimeDatasets : chartData.countDatasets;
    const title = currentMode === 'downtime' ? 'Динамика времени простоя' : 'Количество вызовов';
    const yAxisTitle = currentMode === 'downtime' ? 'Время простоя (HH:MM:SS)' : 'Количество вызовов';
    
    // Определяем формат Y-оси
    const yAxisConfig = {
        beginAtZero: true,
        stacked: chartType === 'bar',
        title: {
            display: true,
            text: yAxisTitle
        }
    };
    
    // Если режим времени простоя, используем кастомный формат для отображения времени
    if (currentMode === 'downtime') {
        yAxisConfig.ticks = {
            callback: function(value) {
                return formatTimeFromSeconds(value);
            }
        };
    }
    
    // Регистрируем плагин datalabels если он доступен
    if (window.ChartDataLabels && !Chart.registry.plugins.get('datalabels')) {
        Chart.register(window.ChartDataLabels);
    }
    
    mainChart = new Chart(ctx, {
        type: chartType,
        data: {
            labels: chartData.labels,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: title
                },
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            if (currentMode === 'downtime') {
                                return `${context.dataset.label}: ${formatTimeFromSeconds(context.parsed.y)}`;
                            } else {
                                return `${context.dataset.label}: ${context.parsed.y}`;
                            }
                        }
                    }
                },
                datalabels: {
                    display: showLabels,
                    color: '#333',
                    align: 'top',
                    anchor: 'end',
                    formatter: function(value, context) {
                        if (currentMode === 'downtime') {
                            return formatTimeFromSeconds(value);
                        } else {
                            return value != null ? value : '';
                        }
                    }
                }
            },
            scales: {
                y: yAxisConfig,
                x: {
                    stacked: chartType === 'bar',
                    title: {
                        display: true,
                        text: getXAxisTitle(chartData.params)
                    }
                }
            },
            interaction: {
                mode: 'nearest',
                intersect: false
            },
            onClick: (_evt, elems) => {
                console.log('Chart clicked', elems);
                if (!elems?.length) return;
                const element = elems[0];
                const datasetIndex = element.datasetIndex;
                const dataIndex = element.index;
                
                const dataset = mainChart.data.datasets[datasetIndex];
                let failureType = dataset.label;
                let year = null;
                
                // Извлекаем год из названия если есть (формат: "Тип поломки (2024)")
                if (failureType.includes(' (')) {
                    const match = failureType.match(/\((\d{4})\)/);
                    if (match) {
                        year = parseInt(match[1]);
                        failureType = failureType.split(' (')[0];
                    }
                }
                
                const periodLabel = mainChart.data.labels[dataIndex];
                
                console.log('Opening drill-down:', { failureType, periodLabel, year });
                
                // Сохраняем текущие параметры фильтров
                currentParams = chartData.params;
                currentFailureType = failureType;
                currentPeriod = periodLabel;
                currentYear = year;
                
                openDrilldownEquipment(failureType, periodLabel, year);
            }
        }
    });
    
    // Альтернативный способ обработки клика для line charts
    // Используем обработчик на canvas, так как onClick в options может не работать для всех типов графиков
    const canvas = document.getElementById('mainChart');
    if (canvas && mainChart) {
        // Удаляем старый обработчик если есть
        if (canvas._chartClickHandler) {
            canvas.removeEventListener('click', canvas._chartClickHandler);
        }
        
        // Создаем новый обработчик
        canvas._chartClickHandler = (evt) => {
            console.log('Canvas clicked', evt);
            if (!mainChart) {
                console.log('Chart not initialized');
                return;
            }
            
            // Пробуем разные режимы определения точки клика
            let points = mainChart.getElementsAtEventForMode(evt, 'nearest', { intersect: true }, true);
            
            if (!points.length) {
                // Для line charts пробуем режим 'index'
                points = mainChart.getElementsAtEventForMode(evt, 'index', { intersect: false }, true);
            }
            
            if (!points.length) {
                // Пробуем режим 'point'
                points = mainChart.getElementsAtEventForMode(evt, 'point', { intersect: true }, true);
            }
            
            if (points.length) {
                console.log('Found points:', points.length, points[0]);
                handleChartClick(points[0]);
            } else {
                console.log('No points found at click position');
            }
        };
        
        canvas.addEventListener('click', canvas._chartClickHandler);
        console.log('Chart click handler attached');
    }
}

function handleChartClick(element) {
    if (!element) return;
    
    const datasetIndex = element.datasetIndex;
    const dataIndex = element.index;
    
    const dataset = mainChart.data.datasets[datasetIndex];
    let failureType = dataset.label;
    let year = null;
    
    // Извлекаем год из названия если есть (формат: "Тип поломки (2024)")
    if (failureType.includes(' (')) {
        const match = failureType.match(/\((\d{4})\)/);
        if (match) {
            year = parseInt(match[1]);
            failureType = failureType.split(' (')[0];
        }
    }
    
    const periodLabel = mainChart.data.labels[dataIndex];
    
    console.log('Opening drill-down:', { failureType, periodLabel, year });
    
    // Сохраняем текущие параметры фильтров
    currentParams = chartData.params;
    currentFailureType = failureType;
    currentPeriod = periodLabel;
    currentYear = year;
    
    openDrilldownEquipment(failureType, periodLabel, year);
}

function createCustomLegend(datasets, rawData) {
    const legendContainer = document.getElementById('customLegend');
    legendContainer.innerHTML = '';
    
    // Создаем объект для хранения данных по каждому dataset (с учетом года в режиме сравнения)
    const datasetData = {};
    
    // Собираем данные из rawData для отображения в легенде
    rawData.forEach(item => {
        const type = item.failure_type?.trim();
        if (!type) return;
        
        // Формируем ключ для dataset: если есть год, используем формат "тип (год)", иначе просто "тип"
        const year = item.year ? parseInt(item.year) : null;
        const datasetKey = year ? `${type} (${year})` : type;
        
        if (!datasetData[datasetKey]) {
            datasetData[datasetKey] = {
                totalDowntime: 0,
                totalCount: 0
            };
        }
        
        const downtimeSeconds = item.total_downtime_seconds ? parseFloat(item.total_downtime_seconds) : 0;
        const count = item.failure_count ? parseInt(item.failure_count) : 0;
        
        datasetData[datasetKey].totalDowntime += downtimeSeconds;
        datasetData[datasetKey].totalCount += count;
    });
    
    datasets.forEach(dataset => {
        const legendItem = document.createElement('div');
        legendItem.style.cssText = 'display: flex; align-items: center; margin-bottom: 8px; cursor: pointer;';
        legendItem.dataset.label = dataset.label;
        
        const colorBox = document.createElement('div');
        colorBox.style.cssText = `width: 20px; height: 3px; background-color: ${dataset.borderColor}; margin-right: 8px; border-radius: 2px;`;
        
        const label = document.createElement('span');
        const typeData = datasetData[dataset.label] || { totalDowntime: 0, totalCount: 0 };
        const downtimeText = formatTimeFromSeconds(typeData.totalDowntime);
        label.textContent = `${dataset.label} (${downtimeText}, ${typeData.totalCount} случаев)`;
        label.style.fontSize = '12px';
        
        legendItem.appendChild(colorBox);
        legendItem.appendChild(label);
        legendContainer.appendChild(legendItem);
        
        legendItem.addEventListener('click', function() {
            const datasetIndex = mainChart.data.datasets.findIndex(d => d.label === dataset.label);
            if (datasetIndex !== -1) {
                const meta = mainChart.getDatasetMeta(datasetIndex);
                meta.hidden = !meta.hidden;
                legendItem.style.opacity = meta.hidden ? '0.5' : '1';
                mainChart.update();
                updateToggleAllButton();
            }
        });
    });
    
    updateToggleAllButton();
}

function toggleAllLegendItems() {
    if (!mainChart) return;
    
    const legendItems = document.querySelectorAll('#customLegend > div');
    const firstItemHidden = legendItems.length > 0 && (legendItems[0].style.opacity === '0.5');
    
    mainChart.data.datasets.forEach((dataset, index) => {
        const meta = mainChart.getDatasetMeta(index);
        meta.hidden = !firstItemHidden;
    });
    
    legendItems.forEach(item => {
        item.style.opacity = firstItemHidden ? '1' : '0.5';
    });
    
    mainChart.update();
    updateToggleAllButton();
}

function updateToggleAllButton() {
    const button = document.getElementById('toggleAllLegend');
    const legendItems = document.querySelectorAll('#customLegend > div');
    
    if (legendItems.length === 0) return;
    
    const firstItemHidden = legendItems[0].style.opacity === '0.5';
    button.textContent = firstItemHidden ? 'Включить все' : 'Отключить все';
}

function toggleChartMode() {
    currentMode = currentMode === 'downtime' ? 'count' : 'downtime';
    const button = document.getElementById('toggleChart');
    button.textContent = currentMode === 'downtime' ? 'Показать количество' : 'Показать время';
    
    if (chartData) {
        createMainChart();
    }
}

function toggleChartType() {
    chartType = chartType === 'line' ? 'bar' : 'line';
    const button = document.getElementById('toggleChartType');
    button.textContent = chartType === 'line' ? 'Накопительная' : 'Линейная';
    
    if (chartData) {
        createMainChart();
    }
}

function generateLabels(data, params) {
    // Проверяем, есть ли данные с годом для сравнения
    const hasYearData = data.length > 0 && data[0].year != null;
    const hasMultipleYears = params.year && params.year.length > 1 && !params.year.includes('all');
    const hasMultipleMonths = params.month && params.month.length > 1 && !params.month.includes('all');
    const hasMonthSelected = params.month && params.month.length > 0 && !params.month.includes('all');
    const hasYearSelected = params.year && params.year.length > 0 && !params.year.includes('all');
    
    // Если выбраны месяцы и годы для сравнения (несколько месяцев ИЛИ несколько годов)
    if (hasMonthSelected && hasYearSelected && (hasMultipleMonths || hasMultipleYears)) {
        // Проверяем, есть ли данные с month и year для сравнения
        const hasMonthInData = data.length > 0 && data[0].month != null;
        
        if (hasMonthInData && hasYearData) {
            // Сравнение месяцев по годам или годов по месяцам
            // Используем только названия месяцев, без года - данные для разных годов будут рядом
            const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
            const months = [...new Set(data.map(item => parseInt(item.month)))].sort((a, b) => a - b);
            
            // Создаем метки только по месяцам: "янв", "фев", "мар"...
            const labels = months.map(month => monthAbbr[month - 1]);
            return labels;
        }
    }
    
    // Если выбран год и не выбран месяц, группируем по месяцам
    if (hasYearSelected && (!hasMonthSelected || params.month.includes('all'))) {
        if (hasMultipleYears && hasYearData) {
            // Сравнение нескольких годов по месяцам
            // Используем только названия месяцев, без года - данные для разных годов будут рядом
            const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
            const months = [...new Set(data.map(item => parseInt(item.period_label)))].sort((a, b) => a - b);
            
            // Создаем метки только по месяцам: "янв", "фев", "мар"...
            const labels = months.map(month => monthAbbr[month - 1]);
            return labels;
        } else {
            // Один год - обычные метки месяцев
            const uniqueMonths = [...new Set(data.map(item => parseInt(item.period_label)))].sort((a, b) => a - b);
            const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
            return uniqueMonths.map(month => monthAbbr[month - 1]);
        }
    }
    
    // Если выбраны месяцы (для сравнения или одного месяца), но нет года в данных
    if (hasMonthSelected && !hasYearData) {
        // Используем period_label как есть (это недели)
        const uniqueLabels = [...new Set(data.map(item => item.period_label))].sort((a, b) => {
            const aNum = parseInt(a);
            const bNum = parseInt(b);
            if (!isNaN(aNum) && !isNaN(bNum)) {
                return aNum - bNum;
            }
            return a > b ? 1 : -1;
        });
        return uniqueLabels;
    }
    
    // По умолчанию используем period_label как есть
    const uniqueLabels = [...new Set(data.map(item => item.period_label))].sort((a, b) => {
        const aNum = parseInt(a);
        const bNum = parseInt(b);
        if (!isNaN(aNum) && !isNaN(bNum)) {
            return aNum - bNum;
        }
        return a > b ? 1 : -1;
    });
    return uniqueLabels;
}

function generateDowntimeDatasets(data, labels) {
    const uniqueTypes = [...new Set(data.map(item => item.failure_type?.trim()).filter(type => type))].sort();
    const colors = [
        '#4A90E2', '#F5A623', '#7ED321', '#BD10E0', '#50E3C2', '#B8E986', '#9013FE', '#D0021B',
        '#417505', '#7B68EE', '#FF6347', '#32CD32', '#FFD700', '#FF69B4', '#00CED1', '#FF4500'
    ];
    
    // Проверяем, есть ли данные с годом и месяцем для сравнения
    const hasYearData = data.length > 0 && data[0].year != null;
    const hasMonthData = data.length > 0 && data[0].month != null;
    const hasMultipleYears = data.some(item => item.year != null) && 
                           [...new Set(data.map(item => item.year).filter(y => y != null))].length > 1;
    const hasMultipleMonths = data.some(item => item.month != null) && 
                           [...new Set(data.map(item => item.month).filter(m => m != null))].length > 1;
    
    // Режим сравнения: если есть несколько годов ИЛИ несколько месяцев с данными
    if (hasYearData && (hasMultipleYears || (hasMonthData && hasMultipleMonths))) {
        // Режим сравнения: создаем dataset для каждого типа поломки + года
        // Используем расширенную палитру цветов для уникальности
        const yearColors = [
            '#4A90E2', '#F5A623', '#7ED321', '#BD10E0', '#50E3C2', '#B8E986', '#9013FE', '#D0021B',
            '#417505', '#7B68EE', '#FF6347', '#32CD32', '#FFD700', '#FF69B4', '#00CED1', '#FF4500',
            '#8E44AD', '#E67E22', '#1ABC9C', '#3498DB', '#9B59B6', '#F39C12', '#16A085', '#2980B9',
            '#C0392B', '#D35400', '#27AE60', '#2ECC71', '#E74C3C', '#95A5A6', '#34495E', '#7F8C8D'
        ];
        const datasets = [];
        let colorIndex = 0;
        
        uniqueTypes.forEach(type => {
            const yearValues = data.map(item => item.year ? parseInt(item.year) : null).filter(y => y != null && !isNaN(y));
            const years = [...new Set(yearValues)].sort();
            
            years.forEach(year => {
                const chartData = labels.map(label => {
                    // Преобразуем label в строку для безопасности
                    const labelStr = String(label);
                    // Теперь label - это просто название месяца: "янв", "фев" и т.д.
                    const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
                    const monthNum = monthAbbr.indexOf(labelStr) + 1;
                    
                    if (monthNum > 0) {
                        const items = data.filter(item => {
                            const itemType = item.failure_type?.trim();
                            const itemYear = item.year ? parseInt(item.year) : null;
                            // Если есть поле month в данных, используем его, иначе period_label
                            const itemMonth = item.month ? parseInt(item.month) : parseInt(item.period_label);
                            return itemType === type && itemYear === year && itemMonth === monthNum;
                        });
                        
                        return items.reduce((sum, item) => sum + (parseFloat(item.total_downtime_seconds) || 0), 0);
                    }
                    return 0;
                });
                
                datasets.push({
                    label: `${type} (${year})`,
                    data: chartData,
                    borderColor: yearColors[colorIndex % yearColors.length],
                    backgroundColor: yearColors[colorIndex % yearColors.length] + '80',
                    tension: 0.1,
                    borderWidth: 2
                });
                colorIndex++;
            });
        });
        
        return datasets;
    } else {
        // Обычный режим без сравнения
        return uniqueTypes.map((type, index) => {
            const chartData = labels.map((label, labelIndex) => {
                // Преобразуем label в строку для безопасности
                const labelStr = String(label);
                // Определяем номер месяца для поиска
                let monthNumber = null;
                let labelYear = null;
                
                // Парсим метку вида "янв 2024" или просто "янв"
                const parts = labelStr.split(' ');
                if (parts.length === 2) {
                    const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
                    monthNumber = monthAbbr.indexOf(parts[0]) + 1;
                    labelYear = parseInt(parts[1]);
                } else if (labelStr.length <= 3 && ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'].includes(labelStr)) {
                    const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
                    monthNumber = monthAbbr.indexOf(labelStr) + 1;
                }
                
                const items = data.filter(item => {
                    const itemType = item.failure_type?.trim();
                    const itemPeriod = parseInt(item.period_label);
                    
                    if (itemType !== type) return false;
                    
                    if (monthNumber !== null) {
                        if (labelYear !== null && hasYearData) {
                            const itemYear = parseInt(item.year);
                            return itemPeriod === monthNumber && itemYear === labelYear;
                        }
                        return itemPeriod === monthNumber;
                    } else {
                        return item.period_label == label;
                    }
                });
                
                // Суммируем время простоя в секундах
                const totalSeconds = items.reduce((sum, item) => {
                    return sum + (parseFloat(item.total_downtime_seconds) || 0);
                }, 0);
                
                return totalSeconds;
            });
            
            return {
                label: type,
                data: chartData,
                borderColor: colors[index % colors.length],
                backgroundColor: colors[index % colors.length] + '80',
                tension: 0.1,
                borderWidth: 2
            };
        });
    }
}

function generateCountDatasets(data, labels) {
    const uniqueTypes = [...new Set(data.map(item => item.failure_type?.trim()).filter(type => type))].sort();
    const colors = [
        '#4A90E2', '#F5A623', '#7ED321', '#BD10E0', '#50E3C2', '#B8E986', '#9013FE', '#D0021B',
        '#417505', '#7B68EE', '#FF6347', '#32CD32', '#FFD700', '#FF69B4', '#00CED1', '#FF4500'
    ];
    
    // Проверяем, есть ли данные с годом и месяцем для сравнения
    const hasYearData = data.length > 0 && data[0].year != null;
    const hasMonthData = data.length > 0 && data[0].month != null;
    const hasMultipleYears = data.some(item => item.year != null) && 
                           [...new Set(data.map(item => item.year).filter(y => y != null))].length > 1;
    const hasMultipleMonths = data.some(item => item.month != null) && 
                           [...new Set(data.map(item => item.month).filter(m => m != null))].length > 1;
    
    // Режим сравнения: если есть несколько годов ИЛИ несколько месяцев с данными
    if (hasYearData && (hasMultipleYears || (hasMonthData && hasMultipleMonths))) {
        // Режим сравнения: создаем dataset для каждого типа поломки + года
        // Используем расширенную палитру цветов для уникальности
        const yearColors = [
            '#4A90E2', '#F5A623', '#7ED321', '#BD10E0', '#50E3C2', '#B8E986', '#9013FE', '#D0021B',
            '#417505', '#7B68EE', '#FF6347', '#32CD32', '#FFD700', '#FF69B4', '#00CED1', '#FF4500',
            '#8E44AD', '#E67E22', '#1ABC9C', '#3498DB', '#9B59B6', '#F39C12', '#16A085', '#2980B9',
            '#C0392B', '#D35400', '#27AE60', '#2ECC71', '#E74C3C', '#95A5A6', '#34495E', '#7F8C8D'
        ];
        const datasets = [];
        let colorIndex = 0;
        
        uniqueTypes.forEach(type => {
            const yearValues = data.map(item => item.year ? parseInt(item.year) : null).filter(y => y != null && !isNaN(y));
            const years = [...new Set(yearValues)].sort();
            
            years.forEach(year => {
                const chartData = labels.map(label => {
                    // Преобразуем label в строку для безопасности
                    const labelStr = String(label);
                    // Теперь label - это просто название месяца: "янв", "фев" и т.д.
                    const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
                    const monthNum = monthAbbr.indexOf(labelStr) + 1;
                    
                    if (monthNum > 0) {
                        const items = data.filter(item => {
                            const itemType = item.failure_type?.trim();
                            const itemYear = item.year ? parseInt(item.year) : null;
                            // Если есть поле month в данных, используем его, иначе period_label
                            const itemMonth = item.month ? parseInt(item.month) : parseInt(item.period_label);
                            return itemType === type && itemYear === year && itemMonth === monthNum;
                        });
                        
                        return items.reduce((sum, item) => sum + (parseInt(item.failure_count) || 0), 0);
                    }
                    return 0;
                });
                
                datasets.push({
                    label: `${type} (${year})`,
                    data: chartData,
                    borderColor: yearColors[colorIndex % yearColors.length],
                    backgroundColor: yearColors[colorIndex % yearColors.length] + '80',
                    tension: 0.1,
                    borderWidth: 2
                });
                colorIndex++;
            });
        });
        
        return datasets;
    } else {
        // Обычный режим без сравнения
        return uniqueTypes.map((type, index) => {
            const chartData = labels.map((label, labelIndex) => {
                // Преобразуем label в строку для безопасности
                const labelStr = String(label);
                // Определяем номер месяца для поиска
                let monthNumber = null;
                let labelYear = null;
                
                // Парсим метку вида "янв 2024" или просто "янв"
                const parts = labelStr.split(' ');
                if (parts.length === 2) {
                    const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
                    monthNumber = monthAbbr.indexOf(parts[0]) + 1;
                    labelYear = parseInt(parts[1]);
                } else if (labelStr.length <= 3 && ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'].includes(labelStr)) {
                    const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
                    monthNumber = monthAbbr.indexOf(labelStr) + 1;
                }
                
                const items = data.filter(item => {
                    const itemType = item.failure_type?.trim();
                    const itemPeriod = parseInt(item.period_label);
                    
                    if (itemType !== type) return false;
                    
                    if (monthNumber !== null) {
                        if (labelYear !== null && hasYearData) {
                            const itemYear = parseInt(item.year);
                            return itemPeriod === monthNumber && itemYear === labelYear;
                        }
                        return itemPeriod === monthNumber;
                    } else {
                        return item.period_label == label;
                    }
                });
                
                return items.reduce((sum, item) => sum + (parseInt(item.failure_count) || 0), 0);
            });
            
            return {
                label: type,
                data: chartData,
                borderColor: colors[index % colors.length],
                backgroundColor: colors[index % colors.length] + '80',
                tension: 0.1,
                borderWidth: 2
            };
        });
    }
}

function getXAxisTitle(params) {
    if (params.week && params.week.length > 0 && !params.week.includes('all')) {
        return 'Дни месяца';
    } else if (params.month && params.month.length > 0 && !params.month.includes('all')) {
        // Проверяем, выбрано ли несколько месяцев или несколько годов
        const hasMultipleMonths = params.month.length > 1;
        const hasMultipleYears = params.year && params.year.length > 1 && !params.year.includes('all');
        
        // Если выбрано несколько месяцев или несколько годов, отображаются месяцы
        if (hasMultipleMonths || hasMultipleYears) {
            return 'Месяцы';
        } else {
            // Если выбран один месяц и один год, отображаются недели
            return 'Номера недель';
        }
    } else if (params.year && params.year.length > 0 && !params.year.includes('all')) {
        return 'Месяцы';
    } else {
        return 'Годы';
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

// Drill-down functions
function openModal() {
    document.getElementById('drillModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('drillModal').style.display = 'none';
    drillLevel = 0;
    currentFailureType = null;
    currentPeriod = null;
    currentYear = null;
    currentMachine = null;
    currentCause = null;
    if (drillChart) {
        drillChart.destroy();
        drillChart = null;
    }
}

async function openDrilldownEquipment(failureType, periodLabel, year) {
    currentFailureType = failureType;
    currentPeriod = periodLabel;
    currentYear = year;
    drillLevel = 1;
    
    const titleParts = [`Тип поломки: ${failureType}`, `Период: ${periodLabel}`];
    if (year) titleParts.push(`Год: ${year}`);
    document.getElementById('modalTitle').innerText = titleParts.join(' — ');
    document.getElementById('backBtn').style.display = 'none';
    openModal();
    
    // Преобразуем период в даты для запроса с учетом года
    const dateRange = getDateRangeFromPeriod(periodLabel, currentParams, year);
    
    const params = new URLSearchParams();
    if (dateRange.dateFrom) params.append('dateFrom', dateRange.dateFrom);
    if (dateRange.dateTo) params.append('dateTo', dateRange.dateTo);
    // Фильтруем по типу поломки - показываем оборудование с поломками этого типа
    params.append('failureType', failureType);
    
    // Добавляем фильтры из текущих параметров
    if (currentParams.area && !currentParams.area.includes('all')) {
        currentParams.area.forEach(area => params.append('area', area));
    }
    if (currentParams.equipment && !currentParams.equipment.includes('all')) {
        currentParams.equipment.forEach(equipment => params.append('equipment', equipment));
    }
    
    try {
        const data = await fetch('/top-equipment/data?' + params.toString()).then(r => r.json());
        renderDrillChart('Оборудование', data, item => {
            currentMachine = item.label;
            drillToCauses();
        });
    } catch (error) {
        console.error('Ошибка загрузки оборудования:', error);
        showError('Ошибка загрузки данных оборудования');
    }
}

async function drillToCauses() {
    drillLevel = 2;
    document.getElementById('backBtn').style.display = 'inline-block';
    
    // Используем период и год для запроса
    const dateRange = getDateRangeFromPeriod(currentPeriod, currentParams, currentYear);
    const params = new URLSearchParams();
    params.append('machine', currentMachine);
    if (dateRange.dateFrom) params.append('dateFrom', dateRange.dateFrom);
    if (dateRange.dateTo) params.append('dateTo', dateRange.dateTo);
    
    // Фильтруем по типу поломки - показываем только причины, относящиеся к запрашиваемому типу
    if (currentFailureType) {
        params.append('failureType', currentFailureType);
    }
    
    // Добавляем фильтры из текущих параметров
    if (currentParams.area && !currentParams.area.includes('all')) {
        currentParams.area.forEach(area => params.append('area', area));
    }
    
    try {
        // Получаем причины, отфильтрованные по типу поломки на сервере
        let data = await fetch('/top-equipment/drilldown/causes?' + params.toString()).then(r => r.json());
        
        // Дополнительная фильтрация на клиенте по failure_type (если поле есть в данных)
        if (currentFailureType && data.length > 0 && data[0].failure_type !== undefined) {
            const requestedType = currentFailureType.trim();
            data = data.filter(item => {
                const itemType = (item.failure_type || '').trim();
                return itemType === requestedType;
            });
        }
        
        // Фильтруем причины, оставляя только те, у которых cause не пустой
        const validCauses = data.filter(item => {
            const cause = (item.cause || item.label || '').trim();
            return cause !== '' && cause !== null && cause !== undefined;
        });
        
        // Если нет валидных причин (все пустые), пропускаем уровни причин и механизмов, переходим сразу к событиям
        if (validCauses.length === 0) {
            console.log('No valid causes found, skipping to events directly');
            currentCause = ''; // Устанавливаем пустую строку для cause
            // Переходим сразу к событиям для данного оборудования с фильтром по типу поломки
            drillToEventsDirectly();
            return;
        }
        
        // Если есть валидные причины, показываем их
        renderDrillChart('Причины', validCauses, item => {
            currentCause = item.label;
            drillToMechanisms();
        });
    } catch (error) {
        console.error('Ошибка загрузки причин:', error);
        showError('Ошибка загрузки данных причин');
    }
}

async function drillToMechanisms() {
    drillLevel = 3;
    document.getElementById('backBtn').style.display = 'inline-block';
    
    // Если и тип, и cause пустые - переходим сразу к событиям
    if ((!currentFailureType || currentFailureType.trim() === '') && 
        (!currentCause || currentCause.trim() === '')) {
        console.log('Both failureType and cause are empty, skipping to events');
        drillToEvents('all');
        return;
    }
    
    // Используем период и год для запроса
    const dateRange = getDateRangeFromPeriod(currentPeriod, currentParams, currentYear);
    const params = new URLSearchParams();
    params.append('machine', currentMachine);
    
    // Если cause пустой, используем пустую строку (для SQL это будет TRIM(cause) = '')
    // SQL запрос использует TRIM(cause) = ?, поэтому пустая строка будет искать записи с пустым cause
    params.append('cause', currentCause || '');
    
    if (dateRange.dateFrom) params.append('dateFrom', dateRange.dateFrom);
    if (dateRange.dateTo) params.append('dateTo', dateRange.dateTo);
    
    if (currentParams.area && !currentParams.area.includes('all')) {
        currentParams.area.forEach(area => params.append('area', area));
    }
    
    try {
        // Получаем механизмы для данного оборудования и причины (или без причины, если cause пустой)
        let data = await fetch('/top-equipment/drilldown/mechanisms?' + params.toString()).then(r => r.json());
        
        // Фильтруем по типу поломки на клиенте (если поле есть в данных)
        // Механизмы должны относиться к запрашиваемому типу поломки
        if (currentFailureType && currentFailureType.trim() !== '' && data.length > 0) {
            // Если в данных есть поле failure_type, фильтруем по нему
            if (data[0].failure_type !== undefined) {
                const requestedType = currentFailureType.trim();
                data = data.filter(item => {
                    const itemType = (item.failure_type || '').trim();
                    return itemType === requestedType;
                });
            }
            // Если поля нет, но есть тип - данные уже должны быть отфильтрованы на сервере
        }
        
        // Фильтруем механизмы, оставляя только валидные (не пустые и не "Не указан")
        const validMechanisms = data.filter(item => {
            const mechanism = (item.mechanism_node || item.label || '').trim();
            return mechanism !== '' && mechanism !== null && mechanism !== undefined && mechanism !== 'Не указан';
        });
        
        // Если нет валидных механизмов, переходим к событиям
        if (validMechanisms.length === 0) {
            console.log('No valid mechanisms found, skipping to events');
            drillToEvents('all');
            return;
        }
        
        renderDrillChart('Узлы/механизмы', validMechanisms, item => drillToEvents(item.label));
    } catch (error) {
        console.error('Ошибка загрузки механизмов:', error);
        showError('Ошибка загрузки данных механизмов');
    }
}

// Функция для прямого запроса событий для оборудования без фильтрации по cause и mechanism
async function drillToEventsDirectly() {
    drillLevel = 4;
    document.getElementById('backBtn').style.display = 'inline-block';
    
    // Используем период и год для запроса
    const dateRange = getDateRangeFromPeriod(currentPeriod, currentParams, currentYear);
    
    try {
        console.log('Getting all events directly for:', { machine: currentMachine, failureType: currentFailureType, dateRange });
        
        // Используем новый endpoint для получения всех событий для оборудования
        const params = new URLSearchParams();
        params.append('machine', currentMachine);
        if (dateRange.dateFrom) params.append('dateFrom', dateRange.dateFrom);
        if (dateRange.dateTo) params.append('dateTo', dateRange.dateTo);
        if (currentFailureType && currentFailureType.trim() !== '') {
            params.append('failureType', currentFailureType);
        }
        if (currentParams.area && !currentParams.area.includes('all')) {
            currentParams.area.forEach(area => params.append('area', area));
        }
        
        // Используем новый endpoint /drilldown/all-events
        const events = await fetch('/top-equipment/drilldown/all-events?' + params.toString()).then(r => r.json());
        
        console.log('Events received:', events.length);
        
        // Сортируем по дате (новые сверху)
        events.sort((a, b) => {
            const dateA = new Date(a.start_bd_t1 || 0);
            const dateB = new Date(b.start_bd_t1 || 0);
            return dateB - dateA;
        });
        
        const titleParts = ['События'];
        if (currentFailureType) titleParts.push(currentFailureType);
        titleParts.push(currentMachine || '');
        titleParts.push(currentPeriod || '');
        document.getElementById('modalTitle').innerText = titleParts.join(' — ');
        
        console.log('Final events to display:', events.length);
        renderEventsTable('События', events);
    } catch (error) {
        console.error('Ошибка загрузки событий:', error);
        showError('Ошибка загрузки данных событий: ' + error.message);
    }
}

async function drillToEvents(mechanism) {
    drillLevel = 4;
    document.getElementById('backBtn').style.display = 'inline-block';
    
    // Используем период и год для запроса
    const dateRange = getDateRangeFromPeriod(currentPeriod, currentParams, currentYear);
    const params = new URLSearchParams();
    params.append('machine', currentMachine);
    
    // Если mechanism = 'all', это означает, что мы пропустили уровень механизмов
    // В этом случае используем пустую строку для cause (если он пустой) и не передаем конкретный механизм
    // Но бэкенд требует обязательный параметр mechanism, поэтому передаем пустую строку
    // SQL запрос использует COALESCE(TRIM(mechanism_node), 'Не указан'), поэтому пустая строка будет искать все
    params.append('cause', currentCause || '');
    
    // Если mechanism = 'all', не добавляем фильтр по механизму (используем пустую строку)
    // Но лучше передать пустую строку, чтобы бэкенд не ругался
    if (mechanism === 'all') {
        // Для 'all' передаем пустую строку - бэкенд может обработать это как "все механизмы"
        // Или можно использовать специальное значение, но пока используем пустую строку
        params.append('mechanism', '');
    } else {
        params.append('mechanism', mechanism);
    }
    
    if (dateRange.dateFrom) params.append('dateFrom', dateRange.dateFrom);
    if (dateRange.dateTo) params.append('dateTo', dateRange.dateTo);
    
    if (currentParams.area && !currentParams.area.includes('all')) {
        currentParams.area.forEach(area => params.append('area', area));
    }
    
    try {
        // Получаем события для данного оборудования, причины и механизма (или все, если mechanism = 'all')
        let events = await fetch('/top-equipment/drilldown/events?' + params.toString()).then(r => r.json());
        
        // Фильтруем по типу поломки на клиенте
        // События должны относиться к запрашиваемому типу поломки
        if (currentFailureType && currentFailureType.trim() !== '' && events.length > 0) {
            // Если в данных есть поле failure_type, фильтруем по нему
            if (events[0].failure_type !== undefined) {
                const requestedType = currentFailureType.trim();
                events = events.filter(event => {
                    const eventType = (event.failure_type || '').trim();
                    return eventType === requestedType;
                });
            }
            // Если поля нет, данные уже отфильтрованы по причине и механизму
        }
        
        // Формируем заголовок для таблицы событий
        let eventTitle = 'События';
        if (mechanism && mechanism !== 'all') {
            eventTitle = `События: ${mechanism}`;
        } else if (!currentCause || currentCause.trim() === '') {
            eventTitle = 'События (без указания причины)';
        } else {
            eventTitle = `События: ${currentCause}`;
        }
        
        renderEventsTable(eventTitle, events);
    } catch (error) {
        console.error('Ошибка загрузки событий:', error);
        showError('Ошибка загрузки данных событий');
    }
}

function renderDrillChart(title, rows, onBarClick) {
    const ctxEl = ensureDrillCanvas();
    if (drillChart) drillChart.destroy();
    
    // Определяем формат данных в зависимости от уровня drill-down
    let labels, downtimeValues, countValues;
    
    if (drillLevel === 1) {
        // Оборудование - два показателя
        labels = rows.map(r => r.machine_name || r.equipment_name || r.label || 'Неизвестно');
        downtimeValues = rows.map(r => Number(r.total_downtime_hours || 0) * 3600); // Конвертируем в секунды
        countValues = rows.map(r => Number(r.failure_count || r.incident_count || 0));
        
        document.getElementById('modalTitle').innerText = `${title} — ${currentFailureType || ''} / ${currentPeriod || ''}`;
        
        drillChart = new Chart(ctxEl, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    { 
                        label: 'Время простоя, ч', 
                        data: downtimeValues.map(v => v / 3600), // Конвертируем обратно в часы для отображения
                        backgroundColor: 'rgba(231,76,60,0.6)' 
                    },
                    { 
                        label: 'Кол-во вызовов', 
                        data: countValues, 
                        backgroundColor: 'rgba(52,152,219,0.6)' 
                    }
                ]
            },
            options: {
                indexAxis: 'y',
                maintainAspectRatio: false,
                onClick: (_e, els) => {
                    if (els?.length) {
                        const i = els[0].index;
                        onBarClick({ label: labels[i], value: downtimeValues[i] });
                    }
                },
                plugins: { legend: { display: true } },
                scales: { x: { beginAtZero: true } }
            }
        });
    } else {
        // Причины или механизмы - один показатель
        labels = rows.map(r => r.cause || r.mechanism_node || r.label);
        downtimeValues = rows.map(r => Number(r.total_downtime_hours || 0));
        
        const titleParts = [title];
        if (currentFailureType) titleParts.push(currentFailureType);
        if (currentMachine) titleParts.push(currentMachine);
        if (currentCause && drillLevel > 2) titleParts.push(currentCause);
        document.getElementById('modalTitle').innerText = titleParts.join(' — ');
        
        drillChart = new Chart(ctxEl, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Время простоя, ч',
                    data: downtimeValues,
                    backgroundColor: 'rgba(231,76,60,0.6)'
                }]
            },
            options: {
                indexAxis: 'y',
                maintainAspectRatio: false,
                onClick: (_e, els) => {
                    if (els?.length) {
                        const i = els[0].index;
                        onBarClick({ label: labels[i], value: downtimeValues[i] });
                    }
                },
                plugins: { legend: { display: false } },
                scales: { x: { beginAtZero: true } }
            }
        });
    }
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

function formatDateTime(dateString) {
    if (!dateString) return '';
    
    try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return dateString; // Если дата невалидна, возвращаем исходную строку
        
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        
        return `${day}.${month}.${year} ${hours}:${minutes}:${seconds}`;
    } catch (error) {
        return dateString; // В случае ошибки возвращаем исходную строку
    }
}

function renderEventsTable(title, events) {
    const container = document.querySelector('.modal-body');
    if (drillChart) {
        drillChart.destroy();
        drillChart = null;
    }
    
    const titleParts = [title];
    if (currentFailureType) titleParts.push(currentFailureType);
    if (currentMachine) titleParts.push(currentMachine);
    if (currentCause) titleParts.push(currentCause);
    document.getElementById('modalTitle').innerText = titleParts.join(' — ');
    
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
                                <td style="padding:8px; white-space:nowrap;">${formatDateTime(e.start_bd_t1)}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>`;
}

function getDateRangeFromPeriod(periodLabel, params, explicitYear = null) {
    // Преобразуем период в диапазон дат
    // periodLabel может быть: номер недели, название месяца, год и т.д.
    const dateRange = { dateFrom: null, dateTo: null };
    
    console.log('getDateRangeFromPeriod called with:', { periodLabel, params, explicitYear });
    
    // Определяем год - приоритет у явно переданного года из метки графика
    let year = null;
    if (explicitYear) {
        year = explicitYear;
    } else if (params.year && params.year.length > 0 && !params.year.includes('all')) {
        year = parseInt(params.year[0]); // Берем первый выбранный год
    } else {
        year = new Date().getFullYear();
    }
    
    // Определяем месяц из параметров
    let month = null;
    if (params.month && params.month.length > 0 && !params.month.includes('all')) {
        const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
        const monthIndex = monthNames.indexOf(params.month[0]);
        if (monthIndex >= 0) {
            month = monthIndex + 1;
        } else {
            // Возможно, это уже номер месяца
            month = parseInt(params.month[0]);
        }
        console.log(`Month determined from params: ${month} (${params.month[0]})`);
    }
    
    // Определяем период из метки
    const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
    const monthIndex = monthAbbr.indexOf(periodLabel);
    
    // Если выбраны недели, но не выбран месяц, определяем месяц из выбранных недель
    const hasWeeksSelected = params.week && params.week.length > 0 && !params.week.includes('all');
    if (hasWeeksSelected && !month) {
        // Определяем месяц из первой выбранной недели
        // Используем более точную формулу: находим первый понедельник года и вычисляем дату недели
        const firstWeek = parseInt(params.week[0]);
        if (!isNaN(firstWeek)) {
            // Находим первый понедельник года (ISO недели начинаются с понедельника)
            const jan1 = new Date(year, 0, 1);
            const jan1Day = jan1.getDay(); // 0 = воскресенье, 1 = понедельник, ...
            // Вычисляем день недели для 1 января (0 = воскресенье)
            // Для ISO недель (mode 1): неделя 1 - это первая неделя с 4+ днями в новом году
            // Упрощенный расчет: находим первый понедельник
            let daysToMonday = (8 - jan1Day) % 7; // Дней до первого понедельника
            if (daysToMonday === 0 && jan1Day !== 1) daysToMonday = 7;
            if (jan1Day === 0) daysToMonday = 1; // Если 1 января - воскресенье
            
            // Дата начала первой недели
            const week1Start = new Date(year, 0, 1 + daysToMonday);
            
            // Дата начала нужной недели
            const daysFromWeek1 = (firstWeek - 1) * 7;
            const weekStart = new Date(week1Start);
            weekStart.setDate(week1Start.getDate() + daysFromWeek1);
            
            month = weekStart.getMonth() + 1;
            console.log(`Determined month ${month} from week ${firstWeek} in year ${year} (week starts: ${weekStart.toISOString().split('T')[0]})`);
        }
    }
    
    if (monthIndex >= 0) {
        // Это месяц в сокращенном формате
        month = monthIndex + 1;
        dateRange.dateFrom = `${year}-${String(month).padStart(2, '0')}-01`;
        const lastDay = new Date(year, month, 0).getDate();
        dateRange.dateTo = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
    } else if (!isNaN(parseInt(periodLabel))) {
        // Это номер недели или день месяца
        const periodNumber = parseInt(periodLabel);
        
        // Если выбраны недели в фильтрах, то periodLabel - это день месяца (не номер недели)
        // Когда выбраны недели, график показывает дни месяца, а не номера недель
        const hasWeeksSelected = params.week && params.week.length > 0 && !params.week.includes('all');
        const hasMonthsSelected = params.month && params.month.length > 0 && !params.month.includes('all');
        
        if (hasWeeksSelected && hasMonthsSelected) {
            // Выбраны и недели, и месяцы - periodLabel это день месяца (DAY из SQL)
            // Месяц должен быть уже определен из params.month выше
            if (!month) {
                console.warn('Month not determined from params, using current month');
                month = new Date().getMonth() + 1;
            }
            dateRange.dateFrom = `${year}-${String(month).padStart(2, '0')}-${String(periodNumber).padStart(2, '0')}`;
            dateRange.dateTo = dateRange.dateFrom;
            console.log(`Date range for week+month selection: ${dateRange.dateFrom} (day ${periodNumber} of month ${month}, year ${year})`);
        } else if (hasWeeksSelected && !hasMonthsSelected) {
            // Выбраны только недели (без месяцев) - periodLabel это день месяца
            // Но по SQL запросу, когда выбраны недели, обычно выбирается и месяц
            // Если месяц не выбран, определяем его из недели (уже сделано выше)
            if (!month) {
                // Если месяц все еще не определен, используем текущий месяц
                month = new Date().getMonth() + 1;
            }
            dateRange.dateFrom = `${year}-${String(month).padStart(2, '0')}-${String(periodNumber).padStart(2, '0')}`;
            dateRange.dateTo = dateRange.dateFrom;
            console.log(`Date range for week selection: ${dateRange.dateFrom} (day ${periodNumber} of month ${month})`);
        } else if (!hasWeeksSelected && periodNumber <= 31) {
            // Не выбраны недели, и число <= 31 - это день месяца
            if (!month) month = new Date().getMonth() + 1; // Используем текущий месяц если не указан
            dateRange.dateFrom = `${year}-${String(month).padStart(2, '0')}-${String(periodNumber).padStart(2, '0')}`;
            dateRange.dateTo = dateRange.dateFrom;
        } else {
            // Это может быть номер недели (если число > 31 или нет контекста месяца)
            // Но обычно это не используется, так как недели отображаются как дни
            if (!month) month = new Date().getMonth() + 1;
            const firstDay = new Date(year, month - 1, 1);
            const weekStart = new Date(firstDay);
            weekStart.setDate(firstDay.getDate() + (periodNumber - 1) * 7);
            dateRange.dateFrom = formatDateForAPI(weekStart);
            const weekEnd = new Date(weekStart);
            weekEnd.setDate(weekStart.getDate() + 6);
            dateRange.dateTo = formatDateForAPI(weekEnd);
        }
    } else {
        // Используем весь год или выбранный месяц
        if (month) {
            dateRange.dateFrom = `${year}-${String(month).padStart(2, '0')}-01`;
            const lastDay = new Date(year, month, 0).getDate();
            dateRange.dateTo = `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
        } else {
            dateRange.dateFrom = `${year}-01-01`;
            dateRange.dateTo = `${year}-12-31`;
        }
    }
    
    return dateRange;
}

function formatDateForAPI(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}


