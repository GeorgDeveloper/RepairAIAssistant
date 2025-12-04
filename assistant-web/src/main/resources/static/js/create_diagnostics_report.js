$(document).ready(function() {
    // Загрузка участков
    $.ajax({
        url: '/dashboard/diagnostics/areas',
        method: 'GET',
        success: function(areas) {
            const areaSelect = $('#area');
            areas.forEach(function(area) {
                const areaName = area.area || area.name;
                if (areaName) {
                    areaSelect.append($('<option></option>').val(areaName).text(areaName));
                }
            });
        },
        error: function() {
            console.error('Ошибка при загрузке участков');
        }
    });

    // Загрузка оборудования
    function loadEquipment(area) {
        const equipmentSelect = $('#equipment');
        equipmentSelect.html('<option value="">Выберите оборудование</option>');
        
        let url = '/dashboard/diagnostics/equipment';
        if (area) {
            url += '?area=' + encodeURIComponent(area);
        }
        
        $.ajax({
            url: url,
            method: 'GET',
            success: function(equipment) {
                equipment.forEach(function(item) {
                    const machineName = item.machine_name || item.equipment;
                    if (machineName) {
                        equipmentSelect.append($('<option></option>').val(machineName).text(machineName));
                    }
                });
            },
            error: function() {
                console.error('Ошибка при загрузке оборудования');
            }
        });
    }

    // Загрузка оборудования при выборе участка
    $('#area').on('change', function() {
        const selectedArea = $(this).val();
        if (selectedArea) {
            loadEquipment(selectedArea);
        } else {
            loadEquipment(null);
        }
    });

    // Изначальная загрузка всего оборудования
    loadEquipment(null);

    // Обработка отправки формы
    $('#diagnosticsReportForm').on('submit', function(e) {
        e.preventDefault();
        
        const formData = new FormData(this);
        
        // Показываем индикатор загрузки
        const submitButton = $(this).find('button[type="submit"]');
        const originalText = submitButton.text();
        submitButton.prop('disabled', true).text('Создание...');
        
        $.ajax({
            url: '/api/diagnostics/create',
            method: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function(response) {
                const messageDiv = $('#message');
                if (response.success) {
                    messageDiv.removeClass('error').addClass('success')
                        .css({
                            'background-color': '#d4edda',
                            'color': '#155724',
                            'border': '1px solid #c3e6cb'
                        })
                        .text('Отчет успешно создан! ID: ' + response.id)
                        .show();
                    
                    // Перенаправление через 2 секунды
                    setTimeout(function() {
                        window.location.href = '/diagnostics-report';
                    }, 2000);
                } else {
                    messageDiv.removeClass('success').addClass('error')
                        .css({
                            'background-color': '#f8d7da',
                            'color': '#721c24',
                            'border': '1px solid #f5c6cb'
                        })
                        .text('Ошибка: ' + (response.message || 'Неизвестная ошибка'))
                        .show();
                    submitButton.prop('disabled', false).text(originalText);
                }
            },
            error: function(xhr) {
                const messageDiv = $('#message');
                let errorMessage = 'Ошибка при создании отчета';
                
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                } else if (xhr.responseText) {
                    try {
                        const response = JSON.parse(xhr.responseText);
                        errorMessage = response.message || errorMessage;
                    } catch (e) {
                        errorMessage = xhr.responseText;
                    }
                }
                
                messageDiv.removeClass('success').addClass('error')
                    .css({
                        'background-color': '#f8d7da',
                        'color': '#721c24',
                        'border': '1px solid #f5c6cb'
                    })
                    .text('Ошибка: ' + errorMessage)
                    .show();
                submitButton.prop('disabled', false).text(originalText);
            }
        });
    });
});

