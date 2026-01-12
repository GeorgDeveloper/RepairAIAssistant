// JavaScript for diagnostics schedule page with checkbox selection

let currentScheduleId = null;
let currentMonth = new Date().getMonth() + 1;
let currentYear = new Date().getFullYear();
let diagnosticsTypes = [];
let scheduleData = null;
let areasList = [];
let equipmentList = [];
let selectedEquipment = new Set();
let enabledDiagnosticsTypes = new Set(['B', 'K', 'y', 'T']); // По умолчанию все включены

$(document).ready(function() {
    loadDiagnosticsTypes();
    loadExistingSchedules();
    loadAreas();
    
    $('#createScheduleBtn').click(function() {
        $('#createScheduleForm').toggle();
        if ($('#createScheduleForm').is(':visible')) {
            loadAreas();
            loadEquipmentForCreate();
        }
    });
    
    $('#cancelCreateBtn').click(function() {
        $('#createScheduleForm').hide();
    });
    
    $('#scheduleForm').submit(function(e) {
        e.preventDefault();
        createSchedule();
    });
    
    $('#loadMonthBtn').click(function() {
        const scheduleId = $('#scheduleYearSelect').val();
        const month = $('#monthSelect').val();
        if (scheduleId) {
            currentScheduleId = scheduleId;
            $('#deleteScheduleBtn').show();
            $('#addEquipmentToScheduleBtn').show();
            $('#prevMonthBtn').show();
            $('#nextMonthBtn').show();
            loadMonthSchedule(scheduleId, month);
        }
    });
    
    $('#prevMonthBtn').click(function() {
        if (currentScheduleId && currentMonth > 1) {
            currentMonth--;
            $('#monthSelect').val(currentMonth);
            loadMonthSchedule(currentScheduleId, currentMonth);
        }
    });
    
    $('#nextMonthBtn').click(function() {
        if (currentScheduleId && currentMonth < 12) {
            currentMonth++;
            $('#monthSelect').val(currentMonth);
            loadMonthSchedule(currentScheduleId, currentMonth);
        }
    });
    
    // Чекбокс для переключения между месяцем и годом
    $('#showYearStatsCheckbox').on('change', function() {
        if (currentScheduleId) {
            loadScheduleStats(currentScheduleId);
        }
    });
    
    $('#deleteScheduleBtn').click(function() {
        deleteSchedule();
    });
    
    $('#addEquipmentToScheduleBtn').click(function() {
        $('#addEquipmentForm').toggle();
        if ($('#addEquipmentForm').is(':visible')) {
            loadAreasForAdd();
            loadEquipmentForAdd();
            // Сбрасываем форму при открытии
            $('#addEquipmentToScheduleForm')[0].reset();
            $('#addEquipmentCheckboxList .equipment-checkbox').prop('checked', false);
            // Сбрасываем параметры диагностики
            $('#addDiagnosticsParams .add-diag-period').val('');
            $('#addDiagnosticsParams .add-diag-duration').each(function() {
                const typeCode = $(this).data('code');
                const type = diagnosticsTypes.find(t => t.code === typeCode);
                if (type) {
                    $(this).val(type.durationMinutes);
                }
            });
        }
    });
    
    $('#cancelAddEquipmentBtn').click(function() {
        $('#addEquipmentForm').hide();
    });
    
    $('#addEquipmentToScheduleForm').submit(function(e) {
        e.preventDefault();
        addEquipmentToSchedule();
    });
    
    // Фильтрация оборудования по участку при создании
    $('#createAreaSelect').on('change', function() {
        filterEquipmentForCreate($(this).val());
    });
    
    // Фильтрация оборудования по участку при добавлении
    $('#addAreaSelect').on('change', function() {
        filterEquipmentForAdd($(this).val());
    });
    
    // Показываем кнопку удаления при выборе графика
    $('#scheduleYearSelect').on('change', function() {
        const scheduleId = $(this).val();
        if (scheduleId) {
            currentScheduleId = scheduleId;
            $('#deleteScheduleBtn').show();
            $('#addEquipmentToScheduleBtn').show();
        } else {
            $('#deleteScheduleBtn').hide();
            $('#addEquipmentToScheduleBtn').hide();
        }
    });
    
    // Кнопки выбора всего оборудования
    $('#selectAllEquipmentBtn').click(function() {
        $('#equipmentCheckboxList .equipment-checkbox').prop('checked', true).trigger('change');
    });
    
    $('#deselectAllEquipmentBtn').click(function() {
        $('#equipmentCheckboxList .equipment-checkbox').prop('checked', false).trigger('change');
    });
    
    $('#addSelectAllEquipmentBtn').click(function() {
        $('#addEquipmentCheckboxList .equipment-checkbox').prop('checked', true).trigger('change');
    });
    
    $('#addDeselectAllEquipmentBtn').click(function() {
        $('#addEquipmentCheckboxList .equipment-checkbox').prop('checked', false).trigger('change');
    });
    
    // Фильтры для графика
    $('#filterEquipment, #filterArea, #filterDiagnosticsType').on('change', function() {
        if (originalScheduleData) {
            applyFilters();
        }
    });
    
    $('#clearFiltersBtn').click(function() {
        $('#filterEquipment').val('');
        $('#filterArea').val('');
        $('#filterDiagnosticsType').val('');
        if (originalScheduleData) {
            applyFilters();
        }
    });
    
    // Обновляем состояние кнопок навигации при изменении месяца
    $('#monthSelect').on('change', function() {
        if (currentScheduleId) {
            currentMonth = parseInt($(this).val());
            loadMonthSchedule(currentScheduleId, currentMonth);
        }
    });
});

function loadDiagnosticsTypes() {
    $.ajax({
        url: '/api/diagnostics-schedule/types',
        method: 'GET',
        success: function(types) {
            window.diagnosticsTypes = types;
            diagnosticsTypes = types;
            updateLegend(types);
            renderDiagnosticsTypesConfig();
            renderDiagnosticsParams();
        },
        error: function(xhr, status, error) {
            console.error('Ошибка загрузки типов диагностики:', error);
        }
    });
}

function renderDiagnosticsTypesConfig() {
    const container = $('#diagnosticsTypesConfig');
    const addContainer = $('#addDiagnosticsTypesConfig');
    container.empty();
    addContainer.empty();
    
    diagnosticsTypes.forEach(function(type) {
        const checkbox = $('<input>').attr({
            type: 'checkbox',
            class: 'diag-type-checkbox',
            'data-code': type.code,
            id: 'diag-type-' + type.code,
            checked: enabledDiagnosticsTypes.has(type.code)
        });
        
        const label = $('<label>').attr('for', 'diag-type-' + type.code)
            .text(type.code + ' - ' + type.name);
        
        const item = $('<div class="diag-type-checkbox-item"></div>')
            .append(checkbox)
            .append(label);
        
        container.append(item);
        addContainer.append(item.clone());
    });
    
    // Обработчики изменения чекбоксов
    $('.diag-type-checkbox').on('change', function() {
        const code = $(this).data('code');
        if ($(this).is(':checked')) {
            enabledDiagnosticsTypes.add(code);
        } else {
            enabledDiagnosticsTypes.delete(code);
        }
        renderDiagnosticsParams();
        renderAddDiagnosticsParams();
    });
}

