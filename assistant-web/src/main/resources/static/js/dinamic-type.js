let mainChart = null;
let currentMode = 'downtime';
let chartType = 'line';
let chartData = null;
let showLabels = false;

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadInitialData();
});

function setupEventListeners() {
    // Настройка множественного выбора для года
    setupMultiSelect('year', function() {
        loadMonths();
    });
    
    // Настройка множественного выбора для месяца
    setupMultiSelect('month', function() {
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
        loadWeeks();
    } catch (error) {
        console.error('Ошибка загрузки месяцев:', error);
    }
}

async function loadWeeks() {
    const years = getSelectedValues('year');
    const months = getSelectedValues('month');
    
    if (years.includes('all') || months.includes('all')) {
        const dropdown = document.getElementById('week-dropdown');
        const existingOptions = dropdown.querySelectorAll('.multi-select-option:not(:first-child)');
        existingOptions.forEach(option => option.remove());
        updateButtonText('week');
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
            }
        }
    });
}

function createCustomLegend(datasets, rawData) {
    const legendContainer = document.getElementById('customLegend');
    legendContainer.innerHTML = '';
    
    // Создаем объект для хранения данных по каждому типу поломки
    const failureTypeData = {};
    
    // Собираем данные из rawData для отображения в легенде
    rawData.forEach(item => {
        const type = item.failure_type?.trim();
        if (!type) return;
        
        if (!failureTypeData[type]) {
            failureTypeData[type] = {
                totalDowntime: 0,
                totalCount: 0
            };
        }
        
        const downtimeSeconds = item.total_downtime_seconds ? parseFloat(item.total_downtime_seconds) : 0;
        const count = item.failure_count ? parseInt(item.failure_count) : 0;
        
        failureTypeData[type].totalDowntime += downtimeSeconds;
        failureTypeData[type].totalCount += count;
    });
    
    datasets.forEach(dataset => {
        const legendItem = document.createElement('div');
        legendItem.style.cssText = 'display: flex; align-items: center; margin-bottom: 8px; cursor: pointer;';
        legendItem.dataset.label = dataset.label;
        
        const colorBox = document.createElement('div');
        colorBox.style.cssText = `width: 20px; height: 3px; background-color: ${dataset.borderColor}; margin-right: 8px; border-radius: 2px;`;
        
        const label = document.createElement('span');
        const typeData = failureTypeData[dataset.label] || { totalDowntime: 0, totalCount: 0 };
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
    // Если выбран год и не выбран месяц, группируем по месяцам
    if (params.year && params.year.length > 0 && !params.year.includes('all') &&
        (!params.month || params.month.includes('all'))) {
        const uniqueMonths = [...new Set(data.map(item => parseInt(item.period_label)))].sort((a, b) => a - b);
        const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
        return uniqueMonths.map(month => monthAbbr[month - 1]);
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
    
    return uniqueTypes.map((type, index) => {
        const chartData = labels.map((label, labelIndex) => {
            // Определяем номер месяца для поиска
            let monthNumber = null;
            if (label.length <= 3 && ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'].includes(label)) {
                const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
                monthNumber = monthAbbr.indexOf(label) + 1;
            }
            
            const items = data.filter(item => {
                const itemType = item.failure_type?.trim();
                const itemPeriod = parseInt(item.period_label);
                
                if (itemType !== type) return false;
                
                if (monthNumber !== null) {
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

function generateCountDatasets(data, labels) {
    const uniqueTypes = [...new Set(data.map(item => item.failure_type?.trim()).filter(type => type))].sort();
    const colors = [
        '#4A90E2', '#F5A623', '#7ED321', '#BD10E0', '#50E3C2', '#B8E986', '#9013FE', '#D0021B',
        '#417505', '#7B68EE', '#FF6347', '#32CD32', '#FFD700', '#FF69B4', '#00CED1', '#FF4500'
    ];
    
    return uniqueTypes.map((type, index) => {
        const chartData = labels.map((label, labelIndex) => {
            // Определяем номер месяца для поиска
            let monthNumber = null;
            if (label.length <= 3 && ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'].includes(label)) {
                const monthAbbr = ['янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];
                monthNumber = monthAbbr.indexOf(label) + 1;
            }
            
            const items = data.filter(item => {
                const itemType = item.failure_type?.trim();
                const itemPeriod = parseInt(item.period_label);
                
                if (itemType !== type) return false;
                
                if (monthNumber !== null) {
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

function getXAxisTitle(params) {
    if (params.week && params.week.length > 0 && !params.week.includes('all')) {
        return 'Дни месяца';
    } else if (params.month && params.month.length > 0 && !params.month.includes('all')) {
        return 'Номера недель';
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

