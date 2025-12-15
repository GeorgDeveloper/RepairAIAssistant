function formatDateTime(value) {
    if (!value) return '';
    const d = new Date(value);
    if (isNaN(d.getTime())) return value;
    const pad = (n) => String(n).padStart(2,'0');
    return `${pad(d.getDate())}.${pad(d.getMonth()+1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function formatDetailsRow(d) {
    var rows = Object.entries(d || {}).map(function(entry){
        var key = entry[0];
        var value = entry[1];
        if (value === null || value === undefined) { value = ''; }
        // Форматируем даты в подробной информации
        if (key.includes('_t') || key === 'created_at' || key === 'date') {
            value = formatDateTime(value);
        }
        return '<tr><th>'+ key +'</th><td>'+ value +'</td></tr>';
    }).join('');
    return '<table class="details-table">' + rows + '</table>';
}

$(document).ready(function () {

    // Устанавливаем даты по умолчанию (последняя неделя)
    function setDefaultDates() {
        const today = new Date();
        const weekAgo = new Date(today);
        weekAgo.setDate(today.getDate() - 7);
        
        const formatDate = (date) => {
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        };
        
        // Устанавливаем значения только если поля пустые
        if (!$('#dateFrom').val()) {
            $('#dateFrom').val(formatDate(weekAgo));
        }
        if (!$('#dateTo').val()) {
            $('#dateTo').val(formatDate(today));
        }
    }
    
    // Устанавливаем даты по умолчанию перед инициализацией таблицы
    setDefaultDates();

    var table = $('#failuresTable').DataTable({
        ajax: { 
            url: '/dashboard/equipment-maintenance-records',
            data: function(d) {
                // Получаем значения или используем значения по умолчанию
                let dateFrom = $('#dateFrom').val();
                let dateTo = $('#dateTo').val();
                
                // Если поля пустые, устанавливаем значения по умолчанию
                if (!dateFrom || !dateTo) {
                    const today = new Date();
                    const weekAgo = new Date(today);
                    weekAgo.setDate(today.getDate() - 7);
                    
                    const formatDate = (date) => {
                        const year = date.getFullYear();
                        const month = String(date.getMonth() + 1).padStart(2, '0');
                        const day = String(date.getDate()).padStart(2, '0');
                        return `${year}-${month}-${day}`;
                    };
                    
                    if (!dateFrom) {
                        dateFrom = formatDate(weekAgo);
                        $('#dateFrom').val(dateFrom);
                    }
                    if (!dateTo) {
                        dateTo = formatDate(today);
                        $('#dateTo').val(dateTo);
                    }
                }
                
                const area = $('#areaSelect').val();
                
                if (dateFrom) {
                    d.dateFrom = dateFrom;
                }
                if (dateTo) {
                    d.dateTo = dateTo;
                }
                if (area && area !== 'all') {
                    d.area = area;
                }
            },
            dataSrc: function(json) {
                return json || [];
            }
        },
        autoWidth: false,
        columns: [
            { className: 'details-control', orderable: false, data: null, defaultContent: '', width: '24px' },
            { data: 'id', title: 'ID', width: '60px' },
            { data: 'mechanism_node', title: 'Узел механизма', width: '150px' },
            { data: 'additional_kit', title: 'Доп. комплект', width: '120px' },
            { data: 'description', title: 'Описание', width: '200px' },
            { data: 'code', title: 'Код', width: '80px' },
            { data: 'hp_bd', title: 'HP BD', width: '80px' },
            { data: 'start_bd_t1', title: 'Начало BD', width: '140px', render: function(d){ return formatDateTime(d); } },
            { data: 'start_maint_t2', title: 'Начало ремонта', width: '140px', render: function(d){ return formatDateTime(d); } },
            { data: 'stop_maint_t3', title: 'Окончание ремонта', width: '140px', render: function(d){ return formatDateTime(d); } },
            { data: 'stop_bd_t4', title: 'Окончание BD', width: '140px', render: function(d){ return formatDateTime(d); } },
            { data: 'machine_downtime', title: 'Время простоя', width: '100px' },
            { data: 'ttr', title: 'TTR', width: '80px' },
            { data: 't2_minus_t1', title: 'T2-T1', width: '80px' },
            { data: 'status', title: 'Статус', width: '100px' },
            { data: 'maintainers', title: 'Сервисники', width: '150px' },
            { data: 'comments', title: 'Комментарии', width: '200px' },
            { data: 'cause', title: 'Причина', width: '150px' },
            { data: 'failure_type', title: 'Тип поломки', width: '120px' },
            { data: 'area', title: 'Зона', width: '120px' },
            { data: 'created_at', title: 'Создано', width: '140px', render: function(d){ return formatDateTime(d); } },
            { data: 'date', title: 'Дата', width: '120px', render: function(d){ return formatDateTime(d).split(' ')[0]; } },
            { data: 'week_number', title: 'Неделя', width: '90px' },
            { data: 'month_name', title: 'Месяц', width: '120px' },
            { data: 'shift', title: 'Смена', width: '80px' },
            { data: 'staff', title: 'Персонал', width: '100px' },
            { data: 'crew', title: 'Бригада', width: '100px' },
            { data: 'crew_de_facto', title: 'Бригада де-факто', width: '120px' },
            { data: 'production_day', title: 'Производственный день', width: '150px' }
        ],
        scrollX: true,
        scrollY: 'calc(60vh - 80px)',
        scrollCollapse: true,
        paging: true,
        pagingType: 'full_numbers',
        dom: 'lfrtip',
        pageLength: 25,
        lengthMenu: [[10, 25, 50, 100, 250, 500], [10, 25, 50, 100, 250, 500]],
        processing: true,
        deferRender: true,
        language: {
            search: "Поиск:",
            lengthMenu: "Показать _MENU_ записей",
            info: "Показано с _START_ по _END_ из _TOTAL_ записей",
            infoEmpty: "Нет записей для отображения",
            infoFiltered: "(отфильтровано из _MAX_ записей)",
            paginate: { 
                first: "Первая", 
                last: "Последняя", 
                next: "Следующая", 
                previous: "Предыдущая" 
            },
            processing: "Загрузка данных...",
            emptyTable: "Нет данных для отображения",
            zeroRecords: "Записи не найдены"
        },
        order: [[1, 'desc']],
        columnDefs: [
            {
                targets: [4, 15, 16],
                render: function(data, type, row) {
                    if (type === 'display' && data && data.length > 50) {
                        return data.substr(0, 50) + '...';
                    }
                    return data;
                }
            }
        ]
    });

    // Заполнение фильтра участков после загрузки данных
    table.on('xhr', function(){
        const json = table.ajax.json() || [];
        const areas = Array.from(new Set(json.map(r => r.area).filter(Boolean))).sort();
        $('#areaSelect').html('<option value="all">Все</option>' + areas.map(a=>`<option value="${a}">${a}</option>`).join(''));
    });

    // Функция загрузки данных с фильтрацией
    function loadFilteredData() {
        table.ajax.reload();
    }

    $('#loadData').on('click', loadFilteredData);

    // Recalculate column widths to ensure horizontal scroll is applied
    table.columns.adjust();
    $(window).on('resize', function(){
        table.columns.adjust();
    });

    $('#failuresTable tbody').on('click', 'td.details-control', function () {
        var tr = $(this).closest('tr');
        var row = table.row(tr);
        if (row.child.isShown()) {
            row.child.hide();
            tr.removeClass('shown');
        } else {
            row.child(formatDetailsRow(row.data())).show();
            tr.addClass('shown');
        }
    });
});