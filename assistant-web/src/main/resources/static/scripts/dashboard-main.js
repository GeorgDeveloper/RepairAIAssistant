// Specific functions for the main dashboard page (dashboard.html)
const MainDashboard = {
    // Функция для создания таблицы с показателями
    createMetricsTable(bdData, availabilityData) {
        console.log('BD Data for metrics table:', bdData);
        console.log('Availability Data for metrics table:', availabilityData);
        const currentDate = new Date().toLocaleDateString('ru-RU');
        // Соответствие отображаемого названия области ключу area из БД
        const areasMap = [
            { label: 'Резиносмешение', key: 'NewMixingArea' },
            { label: 'Сборка 1', key: 'SemifinishingArea' },
            { label: 'Сборка 2', key: 'BuildingArea' },
            { label: 'Вулканизация', key: 'CuringArea' },
            { label: 'УЗО', key: 'FinishigArea' },
            { label: 'Модули', key: 'Modules' },
            { label: 'Завод', key: 'Plant' }
        ];

        // Получаем последние значения из данных графиков по ключу area
        const getCurrentValue = (data, areaKey) => {
            if (!data || !Array.isArray(data)) {
                console.log(`getCurrentValue: Нет данных для области ${areaKey}`);
                return 0;
            }
            const areaData = data.filter(item => item.area === areaKey);
            console.log(`getCurrentValue: Данные для области ${areaKey}:`, areaData);
            if (areaData.length === 0) {
                console.log(`getCurrentValue: Нет записей для области ${areaKey}`);
                return 0;
            }
            // Сортируем по timestamp и берем последнее значение
            const sortedData = areaData.sort((a, b) => {
                const timeA = a.timestamp ? new Date(a.timestamp) : new Date(0);
                const timeB = b.timestamp ? new Date(b.timestamp) : new Date(0);
                return timeA - timeB;
            });
            const last = sortedData[sortedData.length - 1];
            console.log(`getCurrentValue: Последняя запись для области ${areaKey}:`, last);
            const val = Number(last && last.value);
            console.log(`getCurrentValue: Значение для области ${areaKey}: ${val}`);
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

    // Таблица метрик по ключевым линиям (используем реальные данные из main_lines_online)
    async createKeyLinesMetricsTable() {
        const currentDate = new Date().toLocaleDateString('ru-RU');

        try {
            // Получаем текущие метрики ключевых линий
            const mainLinesData = await DashboardAPI.fetchData('/dashboard/main-lines/current');
            
            if (!mainLinesData || !Array.isArray(mainLinesData)) {
                console.warn('Нет данных о ключевых линиях');
                return;
            }

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

            mainLinesData.forEach(line => {
                const bdValue = Number(line.downtime_percentage) || 0;
                const availabilityValue = Number(line.availability) || 0;

                tableHTML += `
                <tr class="section-header">
                    <td colspan="3">${line.line_name || ''}</td>
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

        } catch (error) {
            console.error('Ошибка при загрузке данных ключевых линий:', error);
            const container = document.getElementById('metricsTable-lines');
            if (container) container.innerHTML = '<p>Ошибка загрузки данных ключевых линий</p>';
        }
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

    // Загрузка и отображение нарядов на работы
    async loadWorkOrdersTable() {
        try {
            console.log('Загружаем данные нарядов на работы...');
            // Используем новый API из assistant-base_update
            const workOrders = await DashboardAPI.fetchData('/api/work-orders/dashboard');
            console.log('Получены данные нарядов:', workOrders);
            DashboardTables.createWorkOrdersTable(workOrders, 'workOrdersTable');
        } catch (error) {
            console.error('Ошибка при загрузке нарядов на работы:', error);
            const container = document.getElementById('workOrdersTable');
            if (container) container.innerHTML = '<p>Ошибка загрузки данных нарядов</p>';
        }
    },

    // Инициализация дашборда
    async initializeDashboard() {
        await this.loadTopBreakdownsTables();
        const { bdData, availabilityData } = await DashboardCharts.loadOnlineCharts();
        
        if (bdData || availabilityData) {
            // Обновляем таблицу с текущими данными
            this.createMetricsTable(bdData, availabilityData);
        }
        
        // Загружаем данные ключевых линий
        await this.createKeyLinesMetricsTable();
        
        // Загружаем таблицу нарядов на работы
        await this.loadWorkOrdersTable();
        
        // Обновление графиков и блиц-панели каждые 60 секунд
        DashboardInit.startOnlineChartsRefresh(60000);
        
        // Обновление таблиц топ поломок каждые 5 минут
        setInterval(async () => {
            await this.loadTopBreakdownsTables();
        }, 300000); // 5 минут = 300000 мс
        
        // Обновление таблицы ключевых линий каждые 3 минуты
        setInterval(async () => {
            await this.createKeyLinesMetricsTable();
        }, 180000); // 3 минуты = 180000 мс
        
        // Обновление таблицы показателей участков (BD и Availability) каждые 3 минуты
        setInterval(async () => {
            try {
                console.log('Начинаем обновление таблицы показателей участков...');
                const { bdData, availabilityData } = await DashboardCharts.loadOnlineCharts();
                console.log('Получены данные BD:', bdData);
                console.log('Получены данные Availability:', availabilityData);
                if (bdData || availabilityData) {
                    this.createMetricsTable(bdData, availabilityData);
                    console.log('Таблица показателей участков обновлена успешно');
                } else {
                    console.log('Нет данных для обновления таблицы показателей участков');
                }
            } catch (error) {
                console.error('Ошибка при обновлении таблицы показателей участков:', error);
            }
        }, 180000); // 3 минуты = 180000 мс
        
        // Обновление таблицы нарядов каждые 2 минуты
        setInterval(async () => {
            await this.loadWorkOrdersTable();
        }, 120000); // 2 минуты = 120000 мс
        
        // Полное обновление страницы каждый час
        DashboardInit.startPageRefresh(3600000); // 1 час = 3600000 мс
    }
};

// Initialize the dashboard when the page loads
window.onload = async function() {
    await MainDashboard.initializeDashboard();
};