function renderDiagnosticsParams() {
    const container = $('#diagnosticsParams');
    container.empty();
    
    diagnosticsTypes.forEach(function(type) {
        if (!enabledDiagnosticsTypes.has(type.code)) {
            return;
        }
        
        const paramItem = $('<div class="diag-param-item"></div>');
        paramItem.append($('<label>').text(type.code + ' (' + type.name + '):'));
        
        const inputs = $('<div class="diag-inputs"></div>');
        inputs.append($('<input>').attr({
            type: 'number',
            min: '0.5',
            max: '12',
            step: '0.5',
            value: '',
            class: 'diag-period',
            'data-code': type.code,
            placeholder: 'Период (мес)',
            title: 'Период диагностики в месяцах (0.5-12). 1 = раз в месяц, 6 = раз в полгода, 12 = раз в год'
        }));
        inputs.append($('<input>').attr({
            type: 'number',
            min: '1',
            class: 'diag-duration',
            'data-code': type.code,
            placeholder: 'Длительность (мин)',
            value: type.durationMinutes,
            title: 'Длительность в минутах'
        }));
        
        paramItem.append(inputs);
        container.append(paramItem);
    });
}

function renderAddDiagnosticsParams() {
    const container = $('#addDiagnosticsParams');
    container.empty();
    
    diagnosticsTypes.forEach(function(type) {
        if (!enabledDiagnosticsTypes.has(type.code)) {
            return;
        }
        
        const paramItem = $('<div class="diag-param-item"></div>');
        paramItem.append($('<label>').text(type.code + ' (' + type.name + '):'));
        
        const inputs = $('<div class="diag-inputs"></div>');
        inputs.append($('<input>').attr({
            type: 'number',
            min: '0.5',
            max: '12',
            step: '0.5',
            value: '',
            class: 'add-diag-period',
            'data-code': type.code,
            placeholder: 'Период (мес)',
            title: 'Период диагностики в месяцах (0.5-12). 1 = раз в месяц, 6 = раз в полгода, 12 = раз в год'
        }));
        inputs.append($('<input>').attr({
            type: 'number',
            min: '1',
            class: 'add-diag-duration',
            'data-code': type.code,
            placeholder: 'Длительность (мин)',
            value: type.durationMinutes,
            title: 'Длительность в минутах'
        }));
        
        paramItem.append(inputs);
        container.append(paramItem);
    });
}

function loadAreas() {
    $.ajax({
        url: '/dashboard/diagnostics/areas',
        method: 'GET',
        success: function(areas) {
            areasList = areas;
            updateAreaSelects();
        },
        error: function() {
            console.error('Ошибка при загрузке участков');
        }
    });
}

function updateAreaSelects() {
    const selects = ['#createAreaSelect', '#addAreaSelect'];
    selects.forEach(function(selector) {
        const select = $(selector);
        const currentValue = select.val();
        select.html('<option value="">Все участки</option>');
        
        areasList.forEach(function(area) {
            const areaName = area.area || area.name;
            if (areaName) {
                const option = $('<option></option>').val(areaName).text(areaName);
                if (areaName === currentValue) {
                    option.prop('selected', true);
                }
                select.append(option);
            }
        });
    });
}

function loadAreasForAdd() {
    if (areasList.length === 0) {
        loadAreas();
    } else {
        updateAreaSelects();
    }
}

function loadEquipmentForCreate() {
    $.ajax({
        url: '/dashboard/diagnostics/equipment',
        method: 'GET',
        success: function(equipment) {
            equipmentList = equipment;
            renderEquipmentCheckboxes('#equipmentCheckboxList', equipment);
        },
        error: function() {
            console.error('Ошибка при загрузке оборудования');
        }
    });
}

function loadEquipmentForAdd() {
    $.ajax({
        url: '/dashboard/diagnostics/equipment',
        method: 'GET',
        success: function(equipment) {
            renderEquipmentCheckboxes('#addEquipmentCheckboxList', equipment);
        },
        error: function() {
            console.error('Ошибка при загрузке оборудования');
        }
    });
}

function renderEquipmentCheckboxes(containerSelector, equipment) {
    const container = $(containerSelector);
    container.empty();
    
    if (containerSelector === '#equipmentCheckboxList') {
        selectedEquipment.clear();
    }
    
    equipment.forEach(function(item) {
        const machineName = item.machine_name || item.equipment;
        const area = item.area || '';
        
        if (machineName) {
            const checkbox = $('<input>').attr({
                type: 'checkbox',
                class: 'equipment-checkbox',
                'data-equipment': machineName,
                'data-area': area,
                id: 'eq-' + containerSelector.replace('#', '') + '-' + machineName.replace(/\s+/g, '-').replace(/[^a-zA-Z0-9-]/g, ''),
                value: machineName
            });
            
            const label = $('<label>').attr('for', checkbox.attr('id'))
                .text(machineName + (area ? ' (' + area + ')' : ''));
            
            const itemDiv = $('<div class="equipment-checkbox-item"></div>')
                .append(checkbox)
                .append(label);
            
            container.append(itemDiv);
        }
    });
    
    // Обработчики выбора оборудования
    $(containerSelector + ' .equipment-checkbox').on('change', function() {
        const equipmentName = $(this).data('equipment');
        if (containerSelector === '#equipmentCheckboxList') {
            if ($(this).is(':checked')) {
                selectedEquipment.add(equipmentName);
            } else {
                selectedEquipment.delete(equipmentName);
            }
        }
    });
}

function filterEquipmentForCreate(area) {
    const filtered = area ? equipmentList.filter(eq => (eq.area || '') === area) : equipmentList;
    renderEquipmentCheckboxes('#equipmentCheckboxList', filtered);
}

function filterEquipmentForAdd(area) {
    $.ajax({
        url: area ? '/dashboard/diagnostics/equipment?area=' + encodeURIComponent(area) : '/dashboard/diagnostics/equipment',
        method: 'GET',
        success: function(equipment) {
            renderEquipmentCheckboxes('#addEquipmentCheckboxList', equipment);
        },
        error: function() {
            console.error('Ошибка при загрузке оборудования');
        }
    });
}

