// Ensure modal open and close functionality works
document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('addSolutionModal');
    const openModalButton = document.getElementById('openModalButton');
    const closeModalButton = document.getElementById('closeModalButton');

    openModalButton.addEventListener('click', () => {
        modal.style.display = 'flex';
    });

    closeModalButton.addEventListener('click', () => {
        modal.style.display = 'none';
    });

    // Close modal when clicking outside the content
    window.addEventListener('click', (event) => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
});

$(document).ready(function () {
    // Инициализация таблицы
    var table = $('#solutionsTable').DataTable({
        ajax: {
            url: '/api/summary-of-solutions',
            dataSrc: ''
        },
        columns: [
            { data: 'id' },
            { data: 'date' },
            { data: 'executor' },
            { data: 'region' },
            { data: 'equipment' },
            { data: 'node' },
            { data: 'notes_on_the_operation_of_the_equipment' },
            { data: 'measures_taken' },
            { data: 'comments' },
            {
                data: null,
                render: function (data, type, row) {
                    return `
                        <button class='edit-button' data-id='${row.id}'>Редактировать</button>
                        <button class='delete-button' data-id='${row.id}'>Удалить</button>
                    `;
                }
            }
        ],
        language: {
            search: "Поиск:",
            lengthMenu: "Показать _MENU_ записей",
            info: "Показано с _START_ по _END_ из _TOTAL_ записей",
            paginate: { first: "Первая", last: "Последняя", next: "Следующая", previous: "Предыдущая" }
        }
    });

    table.order([0, 'desc']).draw();

    // Добавление записи
    $('#addSolutionForm').on('submit', function (e) {
        e.preventDefault();

        const date = $('#date');
        const executor = $('#executor');
        const region = $('#region');
        const equipment = $('#equipment');
        const node = $('#node');
        const notes = $('#notes_on_the_operation_of_the_equipment');
        const measures = $('#measures_taken');
        const comments = $('#comments');

        if (!date.val().trim()) {
            alert('Поле "Дата" обязательно для заполнения.');
            date.focus();
            return;
        }

        if (!executor.val().trim()) {
            alert('Поле "Исполнитель" обязательно для заполнения.');
            executor.focus();
            return;
        }

        if (!region.val().trim()) {
            alert('Поле "Участок" обязательно для заполнения.');
            region.focus();
            return;
        }

        if (!equipment.val().trim()) {
            alert('Поле "Оборудование" обязательно для заполнения.');
            equipment.focus();
            return;
        }

        if (!node.val().trim()) {
            alert('Поле "Узел" обязательно для заполнения.');
            node.focus();
            return;
        }

        if (!notes.val().trim()) {
            alert('Поле "Примечания по работе оборудования" обязательно для заполнения.');
            notes.focus();
            return;
        }

        if (!measures.val().trim()) {
            alert('Поле "Принятые меры" обязательно для заполнения.');
            measures.focus();
            return;
        }

        if (!comments.val().trim()) {
            alert('Поле "Комментарии" обязательно для заполнения.');
            comments.focus();
            return;
        }

        // Если все проверки пройдены, отправляем данные
        const data = {
            date: date.val(),
            executor: executor.val(),
            region: $('#region option:selected').text(), // Получаем текст выбранного option
            equipment: $('#equipment option:selected').text(), // Получаем текст выбранного option
            node: $('#node option:selected').text(), // Получаем текст выбранного option
            notes_on_the_operation_of_the_equipment: notes.val(),
            measures_taken: measures.val(),
            comments: comments.val()
        };

        $.ajax({
            url: '/api/summary-of-solutions',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function () {
                $('#solutionsTable').DataTable().ajax.reload();
                $('#addSolutionForm')[0].reset();
                $('#addSolutionModal').hide(); // Закрытие модального окна
            },
            error: function () {
                alert('Ошибка при добавлении записи');
            }
        });
    });

    // Загрузка данных для списка регионов
    fetch('/api/regions')
        .then(response => response.json())
        .then(data => {
            const regionSelect = $('#region');
            data.forEach(region => {
                regionSelect.append(new Option(region.name_region, region.id));
            });
        });

    // Обновление списка оборудования при выборе региона
    $('#region').on('change', function () {
        const regionId = $(this).val();
        const equipmentSelect = $('#equipment');
        equipmentSelect.empty().append(new Option('Выберите оборудование', ''));

        if (regionId) {
            fetch(`/api/equipment?regionId=${regionId}`)
                .then(response => response.json())
                .then(data => {
                    data.forEach(equipment => {
                        equipmentSelect.append(new Option(equipment.name_equipment, equipment.id));
                    });
                });
        }
    });

    // Обновление списка узлов при выборе оборудования
    $('#equipment').on('change', function () {
        const equipmentId = $(this).val();
        const nodeSelect = $('#node');
        nodeSelect.empty().append(new Option('Выберите узел', ''));

        if (equipmentId) {
            fetch(`/api/nodes?equipmentId=${equipmentId}`)
                .then(response => response.json())
                .then(data => {
                    data.forEach(node => {
                        nodeSelect.append(new Option(node.name_node, node.id));
                    });
                });
        }
    });

    // Редактирование и удаление записей
    $(document).on('click', '.delete-button', function () {
        const id = $(this).data('id');
        if (confirm('Вы уверены, что хотите удалить эту запись?')) {
            // Добавлены заголовки в запрос DELETE для summary-of-solutions
            fetch(`/api/summary-of-solutions/${id}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }
            })
                .then(() => $('#solutionsTable').DataTable().ajax.reload())
                .catch(error => alert('Ошибка при удалении: ' + error.message));
        }
    });

    $(document).on('click', '.edit-button', function () {
        const id = $(this).data('id');

        // Получение текущих данных записи с сервера
        fetch(`/api/summary-of-solutions/${id}`)
            .then(response => response.json())
            .then(data => {
                // Заполнение модального окна текущими данными
                $('#date').val(data.date);
                $('#executor').val(data.executor);
                $('#region').val(data.region).trigger('change');
                $('#equipment').val(data.equipment).trigger('change');
                $('#node').val(data.node);
                $('#notes_on_the_operation_of_the_equipment').val(data.notes_on_the_operation_of_the_equipment);
                $('#measures_taken').val(data.measures_taken);
                $('#comments').val(data.comments);

                // Показ модального окна
                $('#addSolutionModal').css('display', 'flex');

                // Изменение логики кнопки "Добавить" на "Сохранить"
                const submitButton = $('#addSolutionForm button[type="submit"]');
                submitButton.text('Сохранить');

                // Удаление предыдущего обработчика и добавление нового для обновления записи
                $('#addSolutionForm').off('submit').on('submit', function (e) {
                    e.preventDefault();

                    const updatedData = {
                        date: $('#date').val(),
                        executor: $('#executor').val(),
                        region: $('#region option:selected').text(), // Получаем текст выбранного option
                        equipment: $('#equipment option:selected').text(), // Получаем текст выбранного option
                        node: $('#node option:selected').text(), // Получаем текст выбранного option
                        notes_on_the_operation_of_the_equipment: $('#notes_on_the_operation_of_the_equipment').val(),
                        measures_taken: $('#measures_taken').val(),
                        comments: $('#comments').val()
                    };

                    // Отправка PUT-запроса на сервер
                    fetch(`/api/summary-of-solutions/${id}`, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(updatedData)
                    })
                        .then(() => {
                            $('#solutionsTable').DataTable().ajax.reload(); // Обновление таблицы
                            $('#addSolutionModal').hide(); // Закрытие модального окна
                            $('#addSolutionForm')[0].reset(); // Очистка формы
                            submitButton.text('Добавить'); // Возврат текста кнопки
                        })
                        .catch(error => alert('Ошибка при обновлении записи: ' + error.message));
                });
            })
            .catch(error => alert('Ошибка при загрузке данных для редактирования: ' + error.message));
    });
});