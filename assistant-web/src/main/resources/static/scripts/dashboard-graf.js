// Specific functions for the graf dashboard page (dashboard_graf.html)
const GrafDashboard = {
    initialized: false,

    // Инициализация дашборда
    async initializeDashboard() {
        if (this.initialized) return;
        this.initialized = true;

        await DashboardCharts.loadOnlineCharts();
        
        // Настройка кнопок "Отключить все"
        this.setupToggleAllButtons();
        
        // Обновление графиков каждые 60 секунд
        DashboardInit.startOnlineChartsRefresh(60000);
        
        // Полное обновление страницы каждый час
        DashboardInit.startPageRefresh(3600000); // 1 час = 3600000 мс

        this.setupResponsiveRerender();
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
    },

    setupResponsiveRerender() {
        const rerender = () => {
            ['#breakDown', '#availabilityOnline'].forEach(selector => {
                try {
                    const chart = $(selector).CanvasJSChart();
                    chart?.render();
                } catch (e) {
                    // chart not ready yet
                }
            });
        };

        let resizeTimer = null;
        const scheduleRerender = () => {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(rerender, 150);
        };

        window.addEventListener('resize', scheduleRerender);

        if (window.visualViewport) {
            window.visualViewport.addEventListener('resize', scheduleRerender);
        }

        const watchedContainers = [
            document.getElementById('resizable_breakDownOnline'),
            document.getElementById('resizable_availabilityOnline'),
            document.querySelector('.graf-dashboard-stack')
        ].filter(Boolean);

        if (window.ResizeObserver && watchedContainers.length) {
            const observer = new ResizeObserver(() => scheduleRerender());
            watchedContainers.forEach(el => observer.observe(el));
        }

        // Allow layout to stabilize before initial render.
        setTimeout(rerender, 150);
        setTimeout(rerender, 400);
    }
};

// Initialize the dashboard when the page loads
window.onload = async function() {
    await GrafDashboard.initializeDashboard();
};