function createSchedule() {
    const year = parseInt($('#year').val());
    const workersCount = parseInt($('#workersCount').val());
    const shiftDurationHours = parseInt($('#shiftDurationHours').val());
    const startDate = $('#startDate').val() || null;
    
    if (selectedEquipment.size === 0) {
        alert('Выберите хотя бы одно оборудование');
        return;
    }
    
    if (enabledDiagnosticsTypes.size === 0) {
        alert('Выберите хотя бы один тип диагностики');
        return;
    }
    
    const equipmentList = [];
    let hasErrors = false;
    
    selectedEquipment.forEach(function(equipmentName) {
        const checkbox = $('#equipmentCheckboxList .equipment-checkbox[data-equipment="' + equipmentName + '"]');
        const area = checkbox.data('area') || null;
        const diagnosticsCounts = {};
        const diagnosticsDurations = {};
        
        enabledDiagnosticsTypes.forEach(function(typeCode) {
            const periodInput = $('#diagnosticsParams .diag-period[data-code="' + typeCode + '"]');
            const durationInput = $('#diagnosticsParams .diag-duration[data-code="' + typeCode + '"]');
            const period = parseFloat(periodInput.val());
            const duration = parseInt(durationInput.val());
            
            if (period && period >= 0.5 && period <= 12) {
                if (!duration || duration <= 0) {
                    alert('Укажите длительность диагностики для типа ' + typeCode + ' (в минутах)');
                    durationInput.focus();
                    hasErrors = true;
                    return false;
                }
                // Период в месяцах сохраняем как есть
                diagnosticsCounts[typeCode] = period;
                diagnosticsDurations[typeCode] = duration;
            } else if (period && (period < 0.5 || period > 12)) {
                alert('Период диагностики должен быть от 0.5 до 12 месяцев для типа ' + typeCode);
                periodInput.focus();
                hasErrors = true;
                return false;
            }
        });
        
        if (hasErrors) {
            return false;
        }
        
        if (Object.keys(diagnosticsCounts).length > 0) {
            equipmentList.push({
                equipment: equipmentName,
                area: area,
                diagnosticsCounts: diagnosticsCounts,
                diagnosticsDurations: diagnosticsDurations
            });
        }
    });
    
    if (hasErrors) {
        return;
    }
    
    if (equipmentList.length === 0) {
        alert('Укажите период диагностики для выбранного оборудования');
        return;
    }
    
    const requestData = {
        year: year,
        workersCount: workersCount,
        shiftDurationHours: shiftDurationHours,
        startDate: startDate,
        equipmentList: equipmentList
    };
    
    $.ajax({
        url: '/api/diagnostics-schedule/create',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        success: function(response) {
            if (response.success) {
                alert('График успешно создан!');
                $('#createScheduleForm').hide();
                $('#scheduleForm')[0].reset();
                selectedEquipment.clear();
                enabledDiagnosticsTypes = new Set(['B', 'K', 'y', 'T']);
                loadExistingSchedules();
            } else {
                alert('Ошибка: ' + response.message);
            }
        },
        error: function(xhr, status, error) {
            const response = xhr.responseJSON;
            alert('Ошибка создания графика: ' + (response ? response.message : error));
        }
    });
}

function addEquipmentToSchedule() {
    if (!currentScheduleId) {
        alert('График не выбран');
        return;
    }
    
    const selected = new Set();
    $('#addEquipmentCheckboxList .equipment-checkbox:checked').each(function() {
        selected.add($(this).data('equipment'));
    });
    
    if (selected.size === 0) {
        alert('Выберите хотя бы одно оборудование');
        return;
    }
    
    const equipmentList = [];
    let hasErrors = false;
    
    selected.forEach(function(equipmentName) {
        const checkbox = $('#addEquipmentCheckboxList .equipment-checkbox[data-equipment="' + equipmentName + '"]');
        const area = checkbox.data('area') || null;
        const diagnosticsCounts = {};
        const diagnosticsDurations = {};
        
        enabledDiagnosticsTypes.forEach(function(typeCode) {
            const periodInput = $('#addDiagnosticsParams .add-diag-period[data-code="' + typeCode + '"]');
            const durationInput = $('#addDiagnosticsParams .add-diag-duration[data-code="' + typeCode + '"]');
            const period = parseFloat(periodInput.val());
            const duration = parseInt(durationInput.val());
            
            if (period && period >= 0.5 && period <= 12) {
                if (!duration || duration <= 0) {
                    alert('Укажите длительность диагностики для типа ' + typeCode + ' (в минутах)');
                    durationInput.focus();
                    hasErrors = true;
                    return false;
                }
                // Период в месяцах сохраняем как есть
                diagnosticsCounts[typeCode] = period;
                diagnosticsDurations[typeCode] = duration;
            } else if (period && (period < 0.5 || period > 12)) {
                alert('Период диагностики должен быть от 0.5 до 12 месяцев для типа ' + typeCode);
                periodInput.focus();
                hasErrors = true;
                return false;
            }
        });
        
        if (hasErrors) {
            return false;
        }
        
        if (Object.keys(diagnosticsCounts).length > 0) {
            equipmentList.push({
                equipment: equipmentName,
                area: area,
                diagnosticsCounts: diagnosticsCounts,
                diagnosticsDurations: diagnosticsDurations
            });
        }
    });
    
    if (hasErrors) {
        return;
    }
    
    if (equipmentList.length === 0) {
        alert('Укажите период диагностики для выбранного оборудования');
        return;
    }
    
    const startDate = $('#addStartDate').val() || null;
    
    const requestData = {
        startDate: startDate,
        equipmentList: equipmentList
    };
    
    $.ajax({
        url: '/api/diagnostics-schedule/' + currentScheduleId + '/add-equipment',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        success: function(response) {
            if (response.success) {
                alert('✅ Оборудование успешно добавлено в график!\n\nНовое оборудование распределено по свободным дням с учетом заданного периода диагностики (±2 недели от идеальной даты).');
                $('#addEquipmentForm').hide();
                $('#addEquipmentToScheduleForm')[0].reset();
                // Сбрасываем чекбоксы
                $('#addEquipmentCheckboxList .equipment-checkbox').prop('checked', false);
                selected.clear();
                // Сбрасываем параметры диагностики
                $('#addDiagnosticsParams .add-diag-period').val('');
                $('#addDiagnosticsParams .add-diag-duration').each(function() {
                    const typeCode = $(this).data('code');
                    const type = diagnosticsTypes.find(t => t.code === typeCode);
                    if (type) {
                        $(this).val(type.durationMinutes);
                    }
                });
                // Перезагружаем текущий месяц и статистику
                if (currentScheduleId) {
                    loadMonthSchedule(currentScheduleId, currentMonth);
                }
            } else {
                alert('Ошибка: ' + response.message);
            }
        },
        error: function(xhr, status, error) {
            const response = xhr.responseJSON;
            alert('Ошибка добавления оборудования: ' + (response ? response.message : error));
        }
    });
}

