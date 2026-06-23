let mainChart = null;
let chartType = 'line';
let chartData = null;

console.log('=== СКРИПТ dinamic-diagnostics.js ЗАГРУЖЕН ===');

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM загружен, инициализация...');
    setupEventListeners();
    loadInitialData();
});

function setupEventListeners() {
    // Настройка single-select для года
    setupSingleSelect('year', function() {
        loadMonths();
    });
    
    // Настройка single-select для месяца
    setupSingleSelect('month');
    
    // Настройка single-select для участка
    setupSingleSelect('area', function() {
        const area = getSelectedArea();
        loadEquipment(area);
    });
    
    // Настройка single-select для оборудования
    setupSingleSelect('equipment');
    
    // Настройка single-select для типа диагностики
    setupSingleSelect('diagnostics-type');
    
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

function getSelectedDiagnosticsType() {
    // Ищем по name="diagnosticsType" или в dropdown
    let selectedRadio = document.querySelector('#diagnostics-type-dropdown input[name="diagnosticsType"]:checked');
    if (!selectedRadio) {
        selectedRadio = document.querySelector('#diagnostics-type-dropdown input[type="radio"]:checked');
    }
    const value = selectedRadio ? selectedRadio.value : 'all';
    console.log('Выбранный тип диагностики:', value, 'Radio:', selectedRadio);
    if (!selectedRadio) {
        console.warn('Не найден выбранный radio button для типа диагностики');
        const allRadios = document.querySelectorAll('#diagnostics-type-dropdown input[type="radio"]');
        console.log('Всего radio buttons в dropdown:', allRadios.length);
        allRadios.forEach((radio, index) => {
            console.log(`Radio ${index}:`, radio.name, radio.value, radio.checked);
        });
    }
    return value;
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
        
        // Для типа диагностики используем правильное имя
        const radioName = prefix === 'diagnostics-type' ? 'diagnosticsType' : prefix;
        option.innerHTML = `
            <input type="radio" name="${radioName}" id="${safeId}" value="${value}">
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
            loadAreas(),
            loadDiagnosticsTypes()
        ]);
        
        // Устанавливаем текущий год по умолчанию
        const currentDate = new Date();
        const currentYear = currentDate.getFullYear().toString();
        
        // Выбираем текущий год
        const yearRadio = document.getElementById(`year-${currentYear}`.replace(/[^a-zA-Z0-9-_]/g, '_'));
        if (yearRadio) {
            document.getElementById('year-all').checked = false;
            yearRadio.checked = true;
            updateSingleButtonText('year');
        }
        
        await loadMonths();
        
        // Выбираем текущий месяц
        const currentMonth = (currentDate.getMonth() + 1).toString();
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

async function loadYears() {
    try {
        // Получаем годы из графиков диагностики
        const response = await fetch('/api/diagnostics-schedule');
        const schedules = await response.json();
        
        const years = schedules.map(s => s.year).filter((v, i, a) => a.indexOf(v) === i).sort((a, b) => b - a);
        const yearsArray = years.map(year => ({ year: year.toString() }));
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
    
    const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
    const monthsWithNames = Array.from({length: 12}, (_, i) => ({
        value: (i + 1).toString(),
        label: monthNames[i]
    }));
    
    populateSingleSelect('month', monthsWithNames, 'value', 'label', 'Все');
}

async function loadAreas() {
    try {
        const response = await fetch('/dashboard/diagnostics/areas');
        const areas = await response.json();
        
        const areasList = areas.map(a => ({
            area: a.area || a.name || a
        }));
        populateSingleSelect('area', areasList, 'area', 'area', 'Все участки');
    } catch (error) {
        console.error('Ошибка загрузки участков:', error);
    }
}

async function loadEquipment(area) {
    try {
        let url = '/dashboard/diagnostics/equipment';
        if (area && area !== 'all') {
            url += `?area=${encodeURIComponent(area)}`;
        }
        
        const response = await fetch(url);
        const equipment = await response.json();
        
        const equipmentList = equipment.map(e => ({
            equipment: e.machine_name || e.equipment || e
        }));
        populateSingleSelect('equipment', equipmentList, 'equipment', 'equipment', 'Всё оборудование');
    } catch (error) {
        console.error('Ошибка загрузки оборудования:', error);
    }
}

async function loadDiagnosticsTypes() {
    try {
        console.log('Загрузка типов диагностики...');
        const response = await fetch('/api/diagnostics-dynamics/diagnostics-types');
        console.log('Ответ от API типов диагностики:', response.status, response.statusText);
        
        const types = await response.json();
        console.log('Загружены типы диагностики:', types);
        
        if (types && types.length > 0) {
            console.log('Заполняем dropdown типами:', types.length, 'типов');
            populateSingleSelect('diagnostics-type', types, 'code', 'name', 'Все типы');
            
            // Проверяем, что опции добавились
            const dropdown = document.getElementById('diagnostics-type-dropdown');
            const options = dropdown.querySelectorAll('.single-select-option');
            console.log('Опций в dropdown после заполнения:', options.length);
        } else {
            console.warn('Типы диагностики не загружены или пусты');
        }
    } catch (error) {
        console.error('Ошибка загрузки типов диагностики:', error);
    }
}

async function applyFilters() {
    showLoading(true);
    try {
        const year = getSelectedYear();
        const month = getSelectedMonth();
        const area = getSelectedArea();
        const equipment = getSelectedEquipment();
        const diagnosticsType = getSelectedDiagnosticsType();
        
        console.log('Применение фильтров:', {
            year, month, area, equipment, diagnosticsType
        });
        
        // Строим URL с параметрами
        const params = new URLSearchParams();
        if (year !== 'all') params.append('year', year);
        if (month !== 'all') params.append('month', month);
        if (area !== 'all') params.append('area', area);
        if (equipment !== 'all') params.append('equipment', equipment);
        if (diagnosticsType !== 'all' && diagnosticsType) {
            params.append('diagnosticsType', diagnosticsType);
            console.log('Добавлен фильтр по типу диагностики:', diagnosticsType);
        }
        
        const url = '/api/diagnostics-dynamics/data?' + params.toString();
        console.log('URL запроса:', url);
        
        // Загружаем данные и статистику параллельно
        const [dataResponse, summaryResponse] = await Promise.all([
            fetch('/api/diagnostics-dynamics/data?' + params.toString()),
            fetch('/api/diagnostics-dynamics/summary?' + params.toString())
        ]);
        
        const data = await dataResponse.json();
        const summary = await summaryResponse.json();
        
        console.log('Полученные данные:', data.length, 'записей');
        console.log('Статистика:', summary);
        
        // Генерируем график
        generateChart(data, summary);
    } catch (error) {
        console.error('Ошибка применения фильтров:', error);
        showError('Ошибка применения фильтров: ' + error.message);
    } finally {
        showLoading(false);
    }
}

function generateChart(data, summary) {
    if (!data || data.length === 0) {
        showError('Нет данных для отображения');
        return;
    }
    
    // Проверяем, группированы ли данные по месяцам
    const groupByMonth = data.length > 0 && data[0].groupByMonth === true;
    
    // Сортируем данные по дате/месяцу
    data.sort((a, b) => {
        const dateA = new Date(a.date + (groupByMonth ? '-01' : ''));
        const dateB = new Date(b.date + (groupByMonth ? '-01' : ''));
        return dateA - dateB;
    });
    
    const labels = data.map(item => {
        if (groupByMonth) {
            // Формат YYYY-MM - отображаем как месяц
            const [year, month] = item.date.split('-');
            const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
                              'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
            const monthIndex = parseInt(month, 10) - 1;
            return monthNames[monthIndex] + ' ' + year;
        } else {
            // Формат YYYY-MM-DD - отображаем как дату
            const date = new Date(item.date);
            return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' });
        }
    });
    
    chartData = {
        labels: labels,
        datasets: [
            {
                label: 'Запланировано',
                data: data.map(item => Number(item.planned) || 0),
                borderColor: '#e31a1c',
                backgroundColor: '#e31a1c80',
                tension: 0.1,
                borderWidth: 2
            },
            {
                label: 'Выполнено',
                data: data.map(item => Number(item.completed) || 0),
                borderColor: '#33a02c',
                backgroundColor: '#33a02c80',
                tension: 0.1,
                borderWidth: 2
            }
        ]
    };
    
    createMainChart(summary);
    createCustomLegend(chartData.datasets);
}

function createMainChart(summary) {
    const ctx = document.getElementById('mainChart').getContext('2d');
    
    if (mainChart) {
        mainChart.destroy();
    }
    
    const totalPlanned = chartData.datasets[0].data.reduce((a, b) => a + b, 0);
    const totalCompleted = chartData.datasets[1].data.reduce((a, b) => a + b, 0);
    const completion = totalPlanned > 0 ? Math.round((totalCompleted / totalPlanned) * 100) : 0;
    
    mainChart = new Chart(ctx, {
        type: chartType,
        data: chartData,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: `Диагностика: Запланировано / Выполнено (Выполнение: ${completion}%)`
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

function toggleChartType() {
    chartType = chartType === 'line' ? 'bar' : 'line';
    const button = document.getElementById('toggleChartType');
    button.textContent = chartType === 'line' ? 'Накопительная' : 'Линейная';
    
    if (chartData) {
        createMainChart();
    }
}

function createCustomLegend(datasets) {
    const legendContainer = document.getElementById('customLegend');
    legendContainer.innerHTML = '';
    
    datasets.forEach((dataset, index) => {
        const legendItem = document.createElement('div');
        legendItem.className = 'legend-item';
        legendItem.style.display = 'flex';
        legendItem.style.alignItems = 'center';
        legendItem.style.marginBottom = '10px';
        legendItem.style.cursor = 'pointer';
        
        const colorBox = document.createElement('div');
        colorBox.style.width = '20px';
        colorBox.style.height = '20px';
        colorBox.style.backgroundColor = dataset.borderColor;
        colorBox.style.marginRight = '10px';
        colorBox.style.border = '1px solid #ddd';
        
        const label = document.createElement('span');
        label.textContent = dataset.label;
        
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.checked = true;
        checkbox.style.marginLeft = '10px';
        
        checkbox.addEventListener('change', function() {
            const meta = mainChart.getDatasetMeta(index);
            meta.hidden = !checkbox.checked;
            mainChart.update();
        });
        
        legendItem.appendChild(colorBox);
        legendItem.appendChild(label);
        legendItem.appendChild(checkbox);
        legendContainer.appendChild(legendItem);
    });
}

function toggleAllLegendItems() {
    const checkboxes = document.querySelectorAll('#customLegend input[type="checkbox"]');
    const allChecked = Array.from(checkboxes).every(cb => cb.checked);
    
    checkboxes.forEach((checkbox, index) => {
        checkbox.checked = !allChecked;
        const meta = mainChart.getDatasetMeta(index);
        meta.hidden = allChecked;
    });
    
    mainChart.update();
    
    const button = document.getElementById('toggleAllLegend');
    button.textContent = allChecked ? 'Включить все' : 'Отключить все';
}

function showLoading(show) {
    document.getElementById('loading').style.display = show ? 'block' : 'none';
}

function showError(message) {
    const errorDiv = document.getElementById('error');
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
    setTimeout(() => {
        errorDiv.style.display = 'none';
    }, 5000);
}

