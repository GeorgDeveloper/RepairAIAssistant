let mainChart = null;
let chartType = 'line';
let chartData = null;

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadInitialData();
});

function setupEventListeners() {
    // Настройка single-select для года
    setupSingleSelect('year', function() {
        loadMonths();
    });
    
    // Настройка single-select для месяца
    setupSingleSelect('month', function() {
        // При изменении месяца обновляем оборудование если выбран участок
        const area = getSelectedArea();
        if (area !== 'all') {
            loadEquipment(area);
        }
    });
    
    // Настройка single-select для участка
    setupSingleSelect('area', function() {
        const area = getSelectedArea();
        loadEquipment(area);
    });
    
    // Настройка single-select для оборудования
    setupSingleSelect('equipment');
    
    document.getElementById('apply-filters').addEventListener('click', function() {
        applyFilters();
    });
    
    document.getElementById('toggleChartType').addEventListener('click', function() {
        toggleChartType();
    });
    
    document.getElementById('toggleAllLegend').addEventListener('click', function() {
        toggleAllLegendItems();
    });
}

function setupSingleSelect(prefix, onChangeCallback) {
    const button = document.getElementById(prefix + '-button');
    const dropdown = document.getElementById(prefix + '-dropdown');
    const text = document.getElementById(prefix + '-text');
    
    // Обработчик клика по кнопке
    button.addEventListener('click', function(e) {
        e.stopPropagation();
        toggleSingleDropdown(prefix);
    });
    
    // Обработчики для radio buttons
    dropdown.addEventListener('change', function(e) {
        if (e.target.type === 'radio') {
            updateSingleButtonText(prefix);
            if (onChangeCallback) onChangeCallback();
            closeSingleDropdown(prefix);
        }
    });
    
    // Закрытие при клике вне элемента
    document.addEventListener('click', function(e) {
        if (!e.target.closest('#' + prefix + '-singleselect')) {
            closeSingleDropdown(prefix);
        }
    });
}

function toggleSingleDropdown(prefix) {
    const button = document.getElementById(prefix + '-button');
    const dropdown = document.getElementById(prefix + '-dropdown');
    
    // Закрыть все остальные выпадающие списки
    document.querySelectorAll('.single-select-dropdown.open').forEach(dd => {
        if (dd.id !== prefix + '-dropdown') {
            dd.classList.remove('open');
            const btn = dd.previousElementSibling;
            if (btn) btn.classList.remove('open');
        }
    });
    
    dropdown.classList.toggle('open');
    button.classList.toggle('open');
}

function closeSingleDropdown(prefix) {
    const button = document.getElementById(prefix + '-button');
    const dropdown = document.getElementById(prefix + '-dropdown');
    dropdown.classList.remove('open');
    button.classList.remove('open');
}

function updateSingleButtonText(prefix) {
    const text = document.getElementById(prefix + '-text');
    const dropdown = document.getElementById(prefix + '-dropdown');
    const selectedRadio = dropdown.querySelector('input[type="radio"]:checked');
    
    if (selectedRadio) {
        text.textContent = selectedRadio.nextElementSibling.textContent;
    }
}

function getSelectedYear() {
    const selectedRadio = document.querySelector('#year-dropdown input[type="radio"]:checked');
    return selectedRadio ? selectedRadio.value : 'all';
}

function getSelectedMonth() {
    const selectedRadio = document.querySelector('#month-dropdown input[type="radio"]:checked');
    return selectedRadio ? selectedRadio.value : 'all';
}

function getSelectedArea() {
    const selectedRadio = document.querySelector('#area-dropdown input[type="radio"]:checked');
    return selectedRadio ? selectedRadio.value : 'all';
}

function getSelectedEquipment() {
    const selectedRadio = document.querySelector('#equipment-dropdown input[type="radio"]:checked');
    return selectedRadio ? selectedRadio.value : 'all';
}