function loadExistingSchedules() {
    $.ajax({
        url: '/api/diagnostics-schedule',
        method: 'GET',
        success: function(schedules) {
            if (schedules && schedules.length > 0) {
                $('#scheduleSelector').show();
                $('#loadScheduleBtn').show();
                $('#createScheduleBtn').text('Создать новый график');
                
                const select = $('#scheduleYearSelect');
                select.empty();
                schedules.forEach(function(schedule) {
                    select.append($('<option>', {
                        value: schedule.id,
                        text: schedule.year + ' год'
                    }));
                });
                
                // Выбираем текущий год, если есть
                const currentSchedule = schedules.find(s => s.year === currentYear);
                if (currentSchedule) {
                    select.val(currentSchedule.id);
                    currentScheduleId = currentSchedule.id;
                    $('#deleteScheduleBtn').show();
                    $('#addEquipmentToScheduleBtn').show();
                    loadMonthSchedule(currentSchedule.id, currentMonth);
                } else if (schedules.length > 0) {
                    const firstSchedule = schedules[0];
                    select.val(firstSchedule.id);
                    currentScheduleId = firstSchedule.id;
                    $('#deleteScheduleBtn').show();
                    $('#addEquipmentToScheduleBtn').show();
                    // Автоматически загружаем первый месяц для удобства
                    loadMonthSchedule(firstSchedule.id, 1);
                }
            }
        },
        error: function(xhr, status, error) {
            console.error('Ошибка загрузки графиков:', error);
        }
    });
}

function loadMonthSchedule(scheduleId, month) {
    currentScheduleId = scheduleId;
    const monthNum = parseInt(month);
    currentMonth = monthNum;
    
    // Обновляем выбранный месяц в селекте
    $('#monthSelect').val(monthNum);
    
    $.ajax({
        url: `/api/diagnostics-schedule/${scheduleId}/month/${month}`,
        method: 'GET',
        success: function(data) {
            scheduleData = data;
            displaySchedule(data);
            // Загружаем статистику за текущий месяц (чекбокс не отмечен)
            $('#showYearStatsCheckbox').prop('checked', false);
            // Загружаем статистику после небольшой задержки, чтобы currentMonth был установлен
            setTimeout(function() {
                loadScheduleStats(scheduleId);
            }, 50);
            // Показываем кнопки навигации и обновляем их состояние
            $('#prevMonthBtn').show().prop('disabled', currentMonth <= 1);
            $('#nextMonthBtn').show().prop('disabled', currentMonth >= 12);
        },
        error: function(xhr, status, error) {
            console.error('Ошибка загрузки графика:', error);
            const response = xhr.responseJSON;
            alert('Ошибка загрузки графика: ' + (response ? response.message : error));
        }
    });
}

// Сохраняем исходные данные для фильтрации
let originalScheduleData = null;

function displaySchedule(data) {
    originalScheduleData = data;
    applyFilters();
}

function applyFilters() {
    if (!originalScheduleData) {
        return;
    }
    
    const entries = originalScheduleData.entries;
    const startDate = parseDate(originalScheduleData.startDate);
    const endDate = parseDate(originalScheduleData.endDate);
    
    // Получаем значения фильтров
    const filterEquipment = $('#filterEquipment').val();
    const filterArea = $('#filterArea').val();
    const filterDiagnosticsType = $('#filterDiagnosticsType').val();
    
    const dates = [];
    const date = new Date(startDate);
    while (date <= endDate) {
        dates.push(new Date(date));
        date.setDate(date.getDate() + 1);
    }
    
    // Фильтруем оборудование
    let equipmentList = Object.keys(entries).sort();
    
    if (filterEquipment) {
        equipmentList = equipmentList.filter(eq => eq === filterEquipment);
    }
    
    // Собираем уникальные участки и типы диагностики из данных
    const areasSet = new Set();
    const typesSet = new Set();
    
    Object.keys(entries).forEach(function(equipment) {
        Object.keys(entries[equipment]).forEach(function(dateStr) {
            entries[equipment][dateStr].forEach(function(entry) {
                if (entry.area) {
                    areasSet.add(entry.area);
                }
                if (entry.diagnosticsType && entry.diagnosticsType.code) {
                    typesSet.add(entry.diagnosticsType.code);
                }
            });
        });
    });
    
    // Обновляем фильтры участков и типов диагностики
    updateFilterSelects(Array.from(areasSet).sort(), Array.from(typesSet).sort());
    
    // Фильтруем по участку и типу диагностики
    const filteredEntries = {};
    equipmentList.forEach(function(equipment) {
        const equipmentEntries = {};
        Object.keys(entries[equipment]).forEach(function(dateStr) {
            let dayEntries = entries[equipment][dateStr] || [];
            
            // Фильтруем по участку
            if (filterArea) {
                dayEntries = dayEntries.filter(e => e.area === filterArea);
            }
            
            // Фильтруем по типу диагностики
            if (filterDiagnosticsType) {
                dayEntries = dayEntries.filter(e => 
                    e.diagnosticsType && e.diagnosticsType.code === filterDiagnosticsType
                );
            }
            
            // Добавляем день только если есть записи после фильтрации или фильтры не применены
            if (dayEntries.length > 0) {
                equipmentEntries[dateStr] = dayEntries;
            } else if (!filterArea && !filterDiagnosticsType) {
                // Если фильтры не применены, показываем все дни (даже пустые)
                equipmentEntries[dateStr] = [];
            }
        });
        
        // Добавляем оборудование только если есть записи после фильтрации или фильтры не применены
        if (Object.keys(equipmentEntries).length > 0) {
            filteredEntries[equipment] = equipmentEntries;
        }
    });
    
    // Обновляем список оборудования в фильтре (все доступное оборудование)
    const allEquipment = Object.keys(entries).sort();
    updateEquipmentFilter(allEquipment);
    
    // Отображаем отфильтрованную таблицу
    renderScheduleTable(filteredEntries, dates);
    
    $('#scheduleContainer').show();
    $('#scheduleFilters').show();
}

