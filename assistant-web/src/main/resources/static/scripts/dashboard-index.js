// Specific functions for the index dashboard page (index.html)
const IndexDashboard = {
    // Управление месяцем
    currentDate: null,
    
    initMonthNavigation() {
        // Проверяем, что элементы существуют
        const prevBtn = document.getElementById('prevMonthBtn');
        const nextBtn = document.getElementById('nextMonthBtn');
        const monthDisplay = document.getElementById('currentMonthDisplay');
        
        if (!prevBtn || !nextBtn || !monthDisplay) {
            console.error('Элементы навигации по месяцам не найдены');
            return;
        }
        
        // Инициализация: устанавливаем текущий месяц (предыдущий день от сегодня)
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        this.currentDate = new Date(yesterday.getFullYear(), yesterday.getMonth(), 1);
        
        this.updateMonthDisplay();
        this.updateNavigationButtons();
        
        // Обработчики событий для кнопок навигации
        prevBtn.addEventListener('click', () => {
            this.navigateMonth(-1);
        });
        
        nextBtn.addEventListener('click', () => {
            this.navigateMonth(1);
        });
    },
    
    navigateMonth(direction) {
        const newDate = new Date(this.currentDate);
        newDate.setMonth(newDate.getMonth() + direction);
        
        // Проверяем, не превышает ли новый месяц текущий месяц
        const today = new Date();
        const currentMonth = new Date(today.getFullYear(), today.getMonth(), 1);
        
        if (newDate <= currentMonth) {
            this.currentDate = newDate;
            this.updateMonthDisplay();
            this.updateNavigationButtons();
            this.initializeDashboard();
        }
    },
    
    updateMonthDisplay() {
        const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
                          'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
        const monthName = monthNames[this.currentDate.getMonth()];
        const year = this.currentDate.getFullYear();
        document.getElementById('currentMonthDisplay').textContent = `${monthName} ${year}`;
    },
    
    updateNavigationButtons() {
        const today = new Date();
        const currentMonth = new Date(today.getFullYear(), today.getMonth(), 1);
        const isCurrentMonth = this.currentDate.getTime() === currentMonth.getTime();
        
        // Отключаем кнопку "вперед", если мы на текущем месяце
        const nextBtn = document.getElementById('nextMonthBtn');
        nextBtn.disabled = isCurrentMonth;
        nextBtn.style.opacity = isCurrentMonth ? '0.5' : '1';
        nextBtn.style.cursor = isCurrentMonth ? 'not-allowed' : 'pointer';
    },
    
    // Проверяет, является ли выбранный месяц текущим (предыдущим месяцем)
    isCurrentMonth() {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        const currentMonth = new Date(yesterday.getFullYear(), yesterday.getMonth(), 1);
        return this.currentDate.getTime() === currentMonth.getTime();
    },
    
    // Утилиты
    Utils: {
        filterEmptyData(dataPoints) {
            return DashboardUtils.filterEmptyData(dataPoints);
        },
        
        getColorForPoint(value, type) {
            return DashboardUtils.getColorForPoint(value, type);
        },
        
        formatSecondsToHHMMSS(totalSeconds) {
            return DashboardUtils.formatSecondsToHHMMSS(totalSeconds);
        }
    },

    // Создание графиков
    Charts: {
        // Генерирует все дни месяца
        generateAllDaysInMonth(year, month) {
            const daysInMonth = new Date(year, month, 0).getDate();
            const days = [];
            for (let day = 1; day <= daysInMonth; day++) {
                const dayStr = String(day).padStart(2, '0');
                const monthStr = String(month).padStart(2, '0');
                days.push(`${dayStr}.${monthStr}.${year}`);
            }
            return days;
        },
        
        create(containerId, chartId, title, dataPoints, chartType) {
            // Уничтожаем старый график и resizable, если они существуют
            try {
                const existingChart = $(chartId).CanvasJSChart();
                if (existingChart) {
                    existingChart.destroy();
                }
            } catch (e) {
                // Игнорируем ошибки, если графика нет
            }
            
            // Уничтожаем resizable, если он существует
            try {
                if ($(containerId).data('ui-resizable')) {
                    $(containerId).resizable('destroy');
                }
            } catch (e) {
                // Игнорируем ошибки
            }
            
            // Проверяем, является ли текущий месяц выбранным
            const isCurrentMonth = IndexDashboard.isCurrentMonth();
            
            let coloredDataPoints;
            
            if (isCurrentMonth) {
                // Для текущего месяца используем старую логику - только дни с данными
                const filteredData = IndexDashboard.Utils.filterEmptyData(dataPoints);
                
                if (!filteredData.length) {
                    document.querySelector(chartId).innerHTML = 
                        `<div style="text-align: center; padding-top: 50px; color: #677;">
                            <h3>${title}</h3>
                            <p>Нет данных для отображения</p>
                        </div>`;
                    return;
                }
                
                const uniqueDataMap = new Map();
                filteredData.forEach(point => {
                    if (point.label && point.y != null) {
                        uniqueDataMap.set(point.label, point.y);
                    }
                });
                
                coloredDataPoints = Array.from(uniqueDataMap.entries())
                    .sort((a, b) => {
                        // Сортируем по дате правильно
                        const dateA = a[0].split('.').reverse().join('-');
                        const dateB = b[0].split('.').reverse().join('-');
                        return new Date(dateA) - new Date(dateB);
                    })
                    .map(([label, y]) => ({
                        label, y,
                        color: IndexDashboard.Utils.getColorForPoint(y, chartType)
                    }));
            } else {
                // Для других месяцев - показываем все дни месяца
                const year = IndexDashboard.currentDate.getFullYear();
                const month = IndexDashboard.currentDate.getMonth() + 1;
                const allDays = this.generateAllDaysInMonth(year, month);
                
                // Создаем Map для быстрого доступа к данным
                const dataMap = new Map();
                if (dataPoints && dataPoints.length > 0) {
                    dataPoints.forEach(point => {
                        if (point.label && point.y != null) {
                            dataMap.set(point.label, point.y);
                        }
                    });
                }
                
                // Создаем точки данных для всех дней месяца
                coloredDataPoints = allDays.map(label => {
                    const y = dataMap.get(label) || null;
                    return {
                        label,
                        y: y,
                        color: y != null ? IndexDashboard.Utils.getColorForPoint(y, chartType) : '#cccccc'
                    };
                });
            }
            
            const options = {
                animationEnabled: true,
                title: { text: title, fontSize: 14 },
                axisX: {
                    labelAngle: -45,
                    interval: isCurrentMonth ? 2 : 1,
                    labelAutoFit: true,
                    labelMaxWidth: isCurrentMonth ? 80 : 60,
                    labelFontSize: isCurrentMonth ? 10 : 9
                },
                axisY: { suffix: "%", labelFontSize: 10 },
                data: [{
                    type: "column",
                    showInLegend: false,
                    dataPoints: coloredDataPoints,
                    dataPointWidth: 10,
                    click: function(e) {
                        if (e.dataPoint.y != null) {
                            console.log('Chart column clicked:', e.dataPoint.label);
                            const date = e.dataPoint.label;
                            IndexDashboard.showIndicatorTable(date);
                        }
                    }
                }]
            };
            
            // Очищаем контейнер перед созданием нового графика
            const chartElement = document.querySelector(chartId);
            if (chartElement) {
                chartElement.innerHTML = '';
            }
            
            // Создаем новый график
            setTimeout(() => {
                try {
                    $(chartId).CanvasJSChart(options);
                    const chart = $(chartId).CanvasJSChart();
                    if (chart) {
                        chart.render();
                    }
                } catch (e) {
                    console.error('Ошибка при создании графика:', e);
                }
            }, 100);
            
            // Настраиваем resizable после создания графика
            setTimeout(() => {
                try {
                    $(containerId).resizable({
                        create() {
                            const chart = $(chartId).CanvasJSChart();
                            if (chart) {
                                chart.render();
                            }
                        },
                        resize() {
                            const chart = $(chartId).CanvasJSChart();
                            if (chart) {
                                chart.render();
                            }
                        }
                    });
                } catch (e) {
                    console.error('Ошибка при настройке resizable:', e);
                }
            }, 200);
        },
        
        createPmLineChart(containerSelector, chartSelector, data) {
            // Уничтожаем старый график и resizable, если они существуют
            try {
                const existingChart = $(chartSelector).CanvasJSChart();
                if (existingChart) {
                    existingChart.destroy();
                }
            } catch (e) {
                // Игнорируем ошибки, если графика нет
            }
            
            try {
                if ($(containerSelector).data('ui-resizable')) {
                    $(containerSelector).resizable('destroy');
                }
            } catch (e) {
                // Игнорируем ошибки
            }
            
            // Проверяем, является ли текущий месяц выбранным
            const isCurrentMonth = IndexDashboard.isCurrentMonth();
            
            let parse;
            
            if (isCurrentMonth) {
                // Для текущего месяца используем старую логику - только дни с данными
                if (!data?.length) {
                    document.querySelector(chartSelector).innerHTML = '<p>Нет данных для отображения</p>';
                    return;
                }
                
                parse = key => data.map(x => ({ 
                    label: x.production_day, 
                    y: Number(x[key]) || 0 
                })).sort((a, b) => {
                    // Сортируем по дате правильно
                    const dateA = a.label.split('.').reverse().join('-');
                    const dateB = b.label.split('.').reverse().join('-');
                    return new Date(dateA) - new Date(dateB);
                });
            } else {
                // Для других месяцев - показываем все дни месяца
                const year = IndexDashboard.currentDate.getFullYear();
                const month = IndexDashboard.currentDate.getMonth() + 1;
                const allDays = this.generateAllDaysInMonth(year, month);
                
                // Создаем Map для быстрого доступа к данным
                const dataMap = new Map();
                if (data && data.length > 0) {
                    data.forEach(x => {
                        if (x.production_day) {
                            dataMap.set(x.production_day, {
                                plan: Number(x.plan) || 0,
                                fact: Number(x.fact) || 0,
                                tag: Number(x.tag) || 0
                            });
                        }
                    });
                }
                
                // Создаем точки данных для всех дней месяца
                parse = key => allDays.map(label => {
                    const dayData = dataMap.get(label);
                    return {
                        label,
                        y: dayData ? dayData[key] : 0
                    };
                });
            }
            
            const planPoints = parse('plan');
            const factPoints = parse('fact');
            const tagPoints = parse('tag');
            
            const totalPlan = planPoints.reduce((s, x) => s + (x.y || 0), 0);
            const totalFact = factPoints.reduce((s, x) => s + (x.y || 0), 0);
            const completion = totalPlan > 0 ? Math.round((totalFact / totalPlan) * 100) : 0;
            
            const options = {
                animationEnabled: true,
                backgroundColor: "#ffffff",
                title: { text: "PM: План / Факт / Tag", fontSize: 14 },
                subtitles: [{
                    text: `Выполнение: ${completion}%`,
                    verticalAlign: "top",
                    horizontalAlign: "right",
                    dockInsidePlotArea: true
                }],
                axisX: { title: "Дата", labelAngle: -45, margin: 10, labelFontSize: isCurrentMonth ? 10 : 9, interval: isCurrentMonth ? 2 : 1 },
                axisY: { title: "Количество", includeZero: true, margin: 10, labelFontSize: 10 },
                legend: { verticalAlign: "bottom" },
                data: [
                    { type: "line", name: "Plan", showInLegend: true, color: "#e31a1c", markerSize: 4, dataPoints: planPoints },
                    { type: "line", name: "Fact", showInLegend: true, color: "#33a02c", markerSize: 4, dataPoints: factPoints },
                    { type: "line", name: "Tag", showInLegend: true, color: "#1f78b4", markerSize: 4, dataPoints: tagPoints }
                ]
            };
            
            // Очищаем контейнер перед созданием нового графика
            const chartElement = document.querySelector(chartSelector);
            if (chartElement) {
                chartElement.innerHTML = '';
            }
            
            // Создаем новый график
            setTimeout(() => {
                try {
                    $(chartSelector).CanvasJSChart(options);
                    const chart = $(chartSelector).CanvasJSChart();
                    if (chart) {
                        chart.render();
                    }
                } catch (e) {
                    console.error('Ошибка при создании PM графика:', e);
                }
            }, 100);
            
            // Настраиваем resizable после создания графика
            setTimeout(() => {
                try {
                    $(containerSelector).resizable({
                        create() {
                            const chart = $(chartSelector).CanvasJSChart();
                            if (chart) {
                                chart.render();
                            }
                        },
                        resize() {
                            const chart = $(chartSelector).CanvasJSChart();
                            if (chart) {
                                chart.render();
                            }
                        }
                    });
                } catch (e) {
                    console.error('Ошибка при настройке resizable для PM:', e);
                }
            }, 200);
        }
    },

    // Создание таблиц
    Tables: {
        createMetrics(metricsData) {
            if (!metricsData) {
                document.getElementById('metricsTable').innerHTML = '<p>Нет данных для отображения</p>';
                return;
            }
            
            // Используем выбранный месяц из навигации
            const selectedDate = IndexDashboard.currentDate;
            const monthNames = ['январь', 'февраль', 'март', 'апрель', 'май', 'июнь',
                              'июль', 'август', 'сентябрь', 'октябрь', 'ноябрь', 'декабрь'];
            const currentMonth = monthNames[selectedDate.getMonth()];
            
            // Проверяем, является ли текущий месяц выбранным
            const isCurrentMonth = IndexDashboard.isCurrentMonth();
            
            let prevDay;
            if (isCurrentMonth) {
                // Для текущего месяца используем старую логику - предыдущий день
                const prevDate = new Date(Date.now() - 24 * 60 * 60 * 1000);
                prevDay = prevDate.toLocaleDateString('ru-RU');
            } else {
                // Для других месяцев - последний день выбранного месяца
                const lastDayOfMonth = new Date(selectedDate.getFullYear(), selectedDate.getMonth() + 1, 0);
                const day = String(lastDayOfMonth.getDate()).padStart(2, '0');
                const month = String(lastDayOfMonth.getMonth() + 1).padStart(2, '0');
                const year = lastDayOfMonth.getFullYear();
                prevDay = `${day}.${month}.${year}`;
            }
            
            const sections = [
                { name: 'Резиносмешение', prefix: 'report_new_mixing_area' },
                { name: 'Сборка 1', prefix: 'report_semifinishing_area' },
                { name: 'Сборка 2', prefix: 'report_building_area' },
                { name: 'Вулканизация', prefix: 'report_curing_area' },
                { name: 'УЗО', prefix: 'report_finishig_area' },
                { name: 'Модули', prefix: 'report_modules' },
                { name: 'Завод', prefix: 'report_plant' }
            ];
            
            let tableHTML = `
                <table class="metrics-table">
                    <thead>
                        <tr>
                            <th>Показатель</th>
                            <th>Цель</th>
                            <th>${currentMonth}</th>
                            <th>${prevDay}</th>
                        </tr>
                    </thead>
                    <tbody>`;
            
            sections.forEach(section => {
                const bdMonth = metricsData[`${section.prefix}_bd_month`];
                const bdToday = metricsData[`${section.prefix}_bd_today`];
                const avMonth = metricsData[`${section.prefix}_availability_month`];
                const avToday = metricsData[`${section.prefix}_availability_today`];
                
                const formatValue = (value, isPercentage = true) => 
                    value != null ? value.toFixed(2) + (isPercentage ? '%' : '') : 'N/A';
                
                const getClass = (value, target, isGreater = false) => {
                    if (value === 0) return '';
                    return isGreater ? 
                        (value >= target ? 'target-met' : 'target-not-met') :
                        (value <= target ? 'target-met' : 'target-not-met');
                };
                
                tableHTML += `
                    <tr class="section-header">
                        <td colspan="4">${section.name}</td>
                    </tr>
                    <tr>
                        <td>BD</td>
                        <td>2%</td>
                        <td class="${getClass(bdMonth, 2)}">${formatValue(bdMonth)}</td>
                        <td class="${getClass(bdToday, 2)}">${formatValue(bdToday)}</td>
                    </tr>
                    <tr>
                        <td>Доступность</td>
                        <td>97%</td>
                        <td class="${getClass(avMonth, 97, true)}">${formatValue(avMonth)}</td>
                        <td class="${getClass(avToday, 97, true)}">${formatValue(avToday)}</td>
                    </tr>`;
            });
            
            tableHTML += '</tbody></table>';
            document.getElementById('metricsTable').innerHTML = tableHTML;
        },
        
        createTopBreakdowns(data, containerId, isWeekly = false) {
            DashboardTables.createTopBreakdownsTable(data, containerId, isWeekly);
        }
    },

    // Инициализация дашборда
    async initializeDashboard() {
        try {
            // Получаем год и месяц из currentDate
            const year = this.currentDate.getFullYear();
            const month = this.currentDate.getMonth() + 1; // JavaScript месяцы начинаются с 0
            
            // Проверяем, является ли текущий месяц выбранным
            const isCurrentMonth = this.isCurrentMonth();
            
            // Формируем URL с параметрами месяца
            const breakDownUrl = `/dashboard/breakDown?year=${year}&month=${month}`;
            const availabilityUrl = `/dashboard/availability?year=${year}&month=${month}`;
            const pmUrl = `/dashboard/pm-plan-fact-tag?year=${year}&month=${month}`;
            const metricsUrl = `/dashboard/current-metrics?year=${year}&month=${month}`;
            
            // Определяем, какие endpoints использовать для топ поломок
            let topBreakdownsUrl, topBreakdownsKeyLinesUrl;
            if (isCurrentMonth) {
                // Для текущего месяца - данные за неделю
                topBreakdownsUrl = '/dashboard/top-breakdowns-week';
                topBreakdownsKeyLinesUrl = '/dashboard/top-breakdowns-week-key-lines';
            } else {
                // Для предыдущих месяцев - данные за месяц
                topBreakdownsUrl = `/dashboard/top-breakdowns-month?year=${year}&month=${month}`;
                topBreakdownsKeyLinesUrl = `/dashboard/top-breakdowns-month-key-lines?year=${year}&month=${month}`;
            }
            
            // Загрузка данных для графиков
            const [breakDownData, availabilityData, currentMetrics, topBreakdowns, topBreakdownsKeyLines, pmData] = await Promise.all([
                DashboardAPI.fetchData(breakDownUrl),
                DashboardAPI.fetchData(availabilityUrl),
                DashboardAPI.fetchData(metricsUrl),
                DashboardAPI.fetchData(topBreakdownsUrl),
                DashboardAPI.fetchData(topBreakdownsKeyLinesUrl),
                DashboardAPI.fetchData(pmUrl)
            ]);
            
            // Подготовка данных для графиков
            const breakDownPoints = breakDownData?.map(item => ({
                label: item.production_day,
                y: item.downtime_percentage != null ? Number(item.downtime_percentage) : null
            })) || [];
            
            const availabilityPoints = availabilityData?.map(item => ({
                label: item.production_day,
                y: item.availability != null ? Number(item.availability) : null
            })) || [];
            
            console.log('BreakDown data points:', breakDownPoints.length);
            console.log('Availability data points:', availabilityPoints.length);
            console.log('PM data:', pmData?.length || 0);
            
            // Обновляем заголовки таблиц
            this.updateTableHeaders(isCurrentMonth);
            
            // Создание графиков и таблиц
            // Используем setTimeout для обеспечения правильного порядка создания
            setTimeout(() => {
                this.Charts.create("#resizable1", "#breakDown", "BreakDown %", breakDownPoints, "breakDown");
            }, 50);
            
            setTimeout(() => {
                this.Charts.create("#resizable2", "#availability", "Availability %", availabilityPoints, "availability");
            }, 100);
            
            setTimeout(() => {
                this.Charts.createPmLineChart('#resizablePm', '#pmChart', pmData);
            }, 150);
            
            this.Tables.createMetrics(currentMetrics);
            // Для недельных и месячных данных используется одинаковый формат (machine_name и machine_downtime_seconds)
            // Всегда используем формат с двумя колонками (machine_name и machine_downtime)
            this.Tables.createTopBreakdowns(topBreakdowns, 'topBreakdownsWeekTable', true);
            this.Tables.createTopBreakdowns(topBreakdownsKeyLines, 'topBreakdownsWeekKeyLinesTable', true);
            
        } catch (error) {
            console.error('Ошибка при инициализации дашборда:', error);
        }
    },
    
    // Обновление заголовков таблиц в зависимости от выбранного месяца
    updateTableHeaders(isCurrentMonth) {
        const keyLinesHeader = document.getElementById('keyLinesHeader');
        const topBreakdownsHeader = document.getElementById('topBreakdownsHeader');
        
        if (keyLinesHeader) {
            keyLinesHeader.textContent = isCurrentMonth ? 'Ключевые линии (неделя)' : 'Ключевые линии (месяц)';
        }
        
        if (topBreakdownsHeader) {
            topBreakdownsHeader.textContent = isCurrentMonth ? 'Топ поломок за неделю' : 'Топ поломок за месяц';
        }
    }
};

