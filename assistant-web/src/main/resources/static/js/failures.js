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
            { className: 'details-control', orderable: false, data: null, defaultContent: '', width: '1.5rem' },
            { data: 'id', title: 'ID', width: '3.75rem' },
            { data: 'mechanism_node', title: 'Узел механизма', width: '9.375rem' },
            { data: 'additional_kit', title: 'Доп. комплект', width: '7.5rem' },
            { data: 'description', title: 'Описание', width: '12.5rem' },
            { data: 'code', title: 'Код', width: '5rem' },
            { data: 'hp_bd', title: 'HP BD', width: '5rem' },
            { data: 'start_bd_t1', title: 'Начало BD', width: '8.75rem', render: function(d){ return formatDateTime(d); } },
            { data: 'start_maint_t2', title: 'Начало ремонта', width: '8.75rem', render: function(d){ return formatDateTime(d); } },
            { data: 'stop_maint_t3', title: 'Окончание ремонта', width: '8.75rem', render: function(d){ return formatDateTime(d); } },
            { data: 'stop_bd_t4', title: 'Окончание BD', width: '8.75rem', render: function(d){ return formatDateTime(d); } },
            { data: 'machine_downtime', title: 'Время простоя', width: '6.25rem' },
            { data: 'ttr', title: 'TTR', width: '5rem' },
            { data: 't2_minus_t1', title: 'T2-T1', width: '5rem' },
            { data: 'status', title: 'Статус', width: '6.25rem' },
            { data: 'maintainers', title: 'Сервисники', width: '9.375rem' },
            { data: 'comments', title: 'Комментарии', width: '12.5rem' },
            { data: 'cause', title: 'Причина', width: '9.375rem' },
            { data: 'failure_type', title: 'Тип поломки', width: '7.5rem' },
            { data: 'area', title: 'Зона', width: '7.5rem' },
            { data: 'created_at', title: 'Создано', width: '8.75rem', render: function(d){ return formatDateTime(d); } },
            { data: 'date', title: 'Дата', width: '7.5rem', render: function(d){ return formatDateTime(d).split(' ')[0]; } },
            { data: 'week_number', title: 'Неделя', width: '5.625rem' },
            { data: 'month_name', title: 'Месяц', width: '7.5rem' },
            { data: 'shift', title: 'Смена', width: '5rem' },
            { data: 'staff', title: 'Персонал', width: '6.25rem' },
            { data: 'crew', title: 'Бригада', width: '6.25rem' },
            { data: 'crew_de_facto', title: 'Бригада де-факто', width: '7.5rem' },
            { data: 'production_day', title: 'Производственный день', width: '9.375rem' }
        ],
        scrollX: true,
        // Ограничиваем вертикальную прокрутку именно областью таблицы
        // (чтобы не скроллилась вся страница).
        scrollY: 'calc(100vh - 13rem)',
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