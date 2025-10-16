// Common API functions used across dashboard pages
const DashboardAPI = {
    async fetchData(url) {
        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Ошибка при получении данных:', error);
            return null;
        }
    },
    
    async fetchCurrentMetrics() {
        return this.fetchData('/dashboard/current-metrics');
    }
};

// Utility functions used across dashboard pages
const DashboardUtils = {
    filterEmptyData(dataPoints) {
        return dataPoints?.filter(point => 
            point.y !== null && point.y !== undefined && point.y !== 0
        ) || [];
    },
    
    getColorForPoint(value, type) {
        if (type === "breakDown" && value > 2) return "#dc3545";
        if (type === "availability" && value < 97) return "#dc3545";
        return "#007bff";
    },
    
    formatSecondsToHHMMSS(totalSeconds) {
        const s = Number(totalSeconds || 0);
        const h = Math.floor(s / 3600);
        const m = Math.floor((s % 3600) / 60);
        const sec = Math.floor(s % 60);
        const pad = n => String(n).padStart(2, '0');
        return `${pad(h)}:${pad(m)}:${pad(sec)}`;
    },
    
    // Фиксированная цветовая схема для участков
    areaColors: {
        'NewMixingArea': '#001522',      // Красный
        'SemifinishingArea': '#0400fa',  // Бирюзовый
        'BuildingArea': '#fa6000',       // Голубой
        'CuringArea': '#418b69',         // Зеленый
        'FinishingArea': '#ffd241',      // Желтый
        'Modules': '#c255c2',            // Фиолетовый
        'Plant': '#ff0505'      // Красный
    }
};

// Common chart functions used across dashboard pages
const DashboardCharts = {
    createChart(containerId, chartId, title, dataPoints, yAxisSuffix = "%") {
        const options = {
            animationEnabled: true,
            title: { text: title },
            axisX: {
                title: "Время",
                labelAngle: -45
            },
            axisY: {
                title: "Значение",
                suffix: yAxisSuffix
            },
            data: dataPoints,
            legend: {
                cursor: "pointer",
                itemclick: function(e) {
                    if (typeof(e.dataSeries.visible) === "undefined" || e.dataSeries.visible) {
                        e.dataSeries.visible = false;
                    } else {
                        e.dataSeries.visible = true;
                    }
                    e.chart.render();
                }
            }
        };

        // Если график уже инициализирован, обновляем данные и перерисовываем
        try {
            const chart = $(chartId).CanvasJSChart();
            if (chart && chart.render) {
                chart.options.title = options.title;
                chart.options.axisX = options.axisX;
                chart.options.axisY = options.axisY;
                chart.options.data = options.data;
                chart.render();
                return;
            }
        } catch (e) { /* игнорируем инициализацию при первом запуске */ }

        // Инициализация при первом создании
        $(containerId).resizable({
            create: function() {
                $(chartId).CanvasJSChart(options);
            },
            resize: function() {
                $(chartId).CanvasJSChart().render();
            }
        });
    },
    
    processChartData(rawData) {
        const groupedData = {};
        
        (rawData || []).forEach(item => {
            if (!groupedData[item.area]) {
                groupedData[item.area] = [];
            }
            groupedData[item.area].push({
                label: item.timestamp,
                y: item.value
            });
        });

        return Object.keys(groupedData).map(area => ({
            type: "line",
            name: area,
            showInLegend: true,
            color: DashboardUtils.areaColors[area] || '#666666', // Фиксированный цвет для участка
            dataPoints: groupedData[area]
        }));
    },
    
    // Загрузка графиков BD и доступности
    async loadOnlineCharts() {
        try {
            const bdData = await DashboardAPI.fetchData('/dashboard/online/bd');
            const availabilityData = await DashboardAPI.fetchData('/dashboard/online/availability');

            if (bdData) {
                const bdChartData = this.processChartData(bdData);
                this.createChart("#resizable_breakDownOnline", "#breakDown", "BD % (онлайн)", bdChartData);
            }

            if (availabilityData) {
                const availabilityChartData = this.processChartData(availabilityData);
                this.createChart("#resizable_availabilityOnline", "#availabilityOnline", "Доступность % (онлайн)", availabilityChartData);
            }
            
            return { bdData, availabilityData };
        } catch (error) {
            console.error('Ошибка при загрузке онлайн графиков:', error);
            return { bdData: null, availabilityData: null };
        }
    }
};

// Common table functions used across dashboard pages
const DashboardTables = {
    createTopBreakdownsTable(data, containerId, isWeekly = false) {
        if (!data || !Array.isArray(data) || data.length === 0) {
            document.getElementById(containerId).innerHTML = '<p>Нет данных для отображения</p>';
            return;
        }
        let tableHTML = `<table class="metrics-table">
            <thead>
                <tr>`;
        if (isWeekly) {
            tableHTML += '<th>machine_name</th><th>machine_downtime</th>';
        } else {
            tableHTML += '<th>code</th><th>machine_name</th><th>machine_downtime</th><th>cause</th>';
        }
        tableHTML += `</tr>
            </thead>
            <tbody>`;
        data.forEach(row => {
            tableHTML += '<tr>';
            if (isWeekly) {
                // Форматируем время для недельных таблиц
                const formatSeconds = (s) => {
                    if (s == null) return '';
                    const total = Number(s) || 0;
                    const h = Math.floor(total / 3600);
                    const m = Math.floor((total % 3600) / 60);
                    const sec = Math.floor(total % 60);
                    return `${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${sec.toString().padStart(2,'0')}`;
                };
                const downtime = (row.machine_downtime_seconds != null) ? formatSeconds(row.machine_downtime_seconds) : (row.machine_downtime || '');
                tableHTML += `<td>${row.machine_name || ''}</td><td>${downtime}</td>`;
            } else {
                tableHTML += `<td>${row.code || ''}</td><td>${row.machine_name || ''}</td><td>${row.machine_downtime || ''}</td><td>${row.cause || ''}</td>`;
            }
            tableHTML += '</tr>';
        });
        tableHTML += '</tbody></table>';
        document.getElementById(containerId).innerHTML = tableHTML;
    }
};

// Dashboard initialization functions
const DashboardInit = {
    async initializeDashboard() {
        // This function will be implemented differently for each page
        console.log('Dashboard initialization function needs to be implemented for each page');
    },
    
    // Обновление графиков и блиц-панели каждые 60 секунд
    startOnlineChartsRefresh(interval = 60000) {
        return setInterval(async () => {
            await DashboardCharts.loadOnlineCharts();
        }, interval);
    },
    
    // Полное обновление страницы каждый час
    startPageRefresh(interval = 3600000) {
        return setInterval(() => {
            window.location.reload();
        }, interval);
    }
};