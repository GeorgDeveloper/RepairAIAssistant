// Немедленное логирование при загрузке скрипта
try {
    console.log('=== СКРИПТ create_diagnostics_report.js ЗАГРУЖЕН ===');
    console.log('Текущий URL:', window.location.href);
    console.log('Search:', window.location.search);
    
    // Проверяем, что jQuery загружен
    if (typeof jQuery === 'undefined') {
        console.error('jQuery не загружен!');
    } else {
        console.log('jQuery загружен, версия:', jQuery.fn.jquery);
    }
} catch (e) {
    console.error('Ошибка в начале скрипта:', e);
}

$(document).ready(function() {
    console.log('=== СКРИПТ ЗАГРУЖЕН ===');
    console.log('Текущий URL:', window.location.href);
    console.log('Search:', window.location.search);
    
    try {
        // Получаем параметры из URL и сохраняем их в глобальные переменные
        const urlParams = new URLSearchParams(window.location.search);
        
        // Сохраняем все параметры для отладки (декодируем значения)
        const allParams = {};
        urlParams.forEach(function(value, key) {
            try {
                allParams[key] = decodeURIComponent(value);
            } catch (e) {
                allParams[key] = value; // Если не удалось декодировать, используем как есть
            }
        });
        console.log('Все параметры из URL (декодированные):', allParams);
        
        // Функция для безопасного декодирования
        function safeDecode(value) {
            if (!value) return null;
            try {
                return decodeURIComponent(value);
            } catch (e) {
                return value;
            }
        }
        
        // Декодируем параметры при сохранении
        window.scheduleReturnParams = {
            returnTo: safeDecode(urlParams.get('return_to')),
            entryId: safeDecode(urlParams.get('entry_id')),
            equipment: safeDecode(urlParams.get('equipment')),
            area: safeDecode(urlParams.get('area')),
            diagnosticsType: safeDecode(urlParams.get('diagnostics_type')),
            detectionDate: safeDecode(urlParams.get('detection_date'))
        };
        
        console.log('Сохраненные параметры возврата:', window.scheduleReturnParams);
    } catch (e) {
        console.error('Ошибка при обработке параметров URL:', e);
        window.scheduleReturnParams = {};
    }
    
    // Функция для безопасного декодирования (глобальная)
    function safeDecode(value) {
        if (!value) return null;
        try {
            return decodeURIComponent(value);
        } catch (e) {
            return value;
        }
    }
    
    // Загрузка оборудования
    function loadEquipment(area, callback) {
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
                
                // Вызываем callback после загрузки оборудования
                if (callback) {
                    callback();
                }
            },
            error: function() {
                console.error('Ошибка при загрузке оборудования');
                if (callback) {
                    callback();
                }
            }
        });
    }

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
            
            // Предзаполняем форму, если есть параметры из графика
            // Используем сохраненные параметры из window.scheduleReturnParams или читаем из URL
            let urlArea, urlEquipment, urlDiagnosticsType, urlDetectionDate;
            
            if (window.scheduleReturnParams) {
                urlArea = window.scheduleReturnParams.area;
                urlEquipment = window.scheduleReturnParams.equipment;
                urlDiagnosticsType = window.scheduleReturnParams.diagnosticsType;
                urlDetectionDate = window.scheduleReturnParams.detectionDate;
            } else {
                // Если параметры не сохранены, читаем из URL
                try {
                    const currentUrlParams = new URLSearchParams(window.location.search);
                    urlArea = safeDecode(currentUrlParams.get('area'));
                    urlEquipment = safeDecode(currentUrlParams.get('equipment'));
                    urlDiagnosticsType = safeDecode(currentUrlParams.get('diagnostics_type'));
                    urlDetectionDate = safeDecode(currentUrlParams.get('detection_date'));
                } catch (e) {
                    console.error('Ошибка при чтении параметров из URL:', e);
                }
            }
            
            console.log('Параметры для заполнения формы:', {
                area: urlArea,
                equipment: urlEquipment,
                diagnosticsType: urlDiagnosticsType,
                detectionDate: urlDetectionDate
            });
            
            if (urlArea) {
                console.log('Устанавливаем участок:', urlArea);
                // Устанавливаем участок
                areaSelect.val(urlArea);
                
                // Загружаем оборудование для этого участка
                loadEquipment(urlArea, function() {
                    // После загрузки оборудования устанавливаем его значение
                    if (urlEquipment) {
                        console.log('Устанавливаем оборудование:', urlEquipment);
                        setTimeout(function() {
                            const equipmentSelect = $('#equipment');
                            equipmentSelect.val(urlEquipment);
                            console.log('Оборудование установлено, текущее значение:', equipmentSelect.val());
                        }, 200);
                    }
                });
            } else {
                console.log('Участок не указан, загружаем все оборудование');
                // Если участок не указан, загружаем все оборудование
                loadEquipment(null, function() {
                    if (urlEquipment) {
                        console.log('Устанавливаем оборудование (без участка):', urlEquipment);
                        setTimeout(function() {
                            const equipmentSelect = $('#equipment');
                            equipmentSelect.val(urlEquipment);
                            console.log('Оборудование установлено, текущее значение:', equipmentSelect.val());
                        }, 200);
                    }
                });
            }
            
            // Заполняем тип диагностики
            if (urlDiagnosticsType) {
                // Маппинг названий типов диагностики из графика в форму
                const diagnosticsTypeMapping = {
                    'Вибродиагностика': 'Вибродиагностика',
                    'Диагностика конденсатоотводчиков': 'Конденсатоотводчики',
                    'Конденсатоотводчики': 'Конденсатоотводчики',
                    'Диагностики утечек воздуха': 'Акустическая', // Возможно, это акустическая диагностика
                    'Тепловизионная диагностика': 'Термодиагностика',
                    'Термодиагностика': 'Термодиагностика'
                };
                
                // Пытаемся найти соответствие
                let mappedType = diagnosticsTypeMapping[urlDiagnosticsType];
                if (!mappedType) {
                    // Если точного соответствия нет, пытаемся найти частичное
                    if (urlDiagnosticsType.includes('Вибро') || urlDiagnosticsType.includes('вибро')) {
                        mappedType = 'Вибродиагностика';
                    } else if (urlDiagnosticsType.includes('Конденсат') || urlDiagnosticsType.includes('конденсат')) {
                        mappedType = 'Конденсатоотводчики';
                    } else if (urlDiagnosticsType.includes('Тепло') || urlDiagnosticsType.includes('термо') || urlDiagnosticsType.includes('Термо')) {
                        mappedType = 'Термодиагностика';
                    } else if (urlDiagnosticsType.includes('Утеч') || urlDiagnosticsType.includes('утеч') || urlDiagnosticsType.includes('Акуст') || urlDiagnosticsType.includes('акуст')) {
                        mappedType = 'Акустическая';
                    } else {
                        // Если не нашли соответствие, используем исходное значение
                        mappedType = urlDiagnosticsType;
                    }
                }
                
                console.log('Маппинг типа диагностики:', urlDiagnosticsType, '->', mappedType);
                const diagnosticsTypeSelect = $('#diagnostics_type');
                diagnosticsTypeSelect.val(mappedType);
                console.log('Тип диагностики установлен, текущее значение:', diagnosticsTypeSelect.val());
                console.log('Доступные опции:', diagnosticsTypeSelect.find('option').map(function() { return $(this).val(); }).get());
            } else {
                console.log('Тип диагностики не указан в параметрах URL');
            }
            
            // Заполняем дату обнаружения
            if (urlDetectionDate) {
                console.log('Устанавливаем дату обнаружения:', urlDetectionDate);
                // Убеждаемся, что дата в формате YYYY-MM-DD
                let dateValue = urlDetectionDate;
                // Если дата в формате YYYY-MM-DD, используем как есть
                // Если в другом формате, пытаемся преобразовать
                if (!/^\d{4}-\d{2}-\d{2}$/.test(dateValue)) {
                    // Пытаемся преобразовать из других форматов
                    const date = new Date(dateValue);
                    if (!isNaN(date.getTime())) {
                        const year = date.getFullYear();
                        const month = String(date.getMonth() + 1).padStart(2, '0');
                        const day = String(date.getDate()).padStart(2, '0');
                        dateValue = year + '-' + month + '-' + day;
                    }
                }
                $('#detection_date').val(dateValue);
                console.log('Дата установлена, текущее значение:', $('#detection_date').val());
            }
        },
        error: function() {
            console.error('Ошибка при загрузке участков');
            // В случае ошибки все равно пытаемся загрузить оборудование
            loadEquipment(null);
        }
    });

    // Загрузка оборудования при выборе участка
    $('#area').on('change', function() {
        const selectedArea = $(this).val();
        if (selectedArea) {
            loadEquipment(selectedArea);
        } else {
            loadEquipment(null);
        }
    });

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
                        // Используем сохраненные параметры
                        let returnTo = null;
                        let entryId = null;
                        
                        if (window.scheduleReturnParams) {
                            returnTo = window.scheduleReturnParams.returnTo;
                            entryId = window.scheduleReturnParams.entryId;
                        }
                        
                        // Если параметры не найдены, пытаемся прочитать из URL
                        if (!returnTo || !entryId) {
                            const urlParams = new URLSearchParams(window.location.search);
                            returnTo = returnTo || urlParams.get('return_to');
                            entryId = entryId || urlParams.get('entry_id');
                        }
                        
                        console.log('=== ПАРАМЕТРЫ ПЕРЕНАПРАВЛЕНИЯ ===');
                        console.log('returnTo:', returnTo);
                        console.log('entryId:', entryId);
                        console.log('Сохраненные параметры:', window.scheduleReturnParams);
                        console.log('Текущий URL:', window.location.href);
                        
                        if (returnTo === 'schedule' && entryId) {
                            console.log('Обновление статуса наряда:', entryId);
                            // Обновляем статус наряда на "выполнено с дефектом"
                            $.ajax({
                                url: '/api/diagnostics-schedule/entry/' + entryId + '/status',
                                method: 'PUT',
                                contentType: 'application/json',
                                data: JSON.stringify({
                                    isCompleted: true,
                                    hasDefect: true
                                }),
                                success: function(statusResponse) {
                                    console.log('=== ОТВЕТ ОБНОВЛЕНИЯ СТАТУСА ===');
                                    console.log('statusResponse:', statusResponse);
                                    // Всегда возвращаемся в график после обновления статуса
                                    console.log('Перенаправление на /diagnostics-schedule');
                                    window.location.href = '/diagnostics-schedule';
                                },
                                error: function(xhr, status, error) {
                                    console.error('=== ОШИБКА ПРИ ОБНОВЛЕНИИ СТАТУСА ===');
                                    console.error('error:', error);
                                    console.error('status:', status);
                                    console.error('xhr:', xhr);
                                    console.error('xhr.responseJSON:', xhr.responseJSON);
                                    console.error('xhr.status:', xhr.status);
                                    // В любом случае возвращаемся в график
                                    console.log('Перенаправление на /diagnostics-schedule (после ошибки)');
                                    window.location.href = '/diagnostics-schedule';
                                }
                            });
                        } else {
                            console.log('=== ПЕРЕНАПРАВЛЕНИЕ НА /diagnostics-report ===');
                            console.log('Причина: returnTo !== "schedule" или entryId отсутствует');
                            console.log('returnTo:', returnTo, '(ожидается: "schedule")');
                            console.log('entryId:', entryId, '(ожидается: число)');
                            window.location.href = '/diagnostics-report';
                        }
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