function populateSingleSelect(prefix, data, valueField, displayField, allText) {
    const dropdown = document.getElementById(prefix + '-dropdown');
    const allRadio = document.getElementById(prefix + '-all');
    
    // Очищаем все опции кроме "Все"
    const existingOptions = dropdown.querySelectorAll('.single-select-option:not(:first-child)');
    existingOptions.forEach(option => option.remove());
    
    // Удаляем дубликаты
    const seen = new Set();
    const uniqueData = [];
    
    data.forEach(item => {
        const value = item[valueField];
        if (value == null) return;
        
        const normalizedValue = String(value).trim();
        if (normalizedValue === '') return;
        
        const key = normalizedValue.toLowerCase();
        
        if (!seen.has(key)) {
            seen.add(key);
            uniqueData.push({
                ...item,
                [valueField]: normalizedValue
            });
        }
    });
    
    // Добавляем новые опции
    uniqueData.forEach(item => {
        const option = document.createElement('div');
        option.className = 'single-select-option';
        const value = item[valueField];
        const displayText = displayField ? item[displayField] : value;
        const safeId = `${prefix}-${value}`.replace(/[^a-zA-Z0-9-_]/g, '_');
        
        option.innerHTML = `
            <input type="radio" name="${prefix}" id="${safeId}" value="${value}">
            <label for="${safeId}">${displayText}</label>
        `;
        dropdown.appendChild(option);
    });
    
    // Обновляем текст кнопки
    updateSingleButtonText(prefix);
}

async function loadInitialData() {
    showLoading(true);
    try {
        await Promise.all([
            loadYears(),
            loadAreas()
        ]);
        
        // Устанавливаем текущий месяц по умолчанию
        const currentDate = new Date();
        const currentYear = currentDate.getFullYear().toString();
        const currentMonth = (currentDate.getMonth() + 1).toString();
        
        // Выбираем текущий год
        const yearRadio = document.getElementById(`year-${currentYear}`.replace(/[^a-zA-Z0-9-_]/g, '_'));
        if (yearRadio) {
            document.getElementById('year-all').checked = false;
            yearRadio.checked = true;
            updateSingleButtonText('year');
        }
        
        await loadMonths();
        
        // Выбираем текущий месяц
        const monthRadio = document.getElementById(`month-${currentMonth}`.replace(/[^a-zA-Z0-9-_]/g, '_'));
        if (monthRadio) {
            document.getElementById('month-all').checked = false;
            monthRadio.checked = true;
            updateSingleButtonText('month');
        }
        
        await applyFilters();
    } catch (error) {
        showError('Ошибка загрузки данных: ' + error.message);
    } finally {
        showLoading(false);
    }
}

function parseDate(dateStr) {
    // Формат даты: "DD.MM.YYYY"
    const parts = dateStr.split('.');
    if (parts.length !== 3) return null;
    return new Date(parseInt(parts[2]), parseInt(parts[1]) - 1, parseInt(parts[0]));
}

async function loadYears() {
    try {
        const response = await fetch('/dashboard/pm-plan-fact-tag-all');
        const data = await response.json();
        
        const years = new Set();
        data.forEach(item => {
            const date = parseDate(item.production_day);
            if (date && !isNaN(date.getTime())) {
                years.add(date.getFullYear());
            }
        });
        
        const yearsArray = Array.from(years).sort((a, b) => b - a).map(year => ({ year: year.toString() }));
        populateSingleSelect('year', yearsArray, 'year', 'year', 'Все');
    } catch (error) {
        console.error('Ошибка загрузки годов:', error);
    }
}

async function loadMonths() {
    const year = getSelectedYear();
    if (year === 'all') {
        const dropdown = document.getElementById('month-dropdown');
        const existingOptions = dropdown.querySelectorAll('.single-select-option:not(:first-child)');
        existingOptions.forEach(option => option.remove());
        document.getElementById('month-all').checked = true;
        updateSingleButtonText('month');
        return;
    }
    
    try {
        const response = await fetch('/dashboard/pm-plan-fact-tag-all');
        const data = await response.json();
        
        const months = new Set();
        data.forEach(item => {
            const date = parseDate(item.production_day);
            if (date && !isNaN(date.getTime()) && date.getFullYear().toString() === year) {
                months.add(date.getMonth() + 1);
            }
        });
        
        const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
        const monthsWithNames = Array.from(months).sort((a, b) => a - b).map(month => ({
            value: month.toString(),
            label: monthNames[month - 1]
        }));
        
        populateSingleSelect('month', monthsWithNames, 'value', 'label', 'Все');
    } catch (error) {
        console.error('Ошибка загрузки месяцев:', error);
    }
}

async function loadAreas() {
    try {
        const response = await fetch('/dashboard/pm/areas');
        const areas = await response.json();
        
        populateSingleSelect('area', areas, 'area', 'area', 'Все участки');
    } catch (error) {
        console.error('Ошибка загрузки участков:', error);
    }
}

async function loadEquipment(area) {
    try {
        let url = '/dashboard/pm/equipment';
        if (area && area !== 'all') {
            url += `?area=${encodeURIComponent(area)}`;
        }
        
        const response = await fetch(url);
        const equipment = await response.json();
        
        populateSingleSelect('equipment', equipment, 'machine_name', 'machine_name', 'Всё оборудование');
    } catch (error) {
        console.error('Ошибка загрузки оборудования:', error);
    }
}

