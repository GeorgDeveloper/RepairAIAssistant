// Specific functions for the index dashboard page (index.html)
const IndexDashboard = {
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
        create(containerId, chartId, title, dataPoints, chartType) {
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
            
            const coloredDataPoints = Array.from(uniqueDataMap.entries())
                .sort((a, b) => new Date(a[0]) - new Date(b[0]))
                .map(([label, y]) => ({
                    label, y,
                    color: IndexDashboard.Utils.getColorForPoint(y, chartType)
                }));
            
            const options = {
                animationEnabled: true,
                title: { text: title, fontSize: 14 },
                axisX: {
                    labelAngle: -45,
                    interval: 2,
                    labelAutoFit: true,
                    labelMaxWidth: 80,
                    labelFontSize: 10
                },
                axisY: { suffix: "%", labelFontSize: 10 },
                data: [{
                    type: "column",
                    showInLegend: false,
                    dataPoints: coloredDataPoints,
                    dataPointWidth: 10,
                    click: function(e) {
                        console.log('Chart column clicked:', e.dataPoint.label);
                        const date = e.dataPoint.label;
                        IndexDashboard.showIndicatorTable(date);
                    }
                }]
            };
            
            $(containerId).resizable({
                create() {
                    $(chartId).CanvasJSChart(options);
                    const chart = $(chartId).CanvasJSChart();
                    setTimeout(() => chart.render(), 0);
                    setTimeout(() => chart.render(), 150);
                },
                resize() {
                    $(chartId).CanvasJSChart().render();
                }
            });
        },
        
        createPmLineChart(containerSelector, chartSelector, data) {
            if (!data?.length) {
                document.querySelector(chartSelector).innerHTML = '<p>Нет данных для отображения</p>';
                return;
            }
            
            const parse = key => data.map(x => ({ 
                label: x.production_day, 
                y: Number(x[key]) || 0 
            }));
            
            const totalPlan = data.reduce((s, x) => s + (Number(x.plan) || 0), 0);
            const totalFact = data.reduce((s, x) => s + (Number(x.fact) || 0), 0);
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
                axisX: { title: "Дата", labelAngle: -45, margin: 10, labelFontSize: 10 },
                axisY: { title: "Количество", includeZero: true, margin: 10, labelFontSize: 10 },
                legend: { verticalAlign: "bottom" },
                data: [
                    { type: "line", name: "Plan", showInLegend: true, color: "#e31a1c", markerSize: 4, dataPoints: parse('plan') },
                    { type: "line", name: "Fact", showInLegend: true, color: "#33a02c", markerSize: 4, dataPoints: parse('fact') },
                    { type: "line", name: "Tag", showInLegend: true, color: "#1f78b4", markerSize: 4, dataPoints: parse('tag') }
                ]
            };
            
            $(containerSelector).resizable({
                create() {
                    $(chartSelector).CanvasJSChart(options);
                    const chart = $(chartSelector).CanvasJSChart();
                    setTimeout(() => chart.render(), 0);
                    setTimeout(() => chart.render(), 150);
                },
                resize() { $(chartSelector).CanvasJSChart().render(); }
            });
        }
    },

    // Создание таблиц
    Tables: {
        createMetrics(metricsData) {
            if (!metricsData) {
                document.getElementById('metricsTable').innerHTML = '<p>Нет данных для отображения</p>';
                return;
            }
            
            const prevDate = new Date(Date.now() - 24 * 60 * 60 * 1000);
            const prevDay = prevDate.toLocaleDateString('ru-RU');
            const currentMonth = prevDate.toLocaleDateString('ru-RU', { month: 'long' });
            
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
            // Загрузка данных для графиков
            const [breakDownData, availabilityData, currentMetrics, topBreakdownsWeek, topBreakdownsWeekKeyLines, pmData] = await Promise.all([
                DashboardAPI.fetchData('/dashboard/breakDown'),
                DashboardAPI.fetchData('/dashboard/availability'),
                DashboardAPI.fetchCurrentMetrics(),
                DashboardAPI.fetchData('/dashboard/top-breakdowns-week'),
                DashboardAPI.fetchData('/dashboard/top-breakdowns-week-key-lines'),
                DashboardAPI.fetchData('/dashboard/pm-plan-fact-tag')
            ]);
            
            // Подготовка данных для графиков
            const breakDownPoints = breakDownData?.map(item => ({
                label: item.production_day,
                y: item.downtime_percentage ? Number(item.downtime_percentage) : null
            })) || [];
            
            const availabilityPoints = availabilityData?.map(item => ({
                label: item.production_day,
                y: item.availability ? Number(item.availability) : null
            })) || [];
            
            // Создание графиков и таблиц
            this.Charts.create("#resizable1", "#breakDown", "BreakDown %", breakDownPoints, "breakDown");
            this.Charts.create("#resizable2", "#availability", "Availability %", availabilityPoints, "availability");
            this.Charts.createPmLineChart('#resizablePm', '#pmChart', pmData);
            
            this.Tables.createMetrics(currentMetrics);
            this.Tables.createTopBreakdowns(topBreakdownsWeek, 'topBreakdownsWeekTable', true);
            this.Tables.createTopBreakdowns(topBreakdownsWeekKeyLines, 'topBreakdownsWeekKeyLinesTable', true);
            
        } catch (error) {
            console.error('Ошибка при инициализации дашборда:', error);
        }
    }
};

// Инициализация при загрузке страницы
window.onload = async function() {
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