function updateFilterSelects(areas, types) {
    // Обновляем фильтр участков
    const areaSelect = $('#filterArea');
    const currentArea = areaSelect.val();
    areaSelect.html('<option value="">Все участки</option>');
    areas.forEach(function(area) {
        areaSelect.append($('<option>').val(area).text(area));
    });
    if (currentArea && areas.includes(currentArea)) {
        areaSelect.val(currentArea);
    }
    
    // Обновляем фильтр типов диагностики
    const typeSelect = $('#filterDiagnosticsType');
    const currentType = typeSelect.val();
    typeSelect.html('<option value="">Все типы</option>');
    types.forEach(function(typeCode) {
        const type = diagnosticsTypes.find(t => t.code === typeCode);
        const typeName = type ? type.name : typeCode;
        typeSelect.append($('<option>').val(typeCode).text(typeCode + ' - ' + typeName));
    });
    if (currentType && types.includes(currentType)) {
        typeSelect.val(currentType);
    }
}

function updateEquipmentFilter(equipmentList) {
    const equipmentSelect = $('#filterEquipment');
    const currentEquipment = equipmentSelect.val();
    equipmentSelect.html('<option value="">Все оборудование</option>');
    equipmentList.forEach(function(equipment) {
        equipmentSelect.append($('<option>').val(equipment).text(equipment));
    });
    if (currentEquipment && equipmentList.includes(currentEquipment)) {
        equipmentSelect.val(currentEquipment);
    }
}

function renderScheduleTable(entries, dates) {
    const equipmentList = Object.keys(entries).sort();
    
    const table = $('#scheduleTable');
    table.empty();
    
    const thead = $('<thead>').append($('<tr>'));
    thead.find('tr').append($('<th>').text('Оборудование'));
    
    dates.forEach(function(d) {
        const day = d.getDate();
        const dayOfWeek = d.getDay();
        const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
        const th = $('<th>').text(day);
        if (isWeekend) {
            th.addClass('weekend');
        }
        thead.find('tr').append(th);
    });
    
    table.append(thead);
    
    const tbody = $('<tbody>');
    
    equipmentList.forEach(function(equipment) {
        const row = $('<tr>');
        row.append($('<td>').text(equipment));
        
        dates.forEach(function(d) {
            const dateStr = formatDate(d);
            const dayEntries = entries[equipment] && entries[equipment][dateStr] 
                ? entries[equipment][dateStr] 
                : [];
            
            const cell = $('<td>').addClass('schedule-cell');
            
            if (d.getDay() === 0 || d.getDay() === 6) {
                cell.addClass('weekend');
            }
            
            // Добавляем дату ко всем ячейкам для определения целевой позиции
            cell.attr('data-current-date', dateStr);
            
            if (dayEntries.length > 0) {
                cell.addClass('has-diagnostics');
                const codes = dayEntries.map(e => e.diagnosticsType.code).join(', ');
                cell.text(codes);
                
                if (dayEntries.length === 1) {
                    const entry = dayEntries[0];
                    const type = entry.diagnosticsType;
                    
                    // Определяем цвет в зависимости от статуса
                    let backgroundColor = type.colorCode || '#90EE90';
                    let statusText = '';
                    
                    if (entry.isCompleted) {
                        // Проверяем, есть ли дефект (в notes)
                        if (entry.notes && entry.notes.includes('Обнаружен дефект')) {
                            backgroundColor = '#DDA0DD'; // Светло-фиолетовый
                            statusText = ' (дефект)';
                        } else {
                            backgroundColor = '#808080'; // Серый
                            statusText = ' (выполнено)';
                        }
                    } else {
                        // Проверяем, просрочен ли наряд
                        if (dateStr) {
                            const scheduledDate = parseDate(dateStr);
                            if (scheduledDate) {
                                const today = new Date();
                                today.setHours(0, 0, 0, 0);
                                const scheduled = new Date(scheduledDate);
                                scheduled.setHours(0, 0, 0, 0);
                                
                                if (scheduled < today) {
                                    // Просроченный наряд
                                    backgroundColor = '#FF6B6B'; // Красный
                                    statusText = ' (не выполнено)';
                                }
                            }
                        }
                    }
                    
                    cell.css('background-color', backgroundColor);
                    // Добавляем возможность перетаскивания для одной записи
                    cell.attr('draggable', 'true');
                    cell.attr('data-entry-id', entry.id);
                    cell.attr('data-equipment', equipment);
                    cell.attr('data-entry-data', JSON.stringify(entry)); // Сохраняем данные для модального окна
                } else {
                    cell.css('background-color', '#FFD700');
                }
                
                cell.attr('title', dayEntries.map(e => {
                    let tooltip = e.diagnosticsType.name;
                    // Используем продолжительность из наряда (сохраненную при создании)
                    const duration = e.diagnosticsType && e.diagnosticsType.durationMinutes 
                        ? e.diagnosticsType.durationMinutes 
                        : null;
                    if (duration) {
                        tooltip += ' (' + duration + ' мин)';
                    }
                    if (e.isCompleted) {
                        if (e.notes && e.notes.includes('Обнаружен дефект')) {
                            tooltip += ' (дефект)';
                        } else {
                            tooltip += ' (выполнено)';
                        }
                    } else {
                        if (dateStr) {
                            const scheduledDate = parseDate(dateStr);
                            if (scheduledDate) {
                                const today = new Date();
                                today.setHours(0, 0, 0, 0);
                                const scheduled = new Date(scheduledDate);
                                scheduled.setHours(0, 0, 0, 0);
                                if (scheduled < today) {
                                    tooltip += ' (не выполнено)';
                                }
                            }
                        }
                    }
                    return tooltip;
                }).join('\n'));
            }
            
            row.append(cell);
        });
        
        tbody.append(row);
    });
    
    table.append(tbody);
    
    // Добавляем обработчики drag and drop для ячеек с диагностиками
    setupDragAndDrop();
    
    // Добавляем обработчик двойного клика для открытия модального окна
    setupDoubleClickHandler();
}

