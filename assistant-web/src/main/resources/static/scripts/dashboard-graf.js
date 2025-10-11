// Specific functions for the graf dashboard page (dashboard_graf.html)
const GrafDashboard = {
    // Инициализация дашборда
    async initializeDashboard() {
        await DashboardCharts.loadOnlineCharts();
        
        // Обновление графиков каждые 60 секунд
        DashboardInit.startOnlineChartsRefresh(60000);
        
        // Полное обновление страницы каждый час
        DashboardInit.startPageRefresh(3600000); // 1 час = 3600000 мс
    }
};

// Initialize the dashboard when the page loads
window.onload = async function() {
    await GrafDashboard.initializeDashboard();
};