async function applyFilters() {
    showLoading(true);
    try {
        const year = getSelectedYear();
        const month = getSelectedMonth();
        const area = getSelectedArea();
        const equipment = getSelectedEquipment();
        
        // Строим URL с параметрами
        let url = '/dashboard/pm-plan-fact-tag-all';
        const params = [];
        
        if (area !== 'all') {
            params.push(`area=${encodeURIComponent(area)}`);
        }
        
        if (equipment !== 'all') {
            params.push(`machineName=${encodeURIComponent(equipment)}`);
        }
        
        if (params.length > 0) {
            url += '?' + params.join('&');
        }
        
        const response = await fetch(url);
        const allData = await response.json();
        
        const filteredData = filterPmDataByDate(allData, year, month);
        generateChart(filteredData);
    } catch (error) {
        console.error('Ошибка применения фильтров:', error);
        showError('Ошибка применения фильтров: ' + error.message);
    } finally {
        showLoading(false);
    }
}

function filterPmDataByDate(data, year, month) {
    return data.filter(item => {
        const date = parseDate(item.production_day);
        if (!date || isNaN(date.getTime())) return false;
        
        // Фильтр по году
        if (year !== 'all' && date.getFullYear().toString() !== year) {
            return false;
        }
        
        // Фильтр по месяцу
        if (month !== 'all' && (date.getMonth() + 1).toString() !== month) {
            return false;
        }
        
        return true;
    });
}

function generateChart(data) {
    if (!data || data.length === 0) {
        showError('Нет данных для отображения');
        return;
    }
    
    // Сортируем данные по дате
    data.sort((a, b) => {
        const dateA = parseDate(a.production_day);
        const dateB = parseDate(b.production_day);
        return dateA - dateB;
    });
    
    const labels = data.map(item => item.production_day);
    
    chartData = {
        labels: labels,
        datasets: [
            {
                label: 'План',
                data: data.map(item => Number(item.plan) || 0),
                borderColor: '#e31a1c',
                backgroundColor: '#e31a1c80',
                tension: 0.1,
                borderWidth: 2
            },
            {
                label: 'Факт',
                data: data.map(item => Number(item.fact) || 0),
                borderColor: '#33a02c',
                backgroundColor: '#33a02c80',
                tension: 0.1,
                borderWidth: 2
            },
            {
                label: 'Tag',
                data: data.map(item => Number(item.tag) || 0),
                borderColor: '#1f78b4',
                backgroundColor: '#1f78b480',
                tension: 0.1,
                borderWidth: 2
            }
        ]
    };
    
    createMainChart();
    createCustomLegend(chartData.datasets);
}

function createMainChart() {
    const ctx = document.getElementById('mainChart').getContext('2d');
    
    if (mainChart) {
        mainChart.destroy();
    }
    
    const totalPlan = chartData.datasets[0].data.reduce((a, b) => a + b, 0);
    const totalFact = chartData.datasets[1].data.reduce((a, b) => a + b, 0);
    const completion = totalPlan > 0 ? Math.round((totalFact / totalPlan) * 100) : 0;
    
    mainChart = new Chart(ctx, {
        type: chartType,
        data: chartData,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: `PM: План / Факт / Tag (Выполнение: ${completion}%)`
                },
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    stacked: chartType === 'bar',
                    title: {
                        display: true,
                        text: 'Количество'
                    }
                },
                x: {
                    stacked: chartType === 'bar',
                    title: {
                        display: true,
                        text: 'Дата'
                    },
                    ticks: {
                        maxRotation: 45,
                        minRotation: 45
                    }
                }
            }
        }
    });
}

function createCustomLegend(datasets) {
    const legendContainer = document.getElementById('customLegend');
    legendContainer.innerHTML = '';
    
    datasets.forEach(dataset => {
        const legendItem = document.createElement('div');
        legendItem.style.cssText = 'display: flex; align-items: center; margin-bottom: 8px; cursor: pointer;';
        legendItem.dataset.label = dataset.label;
        
        const colorBox = document.createElement('div');
        colorBox.style.cssText = `width: 20px; height: 3px; background-color: ${dataset.borderColor}; margin-right: 8px; border-radius: 2px;`;
        
        const label = document.createElement('span');
        label.textContent = dataset.label;
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

function toggleChartType() {
    chartType = chartType === 'line' ? 'bar' : 'line';
    const button = document.getElementById('toggleChartType');
    button.textContent = chartType === 'line' ? 'Накопительная' : 'Линейная';
    
    if (chartData) {
        createMainChart();
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