function setupDragAndDrop() {
    let draggedCell = null;
    let dragStartX = 0;
    let originalDate = null; // Сохраняем исходную дату перед перемещением
    
    // Обработчик начала перетаскивания
    $(document).on('dragstart', '.schedule-cell.has-diagnostics[draggable="true"]', function(e) {
        draggedCell = $(this);
        dragStartX = e.originalEvent.clientX;
        originalDate = $(this).attr('data-current-date'); // Сохраняем исходную дату
        $(this).addClass('dragging');
        e.originalEvent.dataTransfer.effectAllowed = 'move';
        e.originalEvent.dataTransfer.setData('text/html', $(this).html());
    });
    
    // Обработчик окончания перетаскивания
    $(document).on('dragend', '.schedule-cell.has-diagnostics[draggable="true"]', function(e) {
        $(this).removeClass('dragging');
        $('.schedule-cell').removeClass('drag-over-left drag-over-right drag-over-target');
        // Не сбрасываем draggedCell и originalDate здесь, так как они нужны для обработки drop
    });
    
    // Обработчик наведения на ячейку при перетаскивании
    $(document).on('dragover', '.schedule-cell', function(e) {
        e.preventDefault();
        e.originalEvent.dataTransfer.dropEffect = 'move';
        
        if (draggedCell && draggedCell.length > 0) {
            const targetCell = $(this);
            const draggedDateStr = draggedCell.attr('data-current-date');
            const targetDateStr = targetCell.attr('data-current-date');
            
            // Убираем предыдущие классы
            $('.schedule-cell').removeClass('drag-over-left drag-over-right drag-over-target');
            
            // Если есть дата в целевой ячейке, подсвечиваем её
            if (targetDateStr && targetDateStr !== draggedDateStr) {
                const currentDate = parseDate(draggedDateStr);
                const targetDate = parseDate(targetDateStr);
                const daysDiff = Math.round((targetDate - currentDate) / (1000 * 60 * 60 * 24));
                
                // Ограничиваем максимум 7 днями
                if (Math.abs(daysDiff) <= 7) {
                    targetCell.addClass('drag-over-target');
                }
            } else {
                // Если нет даты, используем визуальную обратную связь по направлению
                const dragDeltaX = e.originalEvent.clientX - dragStartX;
                if (Math.abs(dragDeltaX) > 15) {
                    if (dragDeltaX < 0) {
                        draggedCell.addClass('drag-over-left');
                    } else {
                        draggedCell.addClass('drag-over-right');
                    }
                }
            }
        }
    });
    
    // Обработчик отпускания
    $(document).on('drop', '.schedule-cell', function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        if (!draggedCell || draggedCell.length === 0) {
            return;
        }
        
        const entryId = draggedCell.attr('data-entry-id');
        const currentDateStr = draggedCell.attr('data-current-date');
        const equipment = draggedCell.attr('data-equipment');
        
        if (!entryId || !currentDateStr) {
            return;
        }
        
        // Определяем целевую ячейку (куда отпустили)
        const targetCell = $(this);
        const targetDateStr = targetCell.attr('data-current-date');
        
        // Если есть дата в целевой ячейке, используем её
        if (targetDateStr && targetDateStr !== currentDateStr) {
            const currentDate = parseDate(currentDateStr);
            const targetDate = parseDate(targetDateStr);
            
            // Вычисляем разницу в днях
            const daysDiff = Math.round((targetDate - currentDate) / (1000 * 60 * 60 * 24));
            
            // Ограничиваем максимум 7 днями в любую сторону
            const daysOffset = Math.max(-7, Math.min(7, daysDiff));
            
            if (daysOffset !== 0) {
                const newDate = new Date(currentDate);
                newDate.setDate(newDate.getDate() + daysOffset);
                const newDateStr = formatDate(newDate);
                
                // Проверяем, что новая дата в пределах текущего месяца (если он загружен)
                if (currentMonth && newDate.getMonth() + 1 !== currentMonth) {
                    alert('Новая дата должна быть в пределах текущего месяца');
                    return;
                }
                
                // Отправляем запрос на обновление даты (сохраняем исходную дату для возможного отката)
                const originalDate = draggedCell.attr('data-current-date');
                updateEntryDate(entryId, newDateStr, equipment, originalDate);
            }
        } else {
            // Если нет даты в целевой ячейке, используем смещение по горизонтали
            const dragDeltaX = e.originalEvent.clientX - dragStartX;
            
            // Вычисляем количество дней на основе смещения (примерно 30 пикселей = 1 день)
            let daysOffset = Math.round(dragDeltaX / 30);
            
            // Ограничиваем максимум 7 днями в любую сторону
            daysOffset = Math.max(-7, Math.min(7, daysOffset));
            
            if (daysOffset !== 0 && Math.abs(dragDeltaX) > 15) { // Минимальное расстояние для активации
                const currentDate = parseDate(currentDateStr);
                const newDate = new Date(currentDate);
                newDate.setDate(newDate.getDate() + daysOffset);
                const newDateStr = formatDate(newDate);
                
                // Проверяем, что новая дата в пределах текущего месяца (если он загружен)
                if (currentMonth && newDate.getMonth() + 1 !== currentMonth) {
                    alert('Новая дата должна быть в пределах текущего месяца');
                    return;
                }
                
                // Отправляем запрос на обновление даты (сохраняем исходную дату для возможного отката)
                const originalDate = draggedCell.attr('data-current-date');
                updateEntryDate(entryId, newDateStr, equipment, originalDate);
            }
        }
        
        // Убираем классы
        $('.schedule-cell').removeClass('drag-over-left drag-over-right drag-over-target dragging');
        draggedCell = null;
        originalDate = null;
    });
    
    // Обработчик выхода из ячейки
    $(document).on('dragleave', '.schedule-cell', function(e) {
        $('.schedule-cell').removeClass('drag-over-left drag-over-right drag-over-target');
    });
}

// Флаг для предотвращения множественных одновременных запросов
let isUpdatingEntryDate = false;

function updateEntryDate(entryId, newDateStr, equipment, originalDateStr) {
    // Предотвращаем множественные одновременные запросы
    if (isUpdatingEntryDate) {
        return;
    }
    
    isUpdatingEntryDate = true;
    
    $.ajax({
        url: '/api/diagnostics-schedule/entry/' + entryId + '/date',
        method: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({
            newDate: newDateStr
        }),
        success: function(response) {
            isUpdatingEntryDate = false;
            if (response.success) {
                // Перезагружаем график для текущего месяца
                if (currentScheduleId && currentMonth) {
                    loadMonthSchedule(currentScheduleId, currentMonth);
                }
            } else {
                // Недостаток времени или другая проблема - обрабатываем как нормальную ситуацию
                showDateUpdateError(response.message || 'Не удалось переместить наряд', entryId, originalDateStr, equipment);
            }
        },
        error: function(xhr, status, error) {
            isUpdatingEntryDate = false;
            // Обрабатываем только реальные ошибки (сетевые, 404 и т.д.)
            let errorMessage = 'Ошибка обновления даты';
            
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            } else if (xhr.status === 500) {
                errorMessage = 'Внутренняя ошибка сервера. Попробуйте позже.';
            } else if (xhr.status === 404) {
                errorMessage = 'Запись не найдена. Возможно, она была удалена.';
            } else {
                errorMessage = 'Ошибка обновления даты: ' + (error || 'Неизвестная ошибка');
            }
            
            showDateUpdateError(errorMessage, entryId, originalDateStr, equipment);
        }
    });
}

// Флаг для предотвращения множественных одновременных восстановлений
let isRestoringEntryDate = false;

