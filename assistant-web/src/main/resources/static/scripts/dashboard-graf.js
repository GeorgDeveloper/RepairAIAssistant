// Specific functions for the graf dashboard page (dashboard_graf.html)
const GrafDashboard = {
    // Инициализация дашборда
    async initializeDashboard() {
        await DashboardCharts.loadOnlineCharts();
        
        // Настройка кнопок "Отключить все"
        this.setupToggleAllButtons();
        
        // Обновление графиков каждые 60 секунд
        DashboardInit.startOnlineChartsRefresh(60000);
        
        // Полное обновление страницы каждый час
        DashboardInit.startPageRefresh(3600000); // 1 час = 3600000 мс
    },
    
    setupToggleAllButtons() {
        const toggleBDButton = document.getElementById('toggleAllBD');
        const toggleAvailabilityButton = document.getElementById('toggleAllAvailability');
        
        if (toggleBDButton) {
            toggleBDButton.addEventListener('click', () => {
                this.toggleAllSeries('#breakDown', toggleBDButton);
            });
        }
        
        if (toggleAvailabilityButton) {
            toggleAvailabilityButton.addEventListener('click', () => {
                this.toggleAllSeries('#availabilityOnline', toggleAvailabilityButton);
            });
        }
    },
    
    toggleAllSeries(chartId, button) {
        try {
            const chart = $(chartId).CanvasJSChart();
            if (!chart || !chart.options || !chart.options.data) return;
            
            const allHidden = chart.options.data.every(series => series.visible === false);
            
            chart.options.data.forEach(series => {
                series.visible = allHidden;
            });
            
            chart.render();
            button.textContent = allHidden ? 'Отключить все' : 'Включить все';
        } catch (error) {
            console.error('Ошибка при переключении серий графика:', error);
        }
    }
};

// Initialize the dashboard when the page loads
window.onload = async function() {
    await GrafDashboard.initializeDashboard();
};

// Также инициализируем при готовности DOM
document.addEventListener('DOMContentLoaded', async function() {
    await GrafDashboard.initializeDashboard();
});