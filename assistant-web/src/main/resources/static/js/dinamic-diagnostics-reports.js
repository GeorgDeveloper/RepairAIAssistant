let mainChart = null;
let chartType = 'line';
let chartData = null;

console.log('=== СКРИПТ dinamic-diagnostics-reports.js ЗАГРУЖЕН ===');

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
    const selectedRadio = document.querySelector('#' + prefix + '-dropdown input[type="radio"]:checked');
    if (selectedRadio) {
        const label = selectedRadio.nextElementSibling;
        if (label) {
            text.textContent = label.textContent;
        }
    }
}

function populateSingleSelect(prefix, data, valueField, displayField, defaultText) {
    const dropdown = document.getElementById(prefix + '-dropdown');
    
    // Удаляем все опции кроме "Все"
    const allOption = dropdown.querySelector('.single-select-option:first-child');
    dropdown.innerHTML = '';
    if (allOption) {
        dropdown.appendChild(allOption);
    }
    
    // Уникальные значения
    const seen = new Set();
    const uniqueData = [];
    
    data.forEach(item => {
        const value = item[valueField];
        if (value == null) return;
        
        const normalizedValue = String(value).trim();
        if (normalizedValue === '') return;
        
        if (!seen.has(normalizedValue)) {
            seen.add(normalizedValue);
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
    let selectedRadio = document.querySelector('#diagnostics-type-dropdown input[name="diagnosticsType"]:checked');
    if (!selectedRadio) {
        selectedRadio = document.querySelector('#diagnostics-type-dropdown input[type="radio"]:checked');
    }
    const value = selectedRadio ? selectedRadio.value : 'all';
    return value;
}

function showLoading(show) {
    const loading = document.getElementById('loading');
    if (loading) {
        loading.style.display = show ? 'block' : 'none';
    }
}

function showError(message) {
    const error = document.getElementById('error');
    if (error) {
        error.textContent = message;
        error.style.display = message ? 'block' : 'none';
    }
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
        
        // Загружаем месяцы для текущего года
        await loadMonths();
        
        // Применяем фильтры по умолчанию
        await applyFilters();
    } catch (error) {
        console.error('Ошибка загрузки начальных данных:', error);
        showError('Ошибка загрузки данных');
    } finally {
        showLoading(false);
    }
}

async function loadYears() {
    try {
        const response = await fetch('/api/diagnostics-reports-dynamics/data');
        const data = await response.json();
        
        const years = new Set();
        data.forEach(item => {
            if (item.date) {
                const dateStr = item.date;
                if (dateStr.length >= 4) {
                    const year = dateStr.substring(0, 4);
                    if (/^\d{4}$/.test(year)) {
                        years.add(year);
                    }
                }
            }
        });
        
        const sortedYears = Array.from(years).sort((a, b) => b - a);
        const yearList = sortedYears.map(y => ({ year: y }));
        populateSingleSelect('year', yearList, 'year', 'year', 'Все');
    } catch (error) {
        console.error('Ошибка загрузки годов:', error);
    }
}

async function loadMonths() {
    try {
        const year = getSelectedYear();
        if (year === 'all') {
            const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
                              'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
            const monthList = monthNames.map((name, index) => ({
                month: (index + 1).toString(),
                name: name
            }));
            populateSingleSelect('month', monthList, 'month', 'name', 'Все');
            return;
        }
        
        const response = await fetch(`/api/diagnostics-reports-dynamics/data?year=${year}`);
        const data = await response.json();
        
        const months = new Set();
        data.forEach(item => {
            if (item.date) {
                const dateStr = item.date;
                if (dateStr.length >= 7) {
                    const month = dateStr.substring(5, 7);
                    if (/^\d{2}$/.test(month)) {
                        months.add(parseInt(month, 10));
                    }
                }
            }
        });
        
        const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
                          'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
        const monthList = Array.from(months).sort((a, b) => a - b).map(m => ({
            month: m.toString(),
            name: monthNames[m - 1]
        }));
        populateSingleSelect('month', monthList, 'month', 'name', 'Все');
    } catch (error) {
        console.error('Ошибка загрузки месяцев:', error);
    }
}

async function loadAreas() {
    try {
        const response = await fetch('/dashboard/diagnostics/areas');
        const areas = await response.json();
        const areaList = areas.map(e => ({
            area: e.area || e.name
        }));
        populateSingleSelect('area', areaList, 'area', 'area', 'Все участки');
    } catch (error) {
        console.error('Ошибка загрузки участков:', error);
    }
}

async function loadEquipment(area) {
    try {
        let url = '/dashboard/diagnostics/equipment';
        if (area && area !== 'all') {
            url += '?area=' + encodeURIComponent(area);
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
        const response = await fetch('/api/diagnostics-reports-dynamics/diagnostics-types');
        console.log('Ответ от API типов диагностики:', response.status, response.statusText);
        
        const types = await response.json();
        console.log('Загружены типы диагностики:', types);
        
        if (types && types.length > 0) {
            console.log('Заполняем dropdown типами:', types.length, 'типов');
            const typeList = types.map(t => ({ type: t, name: t }));
            populateSingleSelect('diagnostics-type', typeList, 'type', 'name', 'Все типы');
            
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
        
        const url = '/api/diagnostics-reports-dynamics/data?' + params.toString();
        console.log('URL запроса:', url);
        
        // Загружаем данные
        const dataResponse = await fetch(url);
        const data = await dataResponse.json();
        
        console.log('Полученные данные:', data.length, 'записей');
        
        // Генерируем график
        generateChart(data);
    } catch (error) {
        console.error('Ошибка применения фильтров:', error);
        showError('Ошибка применения фильтров: ' + error.message);
    } finally {
        showLoading(false);
    }
}

function generateChart(data) {
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
                label: 'Обнаружено дефектов',
                data: data.map(item => Number(item.detected) || 0),
                borderColor: '#e31a1c',
                backgroundColor: '#e31a1c80',
                tension: 0.1,
                borderWidth: 2,
                hidden: false
            },
            {
                label: 'Открыто',
                data: data.map(item => Number(item.opened) || 0),
                borderColor: '#ff7f00',
                backgroundColor: '#ff7f0080',
                tension: 0.1,
                borderWidth: 2,
                hidden: false
            },
            {
                label: 'В работе',
                data: data.map(item => Number(item.inWork) || 0),
                borderColor: '#1f78b4',
                backgroundColor: '#1f78b480',
                tension: 0.1,
                borderWidth: 2,
                hidden: false
            },
            {
                label: 'Закрыто',
                data: data.map(item => Number(item.closed) || 0),
                borderColor: '#33a02c',
                backgroundColor: '#33a02c80',
                tension: 0.1,
                borderWidth: 2,
                hidden: false
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
    
    mainChart = new Chart(ctx, {
        type: chartType,
        data: chartData,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: 'Динамика отчетов диагностики'
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
                    title: {
                        display: true,
                        text: 'Период'
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
        colorBox.style.border = '1px solid #ccc';
        
        const label = document.createElement('span');
        label.textContent = dataset.label;
        label.style.marginRight = '10px';
        
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.checked = !dataset.hidden;
        checkbox.addEventListener('change', function() {
            const isHidden = !this.checked;
            dataset.hidden = isHidden;
            if (mainChart) {
                mainChart.update();
            }
        });
        
        legendItem.appendChild(colorBox);
        legendItem.appendChild(label);
        legendItem.appendChild(checkbox);
        
        legendItem.addEventListener('click', function(e) {
            if (e.target !== checkbox) {
                checkbox.checked = !checkbox.checked;
                checkbox.dispatchEvent(new Event('change'));
            }
        });
        
        legendContainer.appendChild(legendItem);
    });
}

function toggleAllLegendItems() {
    const checkboxes = document.querySelectorAll('#customLegend input[type="checkbox"]');
    const allChecked = Array.from(checkboxes).every(cb => cb.checked);
    
    checkboxes.forEach(checkbox => {
        checkbox.checked = !allChecked;
        checkbox.dispatchEvent(new Event('change'));
    });
}