function showDateUpdateError(message, entryId, originalDateStr, equipment) {
    // Показываем ошибку
    let errorText = '';
    let shouldRestore = true; // Флаг для определения, нужно ли восстанавливать дату
    
    // Проверяем, является ли ошибка связанной с недостатком времени
    if (message && message.includes('Недостаточно свободного времени')) {
        const dateMatch = message.match(/(\d{4}-\d{2}-\d{2})/);
        const dateStr = dateMatch ? dateMatch[1] : '';
        
        // Извлекаем детали из сообщения
        const requiredMatch = message.match(/Требуется: (\d+) минут/);
        const availableMatch = message.match(/доступно: (\d+) минут/);
        const workersMatch = message.match(/работников: (\d+)/);
        const shiftMatch = message.match(/смена: (\d+) часов/);
        
        errorText = '❌ Недостаточно свободного времени на выбранную дату';
        if (dateStr) {
            const date = new Date(dateStr);
            const formattedDate = date.toLocaleDateString('ru-RU', { 
                day: 'numeric', 
                month: 'long', 
                year: 'numeric' 
            });
            errorText += ' (' + formattedDate + ')';
        }
        errorText += '.\n\n';
        
        if (requiredMatch && availableMatch) {
            errorText += 'Требуется: ' + requiredMatch[1] + ' минут\n';
            errorText += 'Доступно: ' + availableMatch[1] + ' минут\n';
        }
        if (workersMatch && shiftMatch) {
            errorText += 'Работников: ' + workersMatch[1] + ', смена: ' + shiftMatch[1] + ' часов\n';
        }
        errorText += '\n💡 Рекомендации:\n';
        errorText += '• Выберите другую дату с меньшей загрузкой\n';
        errorText += '• Проверьте количество работников и длительность смены\n';
        errorText += '• Убедитесь, что на выбранной дате нет других диагностик для этого оборудования';
    } else if (message && message.includes('уже запланирована диагностика')) {
        errorText = '⚠️ ' + message + '\n\nНа выбранную дату уже запланирована диагностика для этого оборудования. Выберите другую дату.';
    } else if (message && message.includes('должна быть в пределах')) {
        errorText = '⚠️ ' + message;
    } else {
        errorText = '❌ Ошибка обновления даты:\n\n' + message;
    }
    
    // Показываем ошибку
    alert(errorText);
    
    // Восстанавливаем исходную дату после закрытия окна ошибки
    if (shouldRestore && entryId && originalDateStr && !isRestoringEntryDate) {
        restoreEntryDate(entryId, originalDateStr, equipment);
    }
}

function restoreEntryDate(entryId, originalDateStr, equipment) {
    // Предотвращаем множественные одновременные восстановления
    if (isRestoringEntryDate) {
        return;
    }
    
    isRestoringEntryDate = true;
    
    // Восстанавливаем исходную дату
    $.ajax({
        url: '/api/diagnostics-schedule/entry/' + entryId + '/date',
        method: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({
            newDate: originalDateStr
        }),
        success: function(response) {
            isRestoringEntryDate = false;
            if (response.success) {
                // Перезагружаем график для текущего месяца
                if (currentScheduleId && currentMonth) {
                    loadMonthSchedule(currentScheduleId, currentMonth);
                }
            } else {
                console.error('Ошибка при восстановлении даты:', response.message);
                // Перезагружаем график в любом случае, чтобы показать актуальное состояние
                if (currentScheduleId && currentMonth) {
                    loadMonthSchedule(currentScheduleId, currentMonth);
                }
            }
        },
        error: function(xhr, status, error) {
            isRestoringEntryDate = false;
            console.error('Ошибка при восстановлении даты:', error);
            // Перезагружаем график в любом случае, чтобы показать актуальное состояние
            if (currentScheduleId && currentMonth) {
                loadMonthSchedule(currentScheduleId, currentMonth);
            }
        }
    });
}

function setupDoubleClickHandler() {
    // Обработчик двойного клика на ячейки с одной диагностикой
    $(document).on('dblclick', '.schedule-cell.has-diagnostics[draggable="true"]', function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        const entryDataStr = $(this).attr('data-entry-data');
        if (!entryDataStr) {
            return;
        }
        
        const entry = JSON.parse(entryDataStr);
        
        // Показываем модальное окно
        showEntryStatusModal(entry);
    });
}

function showEntryStatusModal(entry) {
    // Создаем модальное окно, если его еще нет
    let modal = $('#entryStatusModal');
    if (modal.length === 0) {
        modal = $('<div>').attr('id', 'entryStatusModal').addClass('entry-status-modal');
        modal.html(`
            <div class="entry-status-modal-content">
                <span class="entry-status-modal-close">&times;</span>
                <h2>Статус наряда</h2>
                <div class="entry-status-info">
                    <p><strong>Оборудование:</strong> <span id="modalEquipment"></span></p>
                    <p><strong>Тип диагностики:</strong> <span id="modalDiagnosticsType"></span></p>
                    <p><strong>Дата:</strong> <span id="modalDate"></span></p>
                </div>
                <div class="entry-status-actions">
                    <button id="btnCompleted" class="btn-status btn-completed">Выполнено</button>
                    <button id="btnDefect" class="btn-status btn-defect">Обнаружен дефект</button>
                </div>
            </div>
        `);
        $('body').append(modal);
        
        // Обработчик закрытия модального окна
        modal.find('.entry-status-modal-close').on('click', function() {
            modal.hide();
        });
        
        // Обработчик клика вне модального окна
        modal.on('click', function(e) {
            if ($(e.target).hasClass('entry-status-modal')) {
                modal.hide();
            }
        });
        
        // Обработчик кнопки "Выполнено"
        modal.find('#btnCompleted').on('click', function() {
            const entryId = modal.attr('data-entry-id');
            updateEntryStatus(entryId, true, false);
            modal.hide();
        });
        
        // Обработчик кнопки "Обнаружен дефект"
        modal.find('#btnDefect').on('click', function() {
            const entryId = modal.attr('data-entry-id');
            const entryData = JSON.parse(modal.attr('data-entry'));
            openDiagnosticsReportForm(entryData);
            modal.hide();
        });
    }
    
    // Заполняем данные
    modal.find('#modalEquipment').text(entry.equipment);
    modal.find('#modalDiagnosticsType').text(entry.diagnosticsType.name);
    const date = new Date(entry.scheduledDate);
    modal.find('#modalDate').text(date.toLocaleDateString('ru-RU'));
    modal.attr('data-entry-id', entry.id);
    modal.attr('data-entry', JSON.stringify(entry));
    
    // Показываем модальное окно
    modal.show();
}

