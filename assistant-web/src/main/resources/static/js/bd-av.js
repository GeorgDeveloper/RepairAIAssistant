let chartType = 'line';
let chart;
let showLabels = false;

document.addEventListener('DOMContentLoaded', async () => {
    setupEventListeners();
    await loadYears();
    await applyFilters();
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
    setupMultiSelect('area');
    
    document.getElementById('apply').addEventListener('click', applyFilters);
    document.getElementById('toggleType').addEventListener('click', () => {
        chartType = chartType === 'line' ? 'bar' : 'line';
        document.getElementById('toggleType').textContent = chartType === 'line' ? 'Столбики' : 'Линия';
        applyFilters();
    });
    document.getElementById('toggleLabels').addEventListener('click', () => {
        showLabels = !showLabels;
        document.getElementById('toggleLabels').textContent = `Значения: ${showLabels ? 'вкл' : 'выкл'}`;
        applyFilters();
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

async function loadYears() {
    const res = await fetch('/bdav/years');
    const data = await res.json();
    populateMultiSelect('year', data, 'year', 'Все');
    
    // Устанавливаем текущий год по умолчанию
    const currentYear = new Date().getFullYear().toString();
    const safeId = `year-${currentYear}`.replace(/[^a-zA-Z0-9-_]/g, '_');
    const currentYearCheckbox = document.getElementById(safeId);
    if (currentYearCheckbox) {
        currentYearCheckbox.checked = true;
        document.getElementById('year-all').checked = false;
        updateButtonText('year');
    }
    await loadMonths();
}

async function loadMonths() {
    const years = getSelectedValues('year');
    if (years.includes('all')) {
        const dropdown = document.getElementById('month-dropdown');
        const existingOptions = dropdown.querySelectorAll('.multi-select-option:not(:first-child)');
        existingOptions.forEach(option => option.remove());
        updateButtonText('month');
        return;
    }
    const res = await fetch(`/bdav/months?year=${years.join(',')}`);
    const data = await res.json();
    const names = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];
    const monthsWithNames = data.map(item => ({
        month: item.month,
        monthName: names[item.month - 1]
    }));
    populateMultiSelect('month', monthsWithNames, 'monthName', 'Все');
    await loadWeeks();
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
    
    // Преобразуем названия месяцев обратно в номера
    const monthNames = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];
    const monthNumbers = months.map(monthName => {
        const index = monthNames.indexOf(monthName);
        return index + 1;
    });
    
    const res = await fetch(`/bdav/weeks?year=${years.join(',')}&month=${monthNumbers.join(',')}`);
    const data = await res.json();
    populateMultiSelect('week', data, 'week', 'Все');
}

async function applyFilters() {
    const years = getSelectedValues('year');
    const months = getSelectedValues('month');
    const weeks = getSelectedValues('week');
    const areas = getSelectedValues('area');
    const metric = document.getElementById('metric').value;
    
    // Преобразуем названия месяцев обратно в номера
    const monthNames = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];
    const monthNumbers = months.map(monthName => {
        const index = monthNames.indexOf(monthName);
        return index + 1;
    });
    
    const params = new URLSearchParams();
    if (!years.includes('all')) {
        years.forEach(year => params.append('year', year));
    }
    if (!months.includes('all')) {
        monthNumbers.forEach(month => params.append('month', month));
    }
    if (!weeks.includes('all')) {
        weeks.forEach(week => params.append('week', week));
    }
    if (!areas.includes('all')) {
        areas.forEach(area => params.append('area', area));
    } else {
        // Если выбрано "Все участки", не передаем параметр area
        // Сервер будет использовать все участки по умолчанию
    }
    params.append('metric', metric);
    
    const res = await fetch(`/bdav/data?${params.toString()}`);
    if (!res.ok) {
        console.error('Ошибка загрузки данных:', res.status, res.statusText);
        throw new Error(`HTTP error! status: ${res.status}`);
    }
    const data = await res.json();
    console.log('Полученные данные:', data); // Добавим логирование для отладки
    renderChart(data, metric, areas);
}

function renderChart(rows, metric, areas) {
    console.log('renderChart вызван с данными:', rows, 'тип:', typeof rows);
    
    // Проверяем, что данные пришли в правильном формате
    if (!Array.isArray(rows)) {
        console.error('Данные не являются массивом:', rows);
        return;
    }
    
    if (rows.length === 0) {
        console.warn('Нет данных для отображения');
        return;
    }
    
    const labels = [...new Set(rows.map(r => r.period_label))];
    console.log('Уникальные метки периодов:', labels);
    const ctx = document.getElementById('chart').getContext('2d');
    if (chart) chart.destroy();
    const datasets = [];
    const areaColors = {
        'Plant':'#34495e','NewMixingArea':'#e74c3c','SemifinishingArea':'#9b59b6','BuildingArea':'#f39c12',
        'CuringArea':'#27ae60','FinishingArea':'#2980b9','Modules':'#8e44ad'
    };
    const areaList = areas.includes('all') ? ['Plant','NewMixingArea','SemifinishingArea','BuildingArea','CuringArea','FinishingArea','Modules'] : areas;
    console.log('Список участков для отображения:', areaList);
    console.log('Пример данных:', rows.slice(0, 3)); // Показываем первые 3 записи для отладки
    
    areaList.forEach(a => {
        const color = areaColors[a] || '#2ecc71';
        const values = labels.map(l => {
            const row = rows.find(r => String(r.period_label) === String(l) && r.area === a);
            return row ? +row.value : 0;
        });
        console.log(`Данные для участка ${a}:`, values);
        datasets.push({
            label: (metric === 'bd' ? 'BD % - ' : 'Доступность % - ') + a,
            data: values,
            borderColor: color,
            backgroundColor: color + '88',
            borderWidth: 2,
            tension: 0.2
        });
    });

    // обязательно зарегистрировать плагин до создания графика
    if (window.ChartDataLabels && !Chart.registry.plugins.get('datalabels')) {
        Chart.register(window.ChartDataLabels);
    }
    chart = new Chart(ctx, {
        type: chartType,
        data: {
            labels: labels,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: true },
                tooltip: { enabled: true },
                datalabels: {
                    display: showLabels,
                    color: '#333',
                    align: 'top',
                    formatter: (v) => (v != null ? v.toFixed(1) : '')
                }
            },
            scales: {
                y: { beginAtZero: true, title: { display: true, text: metric === 'bd' ? 'BD, %' : 'Доступность, %' } },
                x: { title: { display: true, text: xAxisTitle() } }
            }
        }
    });
}

function xAxisTitle() {
    const weeks = getSelectedValues('week');
    const months = getSelectedValues('month');
    const years = getSelectedValues('year');
    
    if (weeks.length > 0 && !weeks.includes('all')) return 'Дни месяца';
    if (months.length > 0 && !months.includes('all')) return 'Номера недель';
    if (years.length > 0 && !years.includes('all')) return 'Месяцы';
    return 'Годы';
}