// Инициализация при загрузке страницы
window.onload = async function() {
    IndexDashboard.initMonthNavigation();
    await IndexDashboard.initializeDashboard();
    
    // Настройка автоматического перерендера графиков при изменении размеров
    const rerender = () => {
        ['#breakDown', '#availability', '#pmChart'].forEach(sel => {
            try { 
                const chart = $(sel).CanvasJSChart(); 
                chart?.render(); 
            } catch (e) {}
        });
    };
    
    const grid = document.querySelector('.dashboard-grid');
    if (grid && window.ResizeObserver) {
        new ResizeObserver(rerender).observe(grid);
    }
    
    setTimeout(rerender, 200);
    setTimeout(rerender, 400);
};

// Function to create and show indicator table in modal
IndexDashboard.showIndicatorTable = async function(date) {
    try {
        console.log('Showing indicator table for date:', date);
        // Fetch metrics data for the selected date
        const metricsData = await DashboardAPI.fetchData(`/dashboard/metrics-for-date?date=${date}`);
        
        if (!metricsData) {
            console.log('No metrics data found for date:', date);
            document.getElementById('indicatorTableContainer').innerHTML = '<p>Нет данных для отображения</p>';
            return;
        }
        
        console.log('Metrics data received:', metricsData);
        // Set the selected date in the modal header
        document.getElementById('selectedDate').textContent = date;
        
        // Create the table HTML
        const sections = [
            { name: 'Резиносмешение', prefix: 'report_new_mixing_area' },
            { name: 'Сборка 1', prefix: 'report_semifinishing_area' },
            { name: 'Сборка 2', prefix: 'report_building_area' },
            { name: 'Вулканизация', prefix: 'report_curing_area' },
            { name: 'УЗО', prefix: 'report_finishig_area' },
            { name: 'Модули', prefix: 'report_modules' },
            { name: 'Завод', prefix: 'report_plant' }
        ];
        
        let tableHTML = `
            <table class="metrics-table">
                <thead>
                    <tr>
                        <th>Показатель</th>
                        <th>Цель</th>
                        <th>октябрь</th>
                        <th>${date}</th>
                    </tr>
                </thead>
                <tbody>`;
        
        sections.forEach(section => {
            const bdMonth = metricsData[`${section.prefix}_bd_month`];
            const bdToday = metricsData[`${section.prefix}_bd_today`];
            const avMonth = metricsData[`${section.prefix}_availability_month`];
            const avToday = metricsData[`${section.prefix}_availability_today`];
            
            const formatValue = (value, isPercentage = true) =>
                value != null ? value.toFixed(2) + (isPercentage ? '%' : '') : 'N/A';
            
            const getClass = (value, target, isGreater = false) => {
                if (value === 0) return '';
                return isGreater ?
                    (value >= target ? 'target-met' : 'target-not-met') :
                    (value <= target ? 'target-met' : 'target-not-met');
            };
            
            tableHTML += `
                <tr class="section-header">
                    <td colspan="4">${section.name}</td>
                </tr>
                <tr>
                    <td>BD</td>
                    <td>2%</td>
                    <td class="${getClass(bdMonth, 2)}">${formatValue(bdMonth)}</td>
                    <td class="${getClass(bdToday, 2)} clickable-cell" 
                        data-date="${date}" 
                        data-area="${section.name}" 
                        data-metric="BD" 
                        title="Кликните для просмотра детализации">${formatValue(bdToday)}</td>
                </tr>
                <tr>
                    <td>Доступность</td>
                    <td>97%</td>
                    <td class="${getClass(avMonth, 97, true)}">${formatValue(avMonth)}</td>
                    <td class="${getClass(avToday, 97, true)}">${formatValue(avToday)}</td>
                </tr>`;
        });
        
        tableHTML += '</tbody></table>';
        document.getElementById('indicatorTableContainer').innerHTML = tableHTML;
        
        // Add click event listeners to clickable cells
        addClickListenersToIndicatorTable();
        
        // Show the modal
        console.log('Showing modal');
        document.getElementById('indicatorModal').style.display = 'flex';
    } catch (error) {
        console.error('Ошибка при получении данных для таблицы:', error);
        document.getElementById('indicatorTableContainer').innerHTML = '<p>Ошибка при загрузке данных</p>';
    }
};