function updateEntryStatus(entryId, isCompleted, hasDefect) {
    $.ajax({
        url: '/api/diagnostics-schedule/entry/' + entryId + '/status',
        method: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({
            isCompleted: isCompleted,
            hasDefect: hasDefect
        }),
        success: function(response) {
            if (response.success) {
                // Перезагружаем график для текущего месяца
                if (currentScheduleId && currentMonth) {
                    loadMonthSchedule(currentScheduleId, currentMonth);
                }
            } else {
                alert('Ошибка при обновлении статуса: ' + (response.message || 'Неизвестная ошибка'));
            }
        },
        error: function(xhr, status, error) {
            let errorMessage = 'Ошибка обновления статуса';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            }
            alert(errorMessage);
        }
    });
}

function openDiagnosticsReportForm(entry) {
    // Формируем URL с параметрами для предзаполнения формы
    const params = new URLSearchParams();
    params.append('equipment', entry.equipment || '');
    params.append('area', entry.area || '');
    // Используем название типа диагностики для формы
    params.append('diagnostics_type', entry.diagnosticsType ? entry.diagnosticsType.name : '');
    params.append('detection_date', entry.scheduledDate || '');
    
    // Убеждаемся, что entry.id - это число
    const entryId = entry.id ? String(entry.id) : '';
    if (entryId) {
        params.append('entry_id', entryId);
    }
    params.append('return_to', 'schedule');
    
    // Открываем форму в новом окне или переходим на страницу
    const url = '/create-diagnostics-report?' + params.toString();
    console.log('Переход на форму создания отчета:', url);
    window.location.href = url;
}

function parseDate(dateStr) {
    if (!dateStr) return null;
    const parts = dateStr.split('-');
    if (parts.length !== 3) return null;
    return new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
}

function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function loadScheduleStats(scheduleId) {
    const showYearStats = $('#showYearStatsCheckbox').is(':checked');
    // Используем текущий месяц, если он установлен, иначе используем выбранный месяц из селекта
    let month = null;
    if (!showYearStats) {
        // Приоритет: currentMonth -> значение из селекта -> текущий месяц
        month = currentMonth;
        if (!month || month < 1 || month > 12) {
            month = parseInt($('#monthSelect').val());
        }
        if (!month || month < 1 || month > 12) {
            month = new Date().getMonth() + 1;
        }
    }
    
    const url = month ? 
        `/api/diagnostics-schedule/${scheduleId}/stats?month=${month}` : 
        `/api/diagnostics-schedule/${scheduleId}/stats`;
    
    $.ajax({
        url: url,
        method: 'GET',
        success: function(stats) {
            if (stats && typeof stats === 'object') {
                displayStats(stats, month);
                $('#scheduleStats').show();
            } else {
                console.error('Некорректный формат данных статистики:', stats);
            }
        },
        error: function(xhr, status, error) {
            console.error('Ошибка загрузки статистики:', error, xhr.responseText);
            alert('Ошибка загрузки статистики: ' + (xhr.responseJSON ? xhr.responseJSON.message : error));
        }
    });
}

function displayStats(stats, month) {
    if (!stats) {
        console.error('Статистика не получена');
        $('#statsContent').html('<p style="color: red;">Ошибка: данные статистики не получены</p>');
        return;
    }
    
    const monthNames = ['', 'Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 
                       'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
    const periodText = month ? ` за ${monthNames[month]}` : ' за год';
    
    let html = `<p style="margin-bottom: 10px; font-weight: bold; color: #666;">Статистика${periodText}:</p>`;
    html += '<table class="stats-table">';
    html += '<tr><th>Показатель</th><th>Запланировано</th><th>Продиагностировано</th><th>%</th></tr>';
    
    // Проверяем наличие данных
    const totalPlanned = stats.totalPlanned || 0;
    const totalCompleted = stats.totalCompleted || 0;
    const completionPercentage = stats.completionPercentage || 0;
    
    html += `<tr>
        <td><strong>Всего</strong></td>
        <td>${totalPlanned}</td>
        <td>${totalCompleted}</td>
        <td>${completionPercentage.toFixed(0)}%</td>
    </tr>`;
    
    if (stats.byType && typeof stats.byType === 'object') {
        Object.keys(stats.byType).forEach(function(typeCode) {
            const typeStats = stats.byType[typeCode];
            if (typeStats && typeof typeStats === 'object') {
                const typeName = diagnosticsTypes.find(t => t.code === typeCode)?.name || typeCode;
                const planned = typeStats.planned || 0;
                const completed = typeStats.completed || 0;
                const typePercentage = typeStats.completionPercentage || 0;
                html += `<tr>
                    <td>${typeName}</td>
                    <td>${planned}</td>
                    <td>${completed}</td>
                    <td>${typePercentage.toFixed(0)}%</td>
                </tr>`;
            }
        });
    }
    
    html += '</table>';
    $('#statsContent').html(html);
}

function updateLegend(types) {
    let html = '';
    types.forEach(function(type) {
        html += `<div class="legend-item">
            <span class="legend-color" style="background-color: ${type.colorCode || '#90EE90'};"></span>
            <strong>${type.code}</strong> - ${type.name}
        </div>`;
    });
    // Добавляем описание цветов статусов
    html += `<div class="legend-item">
        <span class="legend-color" style="background-color: #808080;"></span>
        <strong>Закрыто</strong>
    </div>`;
    html += `<div class="legend-item">
        <span class="legend-color" style="background-color: #FF6B6B;"></span>
        <strong>Просрочен</strong>
    </div>`;
    $('#legendContent').html(html);
    $('#legend').show();
}


function deleteSchedule() {
    if (!currentScheduleId) {
        alert('График не выбран');
        return;
    }
    
    const scheduleYear = $('#scheduleYearSelect option:selected').text();
    const confirmMessage = 'Вы уверены, что хотите удалить график на ' + scheduleYear + ' год?\n\nЭто действие нельзя отменить. Будут удалены все записи графика.';
    
    if (!confirm(confirmMessage)) {
        return;
    }
    
    $.ajax({
        url: '/api/diagnostics-schedule/' + currentScheduleId,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                alert('График успешно удален');
                currentScheduleId = null;
                scheduleData = null;
                $('#scheduleSelector').hide();
                $('#scheduleStats').hide();
                $('#legend').hide();
                $('#scheduleContainer').hide();
                $('#deleteScheduleBtn').hide();
                $('#addEquipmentToScheduleBtn').hide();
                $('#prevMonthBtn').hide();
                $('#nextMonthBtn').hide();
                loadExistingSchedules();
            } else {
                alert('Ошибка: ' + response.message);
            }
        },
        error: function(xhr, status, error) {
            const response = xhr.responseJSON;
            alert('Ошибка при удалении графика: ' + (response ? response.message : error));
        }
    });
}
