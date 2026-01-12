$(document).ready(function () {
   var table = $('#manualsTable').DataTable({
        ajax: {
            url: '/manuals/all',
            dataSrc: ''
        },
        columns: [
            { data: 'id' },
            { data: 'region' },
            { data: 'equipment' },
            { data: 'node' },
            { data: 'deviceType' },
            {
                data: 'fileName',
                render: function (data, type, row) {
                    return `<a href='#' class='download-link' data-id='${row.id}'>${data}</a>`;
                }
            },
            { data: 'content' },
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

    // Обработчик для просмотра файлов
    $(document).on('click', '.download-link', function (e) {
        e.preventDefault();
        const fileId = $(this).data('id');
        const fileName = $(this).text();
        const fileType = getFileType(fileName);
        openFileModal(fileId, fileName, fileType);
    });

    // Modal open and close functionality
    const uploadModal = document.getElementById('uploadManualModal');
    const openUploadModalButton = document.getElementById('openUploadModalButton');
    const closeUploadModalButton = document.getElementById('closeUploadModal');

    openUploadModalButton.addEventListener('click', () => {
        uploadModal.style.display = 'flex';
    });

    closeUploadModalButton.addEventListener('click', () => {
        uploadModal.style.display = 'none';
    });

    // Handle form submission
    document.getElementById('uploadManualForm').addEventListener('submit', function (event) {
        event.preventDefault();

        const region = document.getElementById('region');
        const equipment = document.getElementById('equipment');
        const node = document.getElementById('node');
        const deviceType = document.querySelector('input[name="deviceType"]');
        const content = document.querySelector('input[name="content"]');
        const file = document.querySelector('input[name="file"]');

        if (!region.value.trim()) {
            alert('Поле "Регион" обязательно для заполнения.');
            region.focus();
            return;
        }

        if (!equipment.value.trim()) {
            alert('Поле "Оборудование" обязательно для заполнения.');
            equipment.focus();
            return;
        }

        if (!node.value.trim()) {
            alert('Поле "Узел" обязательно для заполнения.');
            node.focus();
            return;
        }

        if (!deviceType.value.trim()) {
            alert('Поле "Тип устройства" обязательно для заполнения.');
            deviceType.focus();
            return;
        }

        if (!content.value.trim()) {
            alert('Поле "Содержание" обязательно для заполнения.');
            content.focus();
            return;
        }

        if (!file.files.length) {
            alert('Необходимо выбрать файл для загрузки.');
            file.focus();
            return;
        }

        // Получаем текстовые названия вместо ID
        const regionText = $('#region option:selected').text();
        const equipmentText = $('#equipment option:selected').text();
        const nodeText = $('#node option:selected').text();

        // Создаем FormData и добавляем параметры по отдельности
        const formData = new FormData();
        formData.append('file', file.files[0]);
        formData.append('region', regionText); // Отправляем текст, а не значение
        formData.append('equipment', equipmentText); // Отправляем текст, а не значение
        formData.append('node', nodeText); // Отправляем текст, а не значение
        formData.append('deviceType', deviceType.value);
        formData.append('content', content.value);


        fetch('/manuals/upload', {
            method: 'POST',
            body: formData
        })
            .then(response => response.text())
            .then(data => {
                alert(data);
                document.getElementById('uploadManualModal').style.display = 'none';
                // Reload the table to show new data
                $('#manualsTable').DataTable().ajax.reload();
            })
            .catch(error => {
                alert('Ошибка при загрузке: ' + error.message);
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

    // Добавлен обработчик для кнопок удаления и редактирования
    $(document).on('click', '.delete-button', function () {
        const id = $(this).data('id');
        if (confirm('Вы уверены, что хотите удалить эту запись?')) {
            // Добавлены заголовки в запрос DELETE для manuals
            fetch(`/manuals/delete/${id}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }
            })
                .then(() => $('#manualsTable').DataTable().ajax.reload())
                .catch(error => alert('Ошибка при удалении: ' + error.message));
        }
    });

    $(document).on('click', '.edit-button', function () {
        const id = $(this).data('id');
        const table = $('#manualsTable').DataTable();
        const rowData = table.row($(this).parents('tr')).data();
        // Заполнить скрытое поле id
        $('#editManualId').val(rowData.id);

        // 1. Загрузить регионы
    fetch('/api/regions')
            .then(response => response.json())
            .then(regions => {
                const regionSelect = $('#editRegion');
                regionSelect.empty().append(new Option('Выберите участок', ''));
                regions.forEach(region => {
                    regionSelect.append(new Option(region.name_region, region.id));
                });
                // Установить выбранный регион
                regionSelect.val(rowData.region);

                // 2. Загрузить оборудование для выбранного региона
                if (rowData.region) {
                    fetch(`/api/equipment?regionId=${rowData.region}`)
                        .then(response => response.json())
                        .then(equipments => {
                            const equipmentSelect = $('#editEquipment');
                            equipmentSelect.empty().append(new Option('Выберите оборудование', ''));
                            equipments.forEach(equipment => {
                                equipmentSelect.append(new Option(equipment.name_equipment, equipment.id));
                            });
                            equipmentSelect.val(rowData.equipment);

                            // 3. Загрузить узлы для выбранного оборудования
                            if (rowData.equipment) {
                                fetch(`/api/nodes?equipmentId=${rowData.equipment}`)
                                    .then(response => response.json())
                                    .then(nodes => {
                                        const nodeSelect = $('#editNode');
                                        nodeSelect.empty().append(new Option('Выберите узел', ''));
                                        nodes.forEach(node => {
                                            nodeSelect.append(new Option(node.name_node, node.id));
                                        });
                                        nodeSelect.val(rowData.node);
                                    });
                            } else {
                                $('#editNode').empty().append(new Option('Выберите узел', ''));
                            }
                        });
                } else {
                    $('#editEquipment').empty().append(new Option('Выберите оборудование', ''));
                    $('#editNode').empty().append(new Option('Выберите узел', ''));
                }
            });

        // Остальные поля
        $('#editDeviceType').val(rowData.deviceType);
        $('#editContent').val(rowData.content);
        // Показать модальное окно
        $('#editManualModal').css('display', 'flex');
    });

    // Каскадное обновление списков в модальном окне редактирования
    $('#editRegion').on('change', function () {
        const regionId = $(this).val();
        const equipmentSelect = $('#editEquipment');
        equipmentSelect.empty().append(new Option('Выберите оборудование', ''));
        $('#editNode').empty().append(new Option('Выберите узел', ''));
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
    $('#editEquipment').on('change', function () {
        const equipmentId = $(this).val();
        const nodeSelect = $('#editNode');
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

    // Закрыть модальное окно редактирования
    $('#closeEditModal').on('click', function () {
        $('#editManualModal').css('display', 'none');
    });

    // Обработка отправки формы редактирования
    $('#editManualForm').on('submit', function (event) {
        event.preventDefault();
        const id = $('#editManualId').val();
        const manual = {
            region: $('#editRegion option:selected').text(),
            equipment: $('#editEquipment option:selected').text(),
            node: $('#editNode option:selected').text(),
            deviceType: $('#editDeviceType').val(),
            content: $('#editContent').val()
        };
        fetch(`/manuals/manuals/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(manual)
        })
            .then(response => {
                if (!response.ok) throw new Error('Ошибка при обновлении');
                $('#editManualModal').css('display', 'none');
                $('#manualsTable').DataTable().ajax.reload();
            })
            .catch(error => alert(error.message));
    });
});

// Функция для скачивания файла
function downloadFile(fileId, fileName) {
    fetch(`/manuals/download/${fileId}`)
        .then(response => {
            if (!response.ok) {
                return response.text().then(errorText => {
                    try {
                        const errorData = JSON.parse(errorText);
                        throw new Error(errorData.error || 'Файл не найден');
                    } catch {
                        throw new Error(errorText || 'Файл не найден');
                    }
                });
            }
            return response.blob();
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = fileName || 'file';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
        })
        .catch(error => {
            showErrorModal(error.message);
        });
}

// Функция для отображения модального окна с ошибкой
function showErrorModal(message) {
    const errorModal = document.getElementById('errorModal');
    const errorMessage = document.getElementById('errorMessage');
    errorMessage.textContent = message;
    errorModal.style.display = 'flex';
}

// Функция для экранирования HTML
function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

// Функция для определения типа файла
function getFileType(fileName) {
    if (!fileName) return 'document';
    const lowerFileName = fileName.toLowerCase();
    if (/\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(lowerFileName)) {
        return 'image';
    }
    return 'document';
}

// Функция для открытия модального окна просмотра файла
function openFileModal(fileId, fileName, fileType) {
    const modal = document.getElementById('filePreviewModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalBody = document.getElementById('modalBody');
    const downloadLink = document.getElementById('downloadLink');
    
    const fileUrl = `/manuals/view/${fileId}`;
    
    modalTitle.textContent = fileName;
    downloadLink.href = `/manuals/download/${fileId}`;
    downloadLink.download = fileName;
    
    const escapedFileName = escapeHtml(fileName);
    const escapedFileUrl = escapeHtml(fileUrl);
    
    if (fileType === 'image') {
        modalBody.innerHTML = '<img src="' + escapedFileUrl + '" alt="' + escapedFileName + '" style="max-width: 100%; max-height: 70vh;">';
    } else {
        // Для документов используем iframe или прямую ссылку
        if (/\.pdf$/i.test(fileName)) {
            modalBody.innerHTML = '<iframe src="' + escapedFileUrl + '" style="width: 100%; height: 70vh; border: none;"></iframe>';
        } else {
            modalBody.innerHTML = '<div style="text-align: center; padding: 40px;">' +
                '<p>Просмотр документа недоступен. Используйте кнопку "Скачать" для загрузки файла.</p>' +
                '<p style="font-size: 48px; margin: 20px 0;">📄</p>' +
                '<p><strong>' + escapedFileName + '</strong></p>' +
                '</div>';
        }
    }
    
    modal.style.display = 'block';
}

// Функция для закрытия модального окна просмотра файла
function closeFileModal() {
    document.getElementById('filePreviewModal').style.display = 'none';
}

// Закрытие модального окна при клике вне его
window.onclick = function(event) {
    const fileModal = document.getElementById('filePreviewModal');
    if (event.target === fileModal) {
        closeFileModal();
    }
    const errorModal = document.getElementById('errorModal');
    if (event.target === errorModal) {
        errorModal.style.display = 'none';
    }
}