// Add click event listeners to charts after they are created
IndexDashboard.addChartClickListeners = function() {
    // Add click listener to BreakDown chart
    const breakDownChart = $('#breakDown').CanvasJSChart();
    if (breakDownChart) {
        breakDownChart.options.data[0].click = function(e) {
            const date = e.dataPoint.label;
            IndexDashboard.showIndicatorTable(date);
        };
    }
    
    // Add click listener to Availability chart
    const availabilityChart = $('#availability').CanvasJSChart();
    if (availabilityChart) {
        availabilityChart.options.data[0].click = function(e) {
            const date = e.dataPoint.label;
            IndexDashboard.showIndicatorTable(date);
        };
    }
};

// Function to add click listeners to indicator table cells
function addClickListenersToIndicatorTable() {
    const clickableCells = document.querySelectorAll('.clickable-cell');
    clickableCells.forEach(cell => {
        cell.addEventListener('click', function() {
            const date = this.getAttribute('data-date');
            const area = this.getAttribute('data-area');
            const metric = this.getAttribute('data-metric');
            
            console.log('Cell clicked:', { date, area, metric });
            showBreakdownDetails(date, area, metric);
        });
    });
}

// Function to map Russian area names to English area codes
function mapAreaNameToCode(areaName) {
    const areaMapping = {
        'Резиносмешение': 'NewMixingArea',
        'Сборка 1': 'SemifinishingArea', 
        'Сборка 2': 'BuildingArea',
        'Вулканизация': 'CuringArea',
        'УЗО': 'FinishigArea',
        'Модули': 'Modules',
        'Завод': 'Plant'
    };
    return areaMapping[areaName] || areaName;
}

