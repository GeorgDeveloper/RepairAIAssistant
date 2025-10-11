// Specific functions for the main dashboard page (dashboard.html)
const MainDashboard = {
    // Функция для создания таблицы с показателями
    createMetricsTable(bdData, availabilityData) {
        const currentDate = new Date().toLocaleDateString('ru-RU');
        // Соответствие отображаемого названия области ключу area из БД
        const areasMap = [
            { label: 'Резиносмешение', key: 'NewMixingArea' },
            { label: 'Сборка 1', key: 'SemifinishingArea' },
            { label: 'Сборка 2', key: 'BuildingArea' },
            { label: 'Вулканизация', key: 'CuringArea' },
            { label: 'УЗО', key: 'FinishingArea' },
            { label: 'Модули', key: 'Modules' },
            { label: 'Завод', key: 'Plant' }
        ];

        // Получаем последние значения из данных графиков по ключу area
        const getCurrentValue = (data, areaKey) => {
            if (!data || !Array.isArray(data)) return 0;
            const areaData = data.filter(item => item.area === areaKey);
            if (areaData.length === 0) return 0;
            const last = areaData[areaData.length - 1];
            const val = Number(last && last.value);
            return isNaN(val) ? 0 : val;
        };

        let tableHTML = `
        <table class="metrics-table">
            <thead>
                <tr>
                    <th>Показатель</th>
                    <th>Цель</th>
                    <th>${currentDate}</th>
                </tr>
            </thead>
            <tbody>
        `;

        areasMap.forEach(({ label, key }) => {
            const bdValue = getCurrentValue(bdData, key);
            const availabilityValue = getCurrentValue(availabilityData, key);
            
            tableHTML += `
            <tr class="section-header">
                <td colspan="3">${label}</td>
            </tr>
            <tr>
                <td>BD</td>
                <td>2%</td>
                <td class="${bdValue <= 2 ? 'target-met' : 'target-not-met'}">
                    ${bdValue.toFixed(2)}%
                </td>
            </tr>
            <tr>
                <td>Доступность</td>
                <td>97%</td>
                <td class="${availabilityValue >= 97 ? 'target-met' : 'target-not-met'}">
                    ${availabilityValue.toFixed(2)}%
                </td>
            </tr>
        `;
        });

        tableHTML += '</tbody></table>';
        document.getElementById('metricsTable').innerHTML = tableHTML;
    },

    // Таблица метрик по ключевым линиям (используем значения их участков)
    createKeyLinesMetricsTable(bdData, availabilityData) {
        const currentDate = new Date().toLocaleDateString('ru-RU');

        // Сопоставление ключевых линий с участками (area), чтобы подставить их текущие значения
        const keyLinesMap = [
            { label: 'Mixer GK 270 T-C 2.1', areaKey: 'NewMixingArea' },
            { label: 'Mixer GK 320 E 1.1', areaKey: 'NewMixingArea' },
            { label: 'Calender Complex Berstorf - 01', areaKey: 'SemifinishingArea' },
            { label: 'Bandina - 01', areaKey: 'SemifinishingArea' },
            { label: 'Duplex - 01', areaKey: 'SemifinishingArea' },
            { label: 'Calender Comerio Ercole - 01', areaKey: 'SemifinishingArea' },
            { label: 'VMI APEX - 01', areaKey: 'BuildingArea' },
            { label: 'VMI APEX - 02', areaKey: 'BuildingArea' },
            { label: 'Trafila Quadruplex - 01', areaKey: 'SemifinishingArea' },
            { label: 'Bartell Bead Machine - 01', areaKey: 'SemifinishingArea' },
            { label: 'TTM fisher belt cutting - 01', areaKey: 'FinishingArea' },
            { label: 'VMI TPCS 1600-1000', areaKey: 'BuildingArea' }
        ];

        const getCurrentValueByArea = (data, areaKey) => {
            if (!data || !Array.isArray(data)) return 0;
            const areaData = data.filter(item => item.area === areaKey);
            if (areaData.length === 0) return 0;
            const last = areaData[areaData.length - 1];
            const val = Number(last && last.value);
            return isNaN(val) ? 0 : val;
        };

        let tableHTML = `
        <table class="metrics-table">
            <thead>
                <tr>
                    <th>Ключевая линия</th>
                    <th>Цель</th>
                    <th>${currentDate}</th>
                </tr>
            </thead>
            <tbody>
        `;

        keyLinesMap.forEach(({ label, areaKey }) => {
            const bdValue = getCurrentValueByArea(bdData, areaKey);
            const availabilityValue = getCurrentValueByArea(availabilityData, areaKey);

            tableHTML += `
            <tr class="section-header">
                <td colspan="3">${label}</td>
            </tr>
            <tr>
                <td>BD</td>
                <td>2%</td>
                <td class="${bdValue <= 2 ? 'target-met' : 'target-not-met'}">${bdValue.toFixed(2)}%</td>
            </tr>
            <tr>
                <td>Доступность</td>
                <td>97%</td>
                <td class="${availabilityValue >= 97 ? 'target-met' : 'target-not-met'}">${availabilityValue.toFixed(2)}%</td>
            </tr>
        `;
        });

        tableHTML += '</tbody></table>';
        const container = document.getElementById('metricsTable-lines');
        if (container) container.innerHTML = tableHTML;
    },

    // Загрузка и отображение топ поломок
    async loadTopBreakdownsTables() {
        const topBreakdowns = await DashboardAPI.fetchData('/dashboard/top-breakdowns');
        DashboardTables.createTopBreakdownsTable(topBreakdowns, 'topBreakdownsTable');

        // Текущий ТОП (онлайн)
        const topCurrent = await DashboardAPI.fetchData('/dashboard/top-breakdowns-current');
        if (topCurrent && Array.isArray(topCurrent)) {
            let tableHTML = `<table class="metrics-table">
                <thead>
                    <tr><th>area</th><th>machine_name</th><th>machine_downtime</th><th>cause</th></tr>
                </thead>
                <tbody>`;
            const formatSeconds = (s) => {
                if (s == null) return '';
                const total = Number(s) || 0;
                const h = Math.floor(total / 3600);
                const m = Math.floor((total % 3600) / 60);
                const sec = Math.floor(total % 60);
                return `${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${sec.toString().padStart(2,'0')}`;
            };
            topCurrent.forEach(row => {
                const downtime = (row.machine_downtime_seconds != null) ? formatSeconds(row.machine_downtime_seconds) : (row.machine_downtime || '');
                tableHTML += `<tr><td>${row.area || ''}</td><td>${row.machine_name || ''}</td><td>${downtime}</td><td>-</td></tr>`;
            });
            tableHTML += '</tbody></table>';
            const currentContainer = document.getElementById('topBreakdownsCurrentTable');
            if (currentContainer) currentContainer.innerHTML = tableHTML;
        }

        // Добавляем в заголовки дату прошедших суток и текущую дату
        const yesterday = new Date(Date.now() - 24*60*60*1000).toLocaleDateString('ru-RU');
        const today = new Date().toLocaleDateString('ru-RU');
        const topBreakdownsHeader = document.getElementById('topBreakdownsHeader');
        if (topBreakdownsHeader) topBreakdownsHeader.textContent = `Топ поломок за сутки — ${yesterday}`;
        const currentSpan = document.getElementById('currentDateSpan');
        if (currentSpan) currentSpan.textContent = today;
    },

    // Инициализация дашборда
    async initializeDashboard() {
        await this.loadTopBreakdownsTables();
        const { bdData, availabilityData } = await DashboardCharts.loadOnlineCharts();
        
        if (bdData || availabilityData) {
            // Обновляем таблицу с текущими данными
            this.createMetricsTable(bdData, availabilityData);
            this.createKeyLinesMetricsTable(bdData, availabilityData);
        }
        
        // Обновление графиков и блиц-панели каждые 60 секунд
        DashboardInit.startOnlineChartsRefresh(60000);
        
        // Обновление таблиц топ поломок каждые 5 минут
        setInterval(async () => {
            await this.loadTopBreakdownsTables();
        }, 300000); // 5 минут = 300000 мс
        
        // Полное обновление страницы каждый час
        DashboardInit.startPageRefresh(3600000); // 1 час = 3600000 мс
    }
};

// Initialize the dashboard when the page loads
window.onload = async function() {
    await MainDashboard.initializeDashboard();
};