$(function(){
    function loadFilters(){
        $.getJSON('/final/years', function(years){
            var yopts = years.map(function(y){ return '<option value="'+ y.year +'">'+ y.year +'</option>'; }).join('');
            $('#yearSelect').html(yopts).val(new Date().getFullYear());
            refreshMonths();
        });
    }

    function refreshMonths(){
        var ys = $('#yearSelect').val();
        var yearsParam = Array.isArray(ys) ? ys : (ys ? [ys] : []);
        var url = '/final/months' + (yearsParam.length? ('?'+ yearsParam.map(function(y){return 'year='+ encodeURIComponent(y)}).join('&')) : '');
        $.getJSON(url, function(months){
            var names = ['','Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];
            var mopts = months.map(function(m){ return '<option value="'+ m.month +'">'+ names[m.month] +'</option>'; }).join('');
            $('#monthSelect').html(mopts);
            var current = new Date().getMonth()+1;
            if ($('#yearSelect').val() == new Date().getFullYear().toString()) {
                $('#monthSelect').val(current);
            }
        });
    }

    function formatNumber(value, metricName) {
        if (value == null || value === '') return '';
        
        // Для процентных значений форматируем с точностью до сотых
        if (metricName.includes('%') || metricName.includes('Доступность') || metricName.includes('BD') || metricName.includes('Плановые ремонты')) {
            var num = parseFloat(value);
            if (!isNaN(num)) {
                return num.toFixed(2);
            }
        }
        
        // Для остальных значений возвращаем как есть
        return value;
    }

    function renderTable(rows){
        // rows: [{metric, m1..mN}]
        var head = '<tr><th>Показатель</th>';
        var maxCols = 0;
        rows.forEach(function(r){
            var cols = Object.keys(r).filter(function(k){return /^m\d+$/.test(k)}).length; maxCols=Math.max(maxCols, cols);
        });
        for (var i=1;i<=maxCols;i++){ head += '<th class="month-col" data-col="m'+i+'"></th>'; }
        head += '</tr>';
        $('#summaryHead').html(head);

        var body = rows.map(function(r){
            var row = '<tr><th>'+ r.metric +'</th>';
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
        parts.push('limit=6');
        var yq = serializeMulti('year', years);
        var mq = serializeMulti('month', months);
        if (yq) parts.push(yq);
        if (mq) parts.push(mq);
        var url = '/final/data' + (parts.length? ('?'+ parts.join('&')) : '');
        $.getJSON(url, function(rows){
            renderTable(rows);
            updateHeadMonths(rows);
        });
    }

    $('#yearSelect').on('change', function(){ refreshMonths(); });
    $('#applyBtn').on('click', function(){ loadData(); });

    loadFilters();
    loadData();
});