// Function to show breakdown details modal
async function showBreakdownDetails(date, area, metric) {
    try {
        console.log('Fetching breakdown details for:', { date, area, metric });
        
        // Map area name to code
        const areaCode = mapAreaNameToCode(area);
        console.log('Mapped area:', area, '->', areaCode);
        
        // Set modal header info
        document.getElementById('breakdownDate').textContent = date;
        document.getElementById('breakdownArea').textContent = area;
        
        // Show loading state
        document.getElementById('breakdownDetailsContainer').innerHTML = '<p>Загрузка данных...</p>';
        document.getElementById('breakdownDetailsModal').style.display = 'flex';
        
        // Fetch breakdown details
        const breakdownData = await DashboardAPI.fetchData(`/api/work-orders/breakdown-details?date=${date}&area=${areaCode}`);
        
        if (!breakdownData || breakdownData.length === 0) {
            document.getElementById('breakdownDetailsContainer').innerHTML = '<p>Нет данных о нарядах для указанной даты и области</p>';
            return;
        }
        
        // Create breakdown details table
        let tableHTML = `
            <table class="breakdown-details-table">
                <thead>
                    <tr>
                        <th>Код события</th>
                        <th>Машина</th>
                        <th>Узел/механизм</th>
                        <th>Тип поломки</th>
                        <th>Статус</th>
                        <th>Время простоя</th>
                        <th>Время начала</th>
                        <th>Время окончания</th>
                        <th>Причина</th>
                        <th>Комментарий</th>
                    </tr>
                </thead>
                <tbody>`;
        
        breakdownData.forEach(record => {
            const duration = record.machine_downtime || 'N/A';
            const startTime = record.start_bd_t1 || 'N/A';
            const endTime = record.stop_bd_t4 || 'N/A';
            const comment = record.comments || 'N/A';
            const cause = record.cause || 'N/A';
            const failureType = record.failure_type || 'N/A';
            
            // Add color coding based on failure type
            let rowClass = '';
            if (failureType && failureType.toLowerCase().includes('электрик')) rowClass = 'electrical-downtime';
            else if (failureType && failureType.toLowerCase().includes('электрон')) rowClass = 'electronic-downtime';
            else if (failureType && failureType.toLowerCase().includes('механ')) rowClass = 'mechanical-downtime';
            
            tableHTML += `
                <tr class="${rowClass}">
                    <td>${record.code || 'N/A'}</td>
                    <td>${record.machine_name || 'N/A'}</td>
                    <td>${record.mechanism_node || 'N/A'}</td>
                    <td>${failureType}</td>
                    <td>${record.status || 'N/A'}</td>
                    <td>${duration}</td>
                    <td>${startTime}</td>
                    <td>${endTime}</td>
                    <td>${cause}</td>
                    <td class="comment-cell">${comment}</td>
                </tr>`;
        });
        
        tableHTML += '</tbody></table>';
        document.getElementById('breakdownDetailsContainer').innerHTML = tableHTML;
        
    } catch (error) {
        console.error('Ошибка при получении детализации нарядов:', error);
        document.getElementById('breakdownDetailsContainer').innerHTML = '<p>Ошибка при загрузке данных</p>';
    }
}

// Add event listener to close modal when clicking on the close button
document.addEventListener('DOMContentLoaded', function() {
    const indicatorModal = document.getElementById('indicatorModal');
    const breakdownModal = document.getElementById('breakdownDetailsModal');
    const closeBtns = document.querySelectorAll('.modal .close');
    
    closeBtns.forEach(closeBtn => {
        closeBtn.addEventListener('click', function() {
            console.log('Close button clicked');
            const modal = this.closest('.modal');
            modal.style.display = 'none';
        });
    });
    
    // Close modal when clicking outside of it
    window.addEventListener('click', function(event) {
        if (event.target === indicatorModal) {
            console.log('Clicked outside indicator modal');
            indicatorModal.style.display = 'none';
        }
        if (event.target === breakdownModal) {
            console.log('Clicked outside breakdown modal');
            breakdownModal.style.display = 'none';
        }
    });
});