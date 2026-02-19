function formatDateTime(value) {
    if (!value) return '';
    const d = new Date(value);
    if (isNaN(d.getTime())) return value;
    const pad = (n) => String(n).padStart(2,'0');
    return `${pad(d.getDate())}.${pad(d.getMonth()+1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function formatDate(value) {
    if (!value) return '';
    const d = new Date(value);
    if (isNaN(d.getTime())) return value;
    const pad = (n) => String(n).padStart(2,'0');
    return `${pad(d.getDate())}.${pad(d.getMonth()+1)}.${d.getFullYear()}`;
}

function formatDetailsRow(d) {
    var rows = Object.entries(d || {}).map(function(entry){
        var key = entry[0];
        var value = entry[1];
        if (value === null || value === undefined) { value = ''; }
        // Форматируем даты в подробной информации
        if (key.includes('date') || key === 'created_at' || key === 'scheduled_proposed_date' || key === 'scheduled_date') {
            if (key.includes('_date') || key === 'scheduled_proposed_date' || key === 'scheduled_date') {
                value = formatDate(value);
            } else {
                value = formatDateTime(value);
            }
        }
        return '<tr><th>'+ key +'</th><td>'+ value +'</td></tr>';
    }).join('');
    return '<table class="details-table">' + rows + '</table>';
}

$(document).ready(function () {

    function getWeekNumber(dateObj) {
        const d = new Date(Date.UTC(dateObj.getFullYear(), dateObj.getMonth(), dateObj.getDate()))
        const dayNum = d.getUTCDay() || 7; // 1..7 (Mon..Sun)
        d.setUTCDate(d.getUTCDate() + 4 - dayNum);
        const yearStart = new Date(Date.UTC(d.getUTCFullYear(),0,1));
        const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1)/7);
        return weekNo;
    }

    var table = $('#pmTable').DataTable({
        ajax: { 
            url: '/dashboard/pm-maintenance-records', 
            dataSrc: function(json) {
                // Фильтруем по текущему году по умолчанию
                const currentYear = new Date().getFullYear();
                return json.filter(row => {
                    const dt = new Date(row.scheduled_date || row.scheduled_proposed_date || row.date_start_work_order);
                    return !isNaN(dt.getTime()) && dt.getFullYear() === currentYear;
                });
            }
        },
        autoWidth: false,
        columns: [
            { className: 'details-control', orderable: false, data: null, defaultContent: '', width: '24px' },
            { data: 'id', title: 'ID', width: '60px' },
            { data: 'machine_name', title: 'Машина', width: '150px' },
            { data: 'work_order_name', title: 'Название заказа', width: '150px' },
            { data: 'work_order_frequency', title: 'Частота заказа', width: '100px' },
            { data: 'typology', title: 'Типология', width: '120px' },
            { data: 'scheduled_proposed_date', title: 'Предложенная дата', width: '120px', render: function(d){ return formatDate(d); } },
            { data: 'scheduled_date', title: 'Запланированная дата', width: '120px', render: function(d){ return formatDate(d); } },
            { data: 'delta_scheduling_days', title: 'Дельта планирования', width: '120px' },
            { data: 'pm_report_delay_days', title: 'Задержка отчета ППР', width: '120px' },
            { data: 'date_start_work_order', title: 'Начало заказа', width: '140px', render: function(d){ return formatDateTime(d); } },
            { data: 'date_stop_work_order', title: 'Окончание заказа', width: '140px', render: function(d){ return formatDateTime(d); } },
            { data: 'preventive_maintenance_duration_min', title: 'Длительность ППР (мин)', width: '140px' },
            { data: 'wo_estimated_duration_min', title: 'Оценка длительности (мин)', width: '140px' },
            { data: 'wo_suspension_duration_min', title: 'Длительность приостановки (мин)', width: '160px' },
            { data: 'operations_nok', title: 'Операции NOK', width: '100px' },
            { data: 'operations_ok', title: 'Операции OK', width: '100px' },
            { data: 'operations_all', title: 'Все операции', width: '100px' },
            { data: 'status', title: 'Статус', width: '100px' },
            { data: 'maintainers', title: 'Сервисники', width: '150px' },
            { data: 'comment', title: 'Комментарий', width: '200px' },
            { data: 'created_at', title: 'Создано', width: '140px', render: function(d){ return formatDateTime(d); } },
            { data: 'year', title: 'Год', width: '80px' },
            { data: 'month_number', title: 'Номер месяца', width: '100px' },
            { data: 'month_name', title: 'Месяц', width: '120px' },
            { data: 'week_number', title: 'Неделя', width: '90px' },
            { data: 'is_completed', title: 'Завершено', width: '100px', render: function(d){ return d ? 'Да' : 'Нет'; } },
            { data: 'area', title: 'Участок', width: '120px' }
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
                targets: [3, 19, 20],
                render: function(data, type, row) {
                    if (type === 'display' && data && data.length > 50) {
                        return data.substr(0, 50) + '...';
                    }
                    return data;
                }
            }
        ]
    });

    let allData = []; // Хранилище всех данных

    // Заполнение фильтров после загрузки данных
    table.on('xhr', function(){
        allData = table.ajax.json() || [];
        populateFilters();
        // Устанавливаем текущий год по умолчанию
        $('#yearSelect').val(new Date().getFullYear());
    });

    function populateFilters() {
        const data = allData;
        const areas = Array.from(new Set(data.map(r => r.area).filter(Boolean))).sort();
        const years = Array.from(new Set(data.map(r => { 
            const d = new Date(r.scheduled_date || r.scheduled_proposed_date || r.date_start_work_order); 
            return isNaN(new Date(d).getTime())? null : new Date(d).getFullYear(); 
        }).filter(Boolean))).sort((a,b)=>a-b);
        $('#areaSelect').html('<option value="all">Все</option>' + areas.map(a=>`<option value="${a}">${a}</option>`).join(''));
        $('#yearSelect').html('<option value="all">Все</option>' + years.map(y=>`<option value="${y}">${y}</option>`).join(''));
        // месяцы
        const monthNames = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];
        $('#monthSelect').html('<option value="all">Все</option>' + monthNames.map((n,i)=>`<option value="${i+1}">${n}</option>`).join(''));
        $('#weekSelect').html('<option value="all">Все</option>');
    }

    // Функция загрузки данных с фильтрацией
    function loadFilteredData() {
        const year = $('#yearSelect').val();
        const month = $('#monthSelect').val();
        const week = $('#weekSelect').val();
        const area = $('#areaSelect').val();

        let filteredData = allData;

        if (year !== 'all') {
            filteredData = filteredData.filter(row => {
                const dt = new Date(row.scheduled_date || row.scheduled_proposed_date || row.date_start_work_order);
                return !isNaN(dt.getTime()) && dt.getFullYear() === Number(year);
            });
        }

        if (month !== 'all') {
            filteredData = filteredData.filter(row => {
                const dt = new Date(row.scheduled_date || row.scheduled_proposed_date || row.date_start_work_order);
                return !isNaN(dt.getTime()) && (dt.getMonth() + 1) === Number(month);
            });
        }

        if (week !== 'all') {
            filteredData = filteredData.filter(row => {
                const dt = new Date(row.scheduled_date || row.scheduled_proposed_date || row.date_start_work_order);
                return !isNaN(dt.getTime()) && getWeekNumber(dt) === Number(week);
            });
        }

        if (area !== 'all') {
            filteredData = filteredData.filter(row => row.area === area);
        }

        table.clear().rows.add(filteredData).draw();
    }

    // Обновляем недели при выборе месяца/года
    function refreshWeeks() {
        const year = $('#yearSelect').val();
        const month = $('#monthSelect').val();
        if (year === 'all' || month === 'all') { $('#weekSelect').html('<option value="all">Все</option>'); return; }
        const weeks = new Set();
        allData.forEach(r => {
            const d = new Date(r.scheduled_date || r.scheduled_proposed_date || r.date_start_work_order);
            if (!isNaN(d.getTime()) && d.getFullYear() === Number(year) && (d.getMonth()+1) === Number(month)) {
                weeks.add(getWeekNumber(d));
            }
        });
        const opts = Array.from(weeks).sort((a,b)=>a-b).map(w=>`<option value="${w}">${w}</option>`).join('');
        $('#weekSelect').html('<option value="all">Все</option>' + opts);
    }

    $('#yearSelect, #monthSelect').on('change', refreshWeeks);
    $('#loadData').on('click', loadFilteredData);

    // Recalculate column widths to ensure horizontal scroll is applied
    table.columns.adjust();
    $(window).on('resize', function(){
        table.columns.adjust();
    });

    $('#pmTable tbody').on('click', 'td.details-control', function () {
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

