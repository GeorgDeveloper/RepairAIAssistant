(() => {
    const TABLE_MIN_WIDTH_DESKTOP = 760;
    const TABLE_MIN_WIDTH_TABLET = 680;
    const TABLE_MIN_WIDTH_COMPACT = 600;

    function getTableMinWidth() {
        if (window.innerWidth <= 768) return TABLE_MIN_WIDTH_COMPACT;
        if (window.innerWidth <= 1024) return TABLE_MIN_WIDTH_TABLET;
        return TABLE_MIN_WIDTH_DESKTOP;
    }

    function shouldForceMinWidth(table) {
        const headCols = table.querySelectorAll('thead th').length;
        if (headCols >= 8) return true;

        const firstRowCols = table.querySelectorAll('tr:first-child th, tr:first-child td').length;
        return firstRowCols >= 8;
    }

    function wrapTable(table) {
        const parent = table.parentElement;
        if (!parent) return;
        if (parent.classList.contains('table-responsive')) return;
        if (table.closest('.table-responsive')) return;

        const wrapper = document.createElement('div');
        wrapper.className = 'table-responsive';
        parent.insertBefore(wrapper, table);
        wrapper.appendChild(table);
    }

    function updateTableWidths(root = document) {
        const minWidth = `${getTableMinWidth()}px`;
        root.querySelectorAll('table').forEach(table => {
            wrapTable(table);
            if (shouldForceMinWidth(table)) {
                table.style.minWidth = minWidth;
            } else {
                table.style.minWidth = '';
            }
            table.style.width = '100%';
        });
    }

    function rerenderCanvasCharts() {
        if (!window.jQuery) return;
        const selectors = [
            '#breakDown',
            '#availability',
            '#pmChart',
            '#availabilityOnline',
            '#breakDownOnline'
        ];

        selectors.forEach(selector => {
            try {
                const chart = window.jQuery(selector).CanvasJSChart();
                chart?.render?.();
            } catch (e) {
                // Ignore missing chart containers on unrelated pages.
            }
        });
    }

    function applyGlobalAdaptiveOverrides() {
        const styleId = 'fluid-layout-global-overrides';
        if (document.getElementById(styleId)) return;

        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            .total-content,
            .container,
            .dashboard-grid,
            .dashboard-grid-index,
            .home-grid,
            .stack {
                height: auto !important;
                min-height: 0 !important;
            }

            .main-content {
                width: 100% !important;
                height: auto !important;
                min-height: calc(100vh - 5rem) !important;
                overflow-y: visible !important;
                padding-bottom: 0.5rem !important;
            }

            body.breakdowns-screen .main-content {
                height: calc(100vh - 5rem) !important;
                min-height: calc(100vh - 5rem) !important;
                overflow-y: auto !important;
            }

            body.breakdowns-screen .container {
                height: 100%;
                min-height: 0;
            }

            .table-container {
                max-height: none !important;
            }

            .table-responsive > table,
            .table-container > table,
            table.metrics-table,
            table.current-status,
            table.work-orders-table {
                min-width: 0 !important;
                width: 100% !important;
                table-layout: auto !important;
            }
        `;
        document.head.appendChild(style);
    }

    function init() {
        applyGlobalAdaptiveOverrides();
        updateTableWidths(document);

        const observer = new MutationObserver(mutations => {
            for (const mutation of mutations) {
                if (mutation.type !== 'childList') continue;
                mutation.addedNodes.forEach(node => {
                    if (!(node instanceof HTMLElement)) return;
                    if (node.tagName === 'TABLE') {
                        updateTableWidths(node.parentElement || document);
                    } else if (node.querySelector?.('table')) {
                        updateTableWidths(node);
                    }
                });
            }
        });

        observer.observe(document.body, { childList: true, subtree: true });

        let timer = null;
        window.addEventListener('resize', () => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                updateTableWidths(document);
                rerenderCanvasCharts();
            }, 150);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
