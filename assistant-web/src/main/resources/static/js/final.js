$(function(){
    var monthsLimit = 12; // Значение по умолчанию

    function loadConfig(){
        $.getJSON('/final/config', function(config){
            monthsLimit = config.monthsLimit || 12;
        });
    }

    function loadFilters(callback){
        $.getJSON('/final/years', function(years){
            var yopts = years.map(function(y){ return '<option value="'+ y.year +'">'+ y.year +'</option>'; }).join('');
            $('#yearSelect').html(yopts).val(new Date().getFullYear());
            refreshMonths(callback);
        });
    }

    function refreshMonths(callback){
        var ys = $('#yearSelect').val();
        var yearsParam = Array.isArray(ys) ? ys : (ys ? [ys] : []);
        var url = '/final/months' + (yearsParam.length? ('?'+ yearsParam.map(function(y){return 'year='+ encodeURIComponent(y)}).join('&')) : '');
        $.getJSON(url, function(months){
            var names = ['','Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];
            var mopts = months.map(function(m){ return '<option value="'+ m.month +'">'+ names[m.month] +'</option>'; }).join('');
            $('#monthSelect').html(mopts);
            var currentYear = new Date().getFullYear();
            var selectedYear = $('#yearSelect').val();
            if (selectedYear == currentYear.toString()) {
                // Для текущего года выбираем все доступные месяцы
                var monthValues = months.map(function(m){ return m.month; });
                $('#monthSelect').val(monthValues);
            } else {
                // Для других лет выбираем все месяцы
                var monthValues = months.map(function(m){ return m.month; });
                $('#monthSelect').val(monthValues);
            }
            if (callback) callback();
        });
    }

    function formatNumber(value, metricName) {
        if (value == null || value === '') return '';
        
        // Для процентных значений форматируем с точностью до сотых
        if (metricName.includes('%') || metricName.includes('Доступность') || metricName.includes('BD') || metricName.includes('Факт выполнения ппр')) {
            var num = parseFloat(value);
            if (!isNaN(num)) {
                return num.toFixed(2);
            }
        }
        
        // Для остальных значений возвращаем как есть
        return value;
    }

    function calculateYearTotal(rows, metricName, maxCols) {
        // Для строки "Месяц" возвращаем пустую строку
        if (metricName === 'Месяц') {
            return '';
        }
        
        var row = rows.find(function(r) { return r.metric === metricName; });
        if (!row) return '';
        
        var values = [];
        for (var i = 1; i <= maxCols; i++) {
            var value = row['m' + i];
            if (value != null && value !== '') {
                var num = parseFloat(value);
                if (!isNaN(num)) {
                    values.push(num);
                }
            }
        }
        
        if (values.length === 0) return '';
        
        // Для процентных показателей вычисляем среднее
        if (metricName.includes('%') || metricName.includes('Доступность') || metricName.includes('BD') || metricName.includes('Факт выполнения ппр')) {
            var sum = values.reduce(function(a, b) { return a + b; }, 0);
            var avg = sum / values.length;
            return avg.toFixed(2);
        }
        
        // Для счетчиков суммируем
        var total = values.reduce(function(a, b) { return a + b; }, 0);
        return total;
    }

    function renderTable(rows){
        // rows: [{metric, m1..mN}]
        var head = '<tr><th>Показатель</th><th>Итог за год</th>';
        var maxCols = 0;
        rows.forEach(function(r){
            var cols = Object.keys(r).filter(function(k){return /^m\d+$/.test(k)}).length; maxCols=Math.max(maxCols, cols);
        });
        for (var i=1;i<=maxCols;i++){ head += '<th class="month-col" data-col="m'+i+'"></th>'; }
        head += '</tr>';
        $('#summaryHead').html(head);

        var body = rows.map(function(r){
            var yearTotal = calculateYearTotal(rows, r.metric, maxCols);
            var row = '<tr><th>'+ r.metric +'</th>';
            row += '<td>' + (yearTotal !== '' ? yearTotal : '') + '</td>';
            for (var i=1;i<=maxCols;i++){ 
                var value = r['m'+i];
                var formattedValue = formatNumber(value, r.metric);
                row += '<td>' + (formattedValue !== '' ? formattedValue : '') + '</td>'; 
            }
            row += '</tr>';
            return row;
        }).join('');
        $('#summaryBody').html(body);
    }

    function updateHeadMonths(rows){
        var monthsRow = rows.find(function(r){ return r.metric === 'Месяц'; }) || { };
        $('#summaryHead th.month-col').each(function(){
            var col = $(this).data('col');
            $(this).text(monthsRow[col] || '');
        });
    }

    function serializeMulti(name, values){
        if (!values || values.length===0) return '';
        return values.map(function(v){return name+'='+ encodeURIComponent(v)}).join('&');
    }

    function loadData(){
        var yearsVal = $('#yearSelect').val();
        var monthsVal = $('#monthSelect').val();
        var years = Array.isArray(yearsVal) ? yearsVal : (yearsVal? [yearsVal] : []);
        var months = Array.isArray(monthsVal) ? monthsVal : (monthsVal? [monthsVal] : []);
        var parts = [];
        parts.push('limit=' + monthsLimit);
        var yq = serializeMulti('year', years);
        var mq = serializeMulti('month', months);
        if (yq) parts.push(yq);
        if (mq) parts.push(mq);
        var url = '/final/data' + (parts.length? ('?'+ parts.join('&')) : '');
        $.getJSON(url, function(rows){
            renderTable(rows);
            // updateHeadMonths(rows);
        });
    }

    $('#yearSelect').on('change', function(){ refreshMonths(); });
    $('#applyBtn').on('click', function(){ loadData(); });

    loadConfig();
    loadFilters(function(){
        // Загружаем данные только после того, как фильтры загружены и месяцы выбраны
    loadData();
    });
});