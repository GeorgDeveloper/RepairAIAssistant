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

function getFileName(path) {
    if (!path) return '';
    const parts = path.split('/');
    let fileName = parts[parts.length - 1];
    // Убираем UUID префикс, если есть
    if (fileName.includes('_')) {
        fileName = fileName.substring(fileName.indexOf('_') + 1);
    }
    return fileName;
}

function renderFileThumbnail(path, type) {
    if (!path) return '';
    
    const fileName = getFileName(path);
    // Убеждаемся, что путь начинается с /
    const normalizedPath = path.startsWith('/') ? path : '/' + path;
    
    // Правильно кодируем путь для URL - кодируем каждый сегмент отдельно
    // Это важно для правильной обработки пробелов и спецсимволов
    const pathSegments = normalizedPath.substring(1).split('/');
    const encodedSegments = pathSegments.map(segment => {
        // Кодируем каждый сегмент, но сохраняем структуру пути
        return encodeURIComponent(segment);
    });
    const encodedPath = '/' + encodedSegments.join('/');
    
    // Формируем URL для доступа к файлу
    const fileUrl = '/api/diagnostics/files' + encodedPath;
    
    // Для отладки
    console.log('Original path:', path);
    console.log('Encoded path:', encodedPath);
    console.log('File URL:', fileUrl);
    
    // Экранируем кавычки для использования в onclick
    const escapedFileUrl = fileUrl.replace(/'/g, "\\'");
    const escapedFileName = fileName.replace(/'/g, "\\'").replace(/"/g, "&quot;");
    
    if (type === 'photo' || /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName)) {
        // Это изображение - показываем только миниатюру без названия
        return '<div class="file-thumbnail" onclick="openFileModal(\'' + escapedFileUrl + '\', \'' + escapedFileName + '\', \'image\')">' +
               '<img src="' + fileUrl + '" alt="' + escapedFileName + '" onerror="this.style.display=\'none\'; this.nextElementSibling.style.display=\'block\';">' +
               '<span style="display:none;" class="file-icon">📷</span>' +
               '</div>';
    } else {
        // Это документ - показываем только иконку без названия
        const icon = /\.pdf$/i.test(fileName) ? '📄' : /\.(doc|docx)$/i.test(fileName) ? '📝' : '📎';
        return '<div class="file-thumbnail" onclick="openFileModal(\'' + escapedFileUrl + '\', \'' + escapedFileName + '\', \'document\')">' +
               '<span class="file-icon">' + icon + '</span>' +
               '</div>';
    }
}

function openFileModal(fileUrl, fileName, fileType) {
    const modal = document.getElementById('filePreviewModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalBody = document.getElementById('modalBody');
    const downloadLink = document.getElementById('downloadLink');
    
    modalTitle.textContent = fileName;
    downloadLink.href = fileUrl + '?download=true';
    downloadLink.download = fileName;
    
    if (fileType === 'image') {
        modalBody.innerHTML = '<img src="' + fileUrl + '" alt="' + fileName + '" style="max-width: 100%; max-height: 70vh;">';
    } else {
        // Для документов используем iframe или прямую ссылку
        if (/\.pdf$/i.test(fileName)) {
            modalBody.innerHTML = '<iframe src="' + fileUrl + '" style="width: 100%; height: 70vh; border: none;"></iframe>';
        } else {
            modalBody.innerHTML = '<div style="text-align: center; padding: 40px;">' +
                '<p>Просмотр документа недоступен. Используйте кнопку "Скачать" для загрузки файла.</p>' +
                '<p style="font-size: 48px; margin: 20px 0;">📄</p>' +
                '<p><strong>' + fileName + '</strong></p>' +
                '</div>';
        }
    }
    
    modal.style.display = 'block';
}

function closeFileModal() {
    document.getElementById('filePreviewModal').style.display = 'none';
}

/** Тип диагностики в строке от API (разные варианты имён полей в JSON). */
function rowDiagnosticsType(row) {
    if (!row) return '';
    var v = row.diagnostics_type;
    if (v == null || v === '') v = row.diagnosticsType;
    if (v == null || v === '') v = row.DIAGNOSTICS_TYPE;
    return v == null ? '' : String(v);
}

function getFileUrl(path) {
    if (!path) return '';
    var normalizedPath = path.startsWith('/') ? path : '/' + path;
    var pathSegments = normalizedPath.substring(1).split('/');
    var encodedSegments = pathSegments.map(function(segment) {
        return encodeURIComponent(segment);
    });
    var encodedPath = '/' + encodedSegments.join('/');
    return '/api/diagnostics/files' + encodedPath;
}

function generateReport(reportId) {
    // Получаем данные строки из таблицы
    var table = $('#diagnosticsTable').DataTable();
    var rowData = table.rows().data().toArray().find(function(row) {
        return row.id === reportId;
    });
    
    if (!rowData) {
        alert('Данные отчета не найдены');
        return;
    }
    
    // Формируем HTML для печати
    var printWindow = window.open('', '_blank');
    var htmlContent = '<!DOCTYPE html>' +
        '<html lang="ru">' +
        '<head>' +
        '<meta charset="UTF-8">' +
        '<title>Отчет диагностики #' + reportId + '</title>' +
        '<style>' +
        '@media print {' +
        '  body { margin: 0; padding: 20px; }' +
        '  .no-print { display: none; }' +
        '  @page { margin: 1cm; }' +
        '}' +
        'body { font-family: Arial, sans-serif; font-size: 12px; padding: 20px; }' +
        'h1 { text-align: center; margin-bottom: 30px; font-size: 18px; }' +
        'h3 { margin-top: 20px; margin-bottom: 10px; font-size: 14px; }' +
        'table { width: 100%; border-collapse: collapse; margin-bottom: 20px; page-break-inside: avoid; }' +
        'table th, table td { border: 1px solid #000; padding: 8px; text-align: left; vertical-align: top; }' +
        'table th { background-color: #f0f0f0; font-weight: bold; width: 200px; }' +
        'table td { width: auto; }' +
        '.photo-container { margin-top: 20px; page-break-inside: avoid; }' +
        '.photo-container img { max-width: 100%; max-height: 400px; margin: 10px 0; border: 1px solid #ccc; display: block; }' +
        '.print-button { margin: 20px 0; text-align: center; }' +
        '.print-button button { padding: 10px 20px; font-size: 16px; cursor: pointer; background-color: #007bff; color: white; border: none; border-radius: 4px; }' +
        '</style>' +
        '</head>' +
        '<body>' +
        '<h1>ОТЧЕТ ДИАГНОСТИКИ</h1>' +
        '<div class="print-button no-print">' +
        '<button onclick="window.print()">Печать</button>' +
        '</div>' +
        '<table>' +
        '<tr><th>ID</th><td>' + (rowData.id || '') + '</td></tr>' +
        '<tr><th>ДАТА ОБНАРУЖЕНИЯ</th><td>' + formatDate(rowData.detection_date) + '</td></tr>' +
        '<tr><th>ТИП ДИАГНОСТИКИ</th><td>' + (rowData.diagnostics_type || '') + '</td></tr>' +
        '<tr><th>ОБОРУДОВАНИЕ</th><td>' + (rowData.equipment || '') + '</td></tr>' +
        '<tr><th>УЗЕЛ</th><td>' + (rowData.node || '') + '</td></tr>' +
        '<tr><th>УЧАСТОК</th><td>' + (rowData.area || '') + '</td></tr>' +
        '<tr><th>НЕИСПРАВНОСТЬ</th><td>' + (rowData.malfunction || '') + '</td></tr>' +
        '<tr><th>ДОП. КОМПЛЕКТ</th><td>' + (rowData.additional_kit || '') + '</td></tr>' +
        '<tr><th>ПРИЧИНЫ</th><td>' + (rowData.causes || '') + '</td></tr>' +
        '</table>';
    
    // Добавляем фото, если есть
    if (rowData.photo_path) {
        var photoUrl = getFileUrl(rowData.photo_path);
        htmlContent += '<div class="photo-container">' +
            '<h3>ФОТО</h3>' +
            '<img src="' + photoUrl + '" alt="Фото" onerror="this.style.display=\'none\';">' +
            '</div>';
    }
    
    htmlContent += '</body></html>';
    
    printWindow.document.write(htmlContent);
    printWindow.document.close();
    
    // Автоматически открываем диалог печати после загрузки изображений
    if (rowData.photo_path) {
        var img = new Image();
        img.onload = function() {
            setTimeout(function() {
                printWindow.print();
            }, 500);
        };
        img.onerror = function() {
            setTimeout(function() {
                printWindow.print();
            }, 500);
        };
        img.src = getFileUrl(rowData.photo_path);
    } else {
        setTimeout(function() {
            printWindow.print();
        }, 500);
    }
}

function generateFullReportFromTable(reportId) {
    // Получаем данные строки из таблицы
    var table = $('#diagnosticsTable').DataTable();
    var rowData = table.rows().data().toArray().find(function(row) {
        return row.id === reportId;
    });
    
    if (!rowData) {
        alert('Данные отчета не найдены');
        return;
    }
    
    generateFullReport(rowData);
}

function generateFullReport(rowData) {
    // Формируем HTML для полного отчета из всех заполненных полей
    var printWindow = window.open('', '_blank');
    var htmlContent = '<!DOCTYPE html>' +
        '<html lang="ru">' +
        '<head>' +
        '<meta charset="UTF-8">' +
        '<title>Итоговый отчет диагностики #' + rowData.id + '</title>' +
        '<style>' +
        '@media print {' +
        '  body { margin: 0; padding: 20px; }' +
        '  .no-print { display: none; }' +
        '  @page { margin: 1cm; }' +
        '}' +
        'body { font-family: Arial, sans-serif; font-size: 12px; padding: 20px; }' +
        'h1 { text-align: center; margin-bottom: 30px; font-size: 18px; }' +
        'h2 { margin-top: 30px; margin-bottom: 15px; font-size: 16px; border-bottom: 2px solid #000; padding-bottom: 5px; }' +
        'h3 { margin-top: 20px; margin-bottom: 10px; font-size: 14px; }' +
        'table { width: 100%; border-collapse: collapse; margin-bottom: 20px; page-break-inside: avoid; }' +
        'table th, table td { border: 1px solid #000; padding: 8px; text-align: left; vertical-align: top; }' +
        'table th { background-color: #f0f0f0; font-weight: bold; width: 200px; }' +
        'table td { width: auto; }' +
        '.photo-container { margin-top: 20px; page-break-inside: avoid; }' +
        '.photo-container img { max-width: 100%; max-height: 400px; margin: 10px 0; border: 1px solid #ccc; display: block; }' +
        '.print-button { margin: 20px 0; text-align: center; }' +
        '.print-button button { padding: 10px 20px; font-size: 16px; cursor: pointer; background-color: #007bff; color: white; border: none; border-radius: 4px; }' +
        '.section { margin-bottom: 30px; }' +
        '</style>' +
        '</head>' +
        '<body>' +
        '<h1>ИТОГОВЫЙ ОТЧЕТ ДИАГНОСТИКИ</h1>' +
        '<div class="print-button no-print">' +
        '<button onclick="window.print()">Печать</button>' +
        '</div>';
    
    // Основная информация
    htmlContent += '<div class="section">' +
        '<h2>ОСНОВНАЯ ИНФОРМАЦИЯ</h2>' +
        '<table>';
    
    if (rowData.id) htmlContent += '<tr><th>ID</th><td>' + rowData.id + '</td></tr>';
    if (rowData.detection_date) htmlContent += '<tr><th>ДАТА ОБНАРУЖЕНИЯ</th><td>' + formatDate(rowData.detection_date) + '</td></tr>';
    if (rowData.diagnostics_type) htmlContent += '<tr><th>ТИП ДИАГНОСТИКИ</th><td>' + rowData.diagnostics_type + '</td></tr>';
    if (rowData.equipment) htmlContent += '<tr><th>ОБОРУДОВАНИЕ</th><td>' + rowData.equipment + '</td></tr>';
    if (rowData.node) htmlContent += '<tr><th>УЗЕЛ</th><td>' + rowData.node + '</td></tr>';
    if (rowData.area) htmlContent += '<tr><th>УЧАСТОК</th><td>' + rowData.area + '</td></tr>';
    if (rowData.malfunction) htmlContent += '<tr><th>НЕИСПРАВНОСТЬ</th><td>' + rowData.malfunction + '</td></tr>';
    if (rowData.additional_kit) htmlContent += '<tr><th>ДОП. КОМПЛЕКТ</th><td>' + rowData.additional_kit + '</td></tr>';
    if (rowData.causes) htmlContent += '<tr><th>ПРИЧИНЫ</th><td>' + rowData.causes + '</td></tr>';
    if (rowData.report) htmlContent += '<tr><th>ОТЧЕТ</th><td>' + rowData.report + '</td></tr>';
    if (rowData.status) htmlContent += '<tr><th>СТАТУС</th><td>' + rowData.status + '</td></tr>';
    
    htmlContent += '</table></div>';
    
    // Информация об устранении (если статус ЗАКРЫТО)
    if (rowData.status === 'ЗАКРЫТО') {
        htmlContent += '<div class="section">' +
            '<h2>ИНФОРМАЦИЯ ОБ УСТРАНЕНИИ</h2>' +
            '<table>';
        
        if (rowData.elimination_date) htmlContent += '<tr><th>ДАТА УСТРАНЕНИЯ</th><td>' + formatDate(rowData.elimination_date) + '</td></tr>';
        if (rowData.condition_after_elimination) htmlContent += '<tr><th>СОСТОЯНИЕ ПОСЛЕ УСТРАНЕНИЯ</th><td>' + rowData.condition_after_elimination + '</td></tr>';
        if (rowData.responsible) htmlContent += '<tr><th>ОТВЕТСТВЕННЫЙ</th><td>' + rowData.responsible + '</td></tr>';
        if (rowData.non_elimination_reason) htmlContent += '<tr><th>ПРИЧИНА НЕУСТРАНЕНИЯ</th><td>' + rowData.non_elimination_reason + '</td></tr>';
        if (rowData.measures) htmlContent += '<tr><th>МЕРОПРИЯТИЯ</th><td>' + rowData.measures + '</td></tr>';
        if (rowData.comments) htmlContent += '<tr><th>КОММЕНТАРИИ</th><td>' + rowData.comments + '</td></tr>';
        
        htmlContent += '</table></div>';
    }
    
    // Фото (начальные)
    if (rowData.photo_path) {
        var photoUrl = getFileUrl(rowData.photo_path);
        htmlContent += '<div class="section photo-container">' +
            '<h2>ФОТО</h2>' +
            '<img src="' + photoUrl + '" alt="Фото" onerror="this.style.display=\'none\';">' +
            '</div>';
    }
    
    // Фото итог
    if (rowData.photo_result_path) {
        var photoResultUrl = getFileUrl(rowData.photo_result_path);
        htmlContent += '<div class="section photo-container">' +
            '<h2>ФОТО ИТОГ</h2>' +
            '<img src="' + photoResultUrl + '" alt="Фото итог" onerror="this.style.display=\'none\';">' +
            '</div>';
    }
    
    // Документы
    if (rowData.document_path || rowData.document_result_path) {
        htmlContent += '<div class="section">' +
            '<h2>ДОКУМЕНТЫ</h2>';
        if (rowData.document_path) {
            htmlContent += '<p><strong>Документ:</strong> ' + getFileName(rowData.document_path) + '</p>';
        }
        if (rowData.document_result_path) {
            htmlContent += '<p><strong>Документ итог:</strong> ' + getFileName(rowData.document_result_path) + '</p>';
        }
        htmlContent += '</div>';
    }
    
    // Даты создания и обновления
    htmlContent += '<div class="section">' +
        '<h2>СЛУЖЕБНАЯ ИНФОРМАЦИЯ</h2>' +
        '<table>';
    if (rowData.created_at) htmlContent += '<tr><th>Создано</th><td>' + formatDateTime(rowData.created_at) + '</td></tr>';
    if (rowData.updated_at) htmlContent += '<tr><th>Обновлено</th><td>' + formatDateTime(rowData.updated_at) + '</td></tr>';
    htmlContent += '</table></div>';
    
    htmlContent += '</body></html>';
    
    printWindow.document.write(htmlContent);
    printWindow.document.close();
    
    // Автоматически открываем диалог печати после загрузки изображений
    var imagesToLoad = [];
    if (rowData.photo_path) imagesToLoad.push(getFileUrl(rowData.photo_path));
    if (rowData.photo_result_path) imagesToLoad.push(getFileUrl(rowData.photo_result_path));
    
    if (imagesToLoad.length > 0) {
        var loadedCount = 0;
        imagesToLoad.forEach(function(url) {
            var img = new Image();
            img.onload = function() {
                loadedCount++;
                if (loadedCount === imagesToLoad.length) {
                    setTimeout(function() {
                        printWindow.print();
                    }, 500);
                }
            };
            img.onerror = function() {
                loadedCount++;
                if (loadedCount === imagesToLoad.length) {
                    setTimeout(function() {
                        printWindow.print();
                    }, 500);
                }
            };
            img.src = url;
        });
    } else {
        setTimeout(function() {
            printWindow.print();
        }, 500);
    }
}

function closeStatusModal() {
    document.getElementById('statusChangeModal').style.display = 'none';
}

function openStatusModal(reportId, currentStatus) {
    var modal = document.getElementById('statusChangeModal');
    var modalBody = document.getElementById('statusModalBody');
    
    var html = '<div class="form-group">' +
        '<label>Выберите новый статус:</label>' +
        '<select id="newStatus">' +
        '<option value="ОТКРЫТО"' + (currentStatus === 'ОТКРЫТО' ? ' selected' : '') + '>ОТКРЫТО</option>' +
        '<option value="В РАБОТЕ"' + (currentStatus === 'В РАБОТЕ' ? ' selected' : '') + '>В РАБОТЕ</option>' +
        '<option value="ЗАКРЫТО"' + (currentStatus === 'ЗАКРЫТО' ? ' selected' : '') + '>ЗАКРЫТО</option>' +
        '</select>' +
        '</div>' +
        '<div id="workFields" class="form-group" style="display: none;">' +
        '<label>Ответственный:</label>' +
        '<input type="text" id="responsible" placeholder="Введите ответственного">' +
        '</div>' +
        '<div id="closedFields" style="display: none;">' +
        '<div class="form-group">' +
        '<label>Дата устранения:</label>' +
        '<input type="date" id="eliminationDate">' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Состояние после устранения:</label>' +
        '<textarea id="conditionAfterElimination" rows="3" placeholder="Опишите состояние"></textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Ответственный:</label>' +
        '<input type="text" id="responsibleClosed" placeholder="Введите ответственного">' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Причина неустранения:</label>' +
        '<textarea id="nonEliminationReason" rows="3" placeholder="Опишите причину неустранения"></textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Мероприятия:</label>' +
        '<textarea id="measures" rows="3" placeholder="Опишите мероприятия"></textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Комментарии:</label>' +
        '<textarea id="comments" rows="3" placeholder="Введите комментарии"></textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Фото итог:</label>' +
        '<input type="file" id="photoResult" accept="image/*">' +
        '<small>Можно загрузить изображение (JPG, PNG и т.д.)</small>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Отчет итог:</label>' +
        '<input type="file" id="documentResult" accept=".pdf,.doc,.docx">' +
        '<small>Можно загрузить документ (PDF, DOC, DOCX)</small>' +
        '</div>' +
        '</div>' +
        '<div class="form-actions">' +
        '<button id="saveStatusBtn" onclick="saveStatusChange(' + reportId + ')" class="btn-apply">Сохранить</button>' +
        '<button onclick="closeStatusModal()" class="btn-cancel">Отмена</button>' +
        '</div>';
    
    modalBody.innerHTML = html;
    modal.style.display = 'block';
    
    // Обработчик изменения статуса
    document.getElementById('newStatus').addEventListener('change', function() {
        var status = this.value;
        document.getElementById('workFields').style.display = (status === 'В РАБОТЕ') ? 'block' : 'none';
        document.getElementById('closedFields').style.display = (status === 'ЗАКРЫТО') ? 'block' : 'none';
    });
    
    // Инициализация полей при открытии
    var initialStatus = document.getElementById('newStatus').value;
    document.getElementById('workFields').style.display = (initialStatus === 'В РАБОТЕ') ? 'block' : 'none';
    document.getElementById('closedFields').style.display = (initialStatus === 'ЗАКРЫТО') ? 'block' : 'none';
}

function saveStatusChange(reportId) {
    var formData = new FormData();
    var newStatus = document.getElementById('newStatus').value;
    formData.append('status', newStatus);
    
    // Для статуса "В РАБОТЕ" добавляем ответственного
    if (newStatus === 'В РАБОТЕ') {
        var responsible = document.getElementById('responsible').value;
        if (responsible && responsible.trim()) {
            formData.append('responsible', responsible.trim());
        }
    }
    
    // Для статуса "ЗАКРЫТО" добавляем все поля
    if (newStatus === 'ЗАКРЫТО') {
        var eliminationDate = document.getElementById('eliminationDate').value;
        if (eliminationDate && eliminationDate.trim()) {
            formData.append('elimination_date', eliminationDate.trim());
        }
        
        var conditionAfterElimination = document.getElementById('conditionAfterElimination').value;
        if (conditionAfterElimination && conditionAfterElimination.trim()) {
            formData.append('condition_after_elimination', conditionAfterElimination.trim());
        }
        
        var responsibleClosed = document.getElementById('responsibleClosed').value;
        if (responsibleClosed && responsibleClosed.trim()) {
            formData.append('responsible', responsibleClosed.trim());
        }
        
        var nonEliminationReason = document.getElementById('nonEliminationReason').value;
        if (nonEliminationReason && nonEliminationReason.trim()) {
            formData.append('non_elimination_reason', nonEliminationReason.trim());
        }
        
        var measures = document.getElementById('measures').value;
        if (measures && measures.trim()) {
            formData.append('measures', measures.trim());
        }
        
        var comments = document.getElementById('comments').value;
        if (comments && comments.trim()) {
            formData.append('comments', comments.trim());
        }
        
        var photoResult = document.getElementById('photoResult').files[0];
        if (photoResult) {
            formData.append('photo_result', photoResult);
        }
        
        var documentResult = document.getElementById('documentResult').files[0];
        if (documentResult) {
            formData.append('document_result', documentResult);
        }
    }
    
    // Показываем индикатор загрузки
    var saveButton = document.getElementById('saveStatusBtn');
    var originalText = saveButton ? saveButton.textContent : 'Сохранить';
    if (saveButton) {
        saveButton.disabled = true;
        saveButton.textContent = 'Сохранение...';
    }
    
    fetch('/api/diagnostics/' + reportId, {
        method: 'PUT',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error('HTTP ' + response.status + ': ' + text);
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert('Статус успешно обновлен');
            closeStatusModal();
            $('#diagnosticsTable').DataTable().ajax.reload();
        } else {
            alert('Ошибка: ' + (data.message || 'Не удалось обновить статус'));
            saveButton.disabled = false;
            saveButton.textContent = originalText;
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Ошибка при обновлении статуса: ' + error.message);
        saveButton.disabled = false;
        saveButton.textContent = originalText;
    });
}

// Закрытие модального окна при клике вне его
window.onclick = function(event) {
    const fileModal = document.getElementById('filePreviewModal');
    const statusModal = document.getElementById('statusChangeModal');
    if (event.target === fileModal) {
        closeFileModal();
    }
    if (event.target === statusModal) {
        closeStatusModal();
    }
    const updateModal = document.getElementById('updateReportModal');
    if (event.target === updateModal) {
        closeUpdateModal();
    }
}

function closeUpdateModal() {
    document.getElementById('updateReportModal').style.display = 'none';
}

function deleteReport(reportId) {
    if (!confirm('Вы уверены, что хотите удалить этот отчет? Это действие нельзя отменить.')) {
        return;
    }
    
    fetch('/api/diagnostics/' + reportId, {
        method: 'DELETE'
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error('HTTP ' + response.status + ': ' + text);
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert('Отчет успешно удален');
            $('#diagnosticsTable').DataTable().ajax.reload();
        } else {
            alert('Ошибка: ' + (data.message || 'Не удалось удалить отчет'));
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Ошибка при удалении отчета: ' + error.message);
    });
}

function openUpdateModal(reportId) {
    // Получаем данные строки из таблицы
    var table = $('#diagnosticsTable').DataTable();
    var rowData = table.rows().data().toArray().find(function(row) {
        return row.id === reportId;
    });
    
    if (!rowData) {
        alert('Данные отчета не найдены');
        return;
    }
    
    var modal = document.getElementById('updateReportModal');
    var modalBody = document.getElementById('updateModalBody');
    
    // Формируем HTML для формы обновления
    var html = '<form id="updateReportForm">' +
        '<div class="form-group">' +
        '<label>Дата обнаружения:</label>' +
        '<input type="date" id="updateDetectionDate" value="' + (rowData.detection_date ? formatDateForInput(rowData.detection_date) : '') + '">' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Тип диагностики:</label>' +
        '<select id="updateDiagnosticsType">' +
        '<option value="">Выберите тип диагностики</option>' +
        '<option value="Акустическая"' + (rowData.diagnostics_type === 'Акустическая' ? ' selected' : '') + '>Акустическая</option>' +
        '<option value="Вибродиагностика"' + (rowData.diagnostics_type === 'Вибродиагностика' ? ' selected' : '') + '>Вибродиагностика</option>' +
        '<option value="Конденсатоотводчики"' + (rowData.diagnostics_type === 'Конденсатоотводчики' ? ' selected' : '') + '>Конденсатоотводчики</option>' +
        '<option value="Термодиагностика"' + (rowData.diagnostics_type === 'Термодиагностика' ? ' selected' : '') + '>Термодиагностика</option>' +
        '</select>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Участок:</label>' +
        '<select id="updateArea">' +
        '<option value="">Выберите участок</option>' +
        '</select>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Оборудование:</label>' +
        '<select id="updateEquipment">' +
        '<option value="">Выберите оборудование</option>' +
        '</select>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Узел:</label>' +
        '<input type="text" id="updateNode" value="' + escapeHtml(rowData.node || '') + '" placeholder="Введите узел">' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Неисправность:</label>' +
        '<textarea id="updateMalfunction" rows="3" placeholder="Опишите неисправность">' + escapeHtml(rowData.malfunction || '') + '</textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Доп. комплект:</label>' +
        '<input type="text" id="updateAdditionalKit" value="' + escapeHtml(rowData.additional_kit || '') + '" placeholder="Введите доп. комплект">' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Причины:</label>' +
        '<textarea id="updateCauses" rows="3" placeholder="Опишите причины">' + escapeHtml(rowData.causes || '') + '</textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Фото:</label>';
    
    if (rowData.photo_path) {
        var photoUrl = getFileUrl(rowData.photo_path);
        var photoFileName = getFileName(rowData.photo_path);
        html += '<div class="file-preview">' +
            '<img src="' + photoUrl + '" alt="Фото">' +
            '<div style="margin-top: 5px;">' +
            '<span>' + escapeHtml(photoFileName) + '</span> ' +
            '<button type="button" onclick="deleteFileFromReport(' + reportId + ', \'photo\')" class="btn-cancel" style="padding: 5px 10px; margin-left: 10px; font-size: 12px;">Удалить</button>' +
            '</div>' +
            '</div>';
    }
    html += '<input type="file" id="updatePhoto" accept="image/*">' +
        '<small>Можно загрузить изображение (JPG, PNG и т.д.)</small>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Документ:</label>';
    
    if (rowData.document_path) {
        var docUrl = getFileUrl(rowData.document_path);
        var docFileName = getFileName(rowData.document_path);
        html += '<div class="file-preview">' +
            '<a href="' + docUrl + '" target="_blank">' + escapeHtml(docFileName) + '</a> ' +
            '<button type="button" onclick="deleteFileFromReport(' + reportId + ', \'document\')" class="btn-cancel" style="padding: 5px 10px; margin-left: 10px; font-size: 12px;">Удалить</button>' +
            '</div>';
    }
    html += '<input type="file" id="updateDocument" accept=".pdf,.doc,.docx">' +
        '<small>Можно загрузить документ (PDF, DOC, DOCX)</small>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Статус:</label>' +
        '<select id="updateStatus">' +
        '<option value="ОТКРЫТО"' + (rowData.status === 'ОТКРЫТО' ? ' selected' : '') + '>ОТКРЫТО</option>' +
        '<option value="В РАБОТЕ"' + (rowData.status === 'В РАБОТЕ' ? ' selected' : '') + '>В РАБОТЕ</option>' +
        '<option value="ЗАКРЫТО"' + (rowData.status === 'ЗАКРЫТО' ? ' selected' : '') + '>ЗАКРЫТО</option>' +
        '</select>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Дата устранения:</label>' +
        '<input type="date" id="updateEliminationDate" value="' + (rowData.elimination_date ? formatDateForInput(rowData.elimination_date) : '') + '">' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Состояние после устранения:</label>' +
        '<textarea id="updateConditionAfterElimination" rows="3" placeholder="Опишите состояние">' + escapeHtml(rowData.condition_after_elimination || '') + '</textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Ответственный:</label>' +
        '<input type="text" id="updateResponsible" value="' + escapeHtml(rowData.responsible || '') + '" placeholder="Введите ответственного">' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Причина неустранения:</label>' +
        '<textarea id="updateNonEliminationReason" rows="3" placeholder="Опишите причину неустранения">' + escapeHtml(rowData.non_elimination_reason || '') + '</textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Мероприятия:</label>' +
        '<textarea id="updateMeasures" rows="3" placeholder="Опишите мероприятия">' + escapeHtml(rowData.measures || '') + '</textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Комментарии:</label>' +
        '<textarea id="updateComments" rows="3" placeholder="Введите комментарии">' + escapeHtml(rowData.comments || '') + '</textarea>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Фото итог:</label>';
    
    if (rowData.photo_result_path) {
        var photoResultUrl = getFileUrl(rowData.photo_result_path);
        var photoResultFileName = getFileName(rowData.photo_result_path);
        html += '<div class="file-preview">' +
            '<img src="' + photoResultUrl + '" alt="Фото итог">' +
            '<div style="margin-top: 5px;">' +
            '<span>' + escapeHtml(photoResultFileName) + '</span> ' +
            '<button type="button" onclick="deleteFileFromReport(' + reportId + ', \'photo_result\')" class="btn-cancel" style="padding: 5px 10px; margin-left: 10px; font-size: 12px;">Удалить</button>' +
            '</div>' +
            '</div>';
    }
    html += '<input type="file" id="updatePhotoResult" accept="image/*">' +
        '<small>Можно загрузить изображение (JPG, PNG и т.д.)</small>' +
        '</div>' +
        '<div class="form-group">' +
        '<label>Отчет итог:</label>';
    
    if (rowData.document_result_path) {
        var docResultUrl = getFileUrl(rowData.document_result_path);
        var docResultFileName = getFileName(rowData.document_result_path);
        html += '<div class="file-preview">' +
            '<a href="' + docResultUrl + '" target="_blank">' + escapeHtml(docResultFileName) + '</a> ' +
            '<button type="button" onclick="deleteFileFromReport(' + reportId + ', \'document_result\')" class="btn-cancel" style="padding: 5px 10px; margin-left: 10px; font-size: 12px;">Удалить</button>' +
            '</div>';
    }
    html += '<input type="file" id="updateDocumentResult" accept=".pdf,.doc,.docx">' +
        '<small>Можно загрузить документ (PDF, DOC, DOCX)</small>' +
        '</div>' +
        '<div class="form-actions">' +
        '<button type="button" onclick="saveReportUpdate(' + reportId + ')" class="btn-apply">Сохранить</button>' +
        '<button type="button" onclick="closeUpdateModal()" class="btn-cancel">Отмена</button>' +
        '</div>' +
        '</form>';
    
    modalBody.innerHTML = html;
    modal.style.display = 'block';
    
    // Загружаем участки
    $.ajax({
        url: '/dashboard/diagnostics/areas',
        method: 'GET',
        success: function(areas) {
            const areaSelect = $('#updateArea');
            areas.forEach(function(area) {
                const areaName = area.area || area.name;
                if (areaName) {
                    areaSelect.append($('<option></option>').val(areaName).text(areaName));
                }
            });
            // Устанавливаем выбранное значение
            if (rowData.area) {
                areaSelect.val(rowData.area);
            }
            // Загружаем оборудование для выбранного участка
            loadEquipmentForUpdate(rowData.area, rowData.equipment);
            
            // Обработчик изменения участка (добавляем после загрузки)
            areaSelect.off('change.updateModal').on('change.updateModal', function() {
                const selectedArea = $(this).val();
                loadEquipmentForUpdate(selectedArea, null);
            });
        },
        error: function() {
            console.error('Ошибка при загрузке участков');
        }
    });
}

function loadEquipmentForUpdate(area, selectedEquipment) {
    const equipmentSelect = $('#updateEquipment');
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
            // Устанавливаем выбранное значение
            if (selectedEquipment) {
                equipmentSelect.val(selectedEquipment);
            }
        },
        error: function() {
            console.error('Ошибка при загрузке оборудования');
        }
    });
}

function escapeHtml(text) {
    if (!text) return '';
    var map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

function formatDateForInput(dateValue) {
    if (!dateValue) return '';
    var d = new Date(dateValue);
    if (isNaN(d.getTime())) return '';
    var pad = (n) => String(n).padStart(2,'0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`;
}

function deleteFileFromReport(reportId, fileType) {
    if (!confirm('Вы уверены, что хотите удалить этот файл?')) {
        return;
    }
    
    fetch('/api/diagnostics/' + reportId + '/file?file_type=' + fileType, {
        method: 'DELETE'
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error('HTTP ' + response.status + ': ' + text);
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert('Файл успешно удален');
            $('#diagnosticsTable').DataTable().ajax.reload();
            // Закрываем модальное окно обновления и открываем заново для обновления данных
            closeUpdateModal();
            setTimeout(function() {
                openUpdateModal(reportId);
            }, 500);
        } else {
            alert('Ошибка: ' + (data.message || 'Не удалось удалить файл'));
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Ошибка при удалении файла: ' + error.message);
    });
}

function saveReportUpdate(reportId) {
    var formData = new FormData();
    
    var detectionDate = document.getElementById('updateDetectionDate').value;
    if (detectionDate) {
        formData.append('detection_date', detectionDate);
    }
    
    var diagnosticsType = document.getElementById('updateDiagnosticsType').value;
    if (diagnosticsType) {
        formData.append('diagnostics_type', diagnosticsType);
    }
    
    var equipment = document.getElementById('updateEquipment').value;
    if (equipment) {
        formData.append('equipment', equipment);
    }
    
    var node = document.getElementById('updateNode').value;
    if (node) {
        formData.append('node', node);
    }
    
    var area = document.getElementById('updateArea').value;
    if (area) {
        formData.append('area', area);
    }
    
    var malfunction = document.getElementById('updateMalfunction').value;
    if (malfunction) {
        formData.append('malfunction', malfunction);
    }
    
    var additionalKit = document.getElementById('updateAdditionalKit').value;
    if (additionalKit) {
        formData.append('additional_kit', additionalKit);
    }
    
    var causes = document.getElementById('updateCauses').value;
    if (causes) {
        formData.append('causes', causes);
    }
    
    var photo = document.getElementById('updatePhoto').files[0];
    if (photo) {
        formData.append('photo', photo);
    }
    
    var documentFile = document.getElementById('updateDocument').files[0];
    if (documentFile) {
        formData.append('document', documentFile);
    }
    
    var status = document.getElementById('updateStatus').value;
    if (status) {
        formData.append('status', status);
    }
    
    var eliminationDate = document.getElementById('updateEliminationDate').value;
    if (eliminationDate) {
        formData.append('elimination_date', eliminationDate);
    }
    
    var conditionAfterElimination = document.getElementById('updateConditionAfterElimination').value;
    if (conditionAfterElimination) {
        formData.append('condition_after_elimination', conditionAfterElimination);
    }
    
    var responsible = document.getElementById('updateResponsible').value;
    if (responsible) {
        formData.append('responsible', responsible);
    }
    
    var nonEliminationReason = document.getElementById('updateNonEliminationReason').value;
    if (nonEliminationReason) {
        formData.append('non_elimination_reason', nonEliminationReason);
    }
    
    var measures = document.getElementById('updateMeasures').value;
    if (measures) {
        formData.append('measures', measures);
    }
    
    var comments = document.getElementById('updateComments').value;
    if (comments) {
        formData.append('comments', comments);
    }
    
    var photoResult = document.getElementById('updatePhotoResult').files[0];
    if (photoResult) {
        formData.append('photo_result', photoResult);
    }
    
    var documentResult = document.getElementById('updateDocumentResult').files[0];
    if (documentResult) {
        formData.append('document_result', documentResult);
    }
    
    var saveButton = document.querySelector('#updateReportForm button[onclick*="saveReportUpdate"]');
    var originalText = saveButton ? saveButton.textContent : 'Сохранить';
    if (saveButton) {
        saveButton.disabled = true;
        saveButton.textContent = 'Сохранение...';
    }
    
    fetch('/api/diagnostics/' + reportId + '/update', {
        method: 'PUT',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error('HTTP ' + response.status + ': ' + text);
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert('Отчет успешно обновлен');
            closeUpdateModal();
            $('#diagnosticsTable').DataTable().ajax.reload();
        } else {
            alert('Ошибка: ' + (data.message || 'Не удалось обновить отчет'));
            if (saveButton) {
                saveButton.disabled = false;
                saveButton.textContent = originalText;
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Ошибка при обновлении отчета: ' + error.message);
        if (saveButton) {
            saveButton.disabled = false;
            saveButton.textContent = originalText;
        }
    });
}

function formatDetailsRow(d) {
    // Определяем порядок полей для отображения в детальной таблице
    var fieldOrder = [
        'id',
        'detection_date',
        'diagnostics_type',
        'equipment',
        'node',
        'area',
        'malfunction',
        'additional_kit',
        'causes',
        'photo_path',
        'document_path',
        'report',
        'status',
        'elimination_date',
        'condition_after_elimination',
        'responsible',
        'non_elimination_reason',
        'measures',
        'comments',
        'photo_result_path',
        'document_result_path',
        'created_at',
        'updated_at'
    ];
    
    // Переводим ключи на русский для отображения
    var keyTranslations = {
        'id': 'ID',
        'detection_date': 'Дата обнаружения',
        'diagnostics_type': 'Тип диагностики',
        'equipment': 'Оборудование',
        'node': 'Узел',
        'area': 'Участок',
        'malfunction': 'Неисправность',
        'additional_kit': 'Доп. комплект',
        'causes': 'Причины',
        'photo_path': 'Фото',
        'document_path': 'Документ',
        'photo_result_path': 'Фото итог',
        'document_result_path': 'Отчет итог',
        'report': 'Отчет',
        'status': 'Статус',
        'elimination_date': 'Дата устранения',
        'condition_after_elimination': 'Состояние после устранения',
        'responsible': 'Ответственный',
        'non_elimination_reason': 'Причина неустранения',
        'measures': 'Мероприятия',
        'comments': 'Комментарии',
        'created_at': 'Создано',
        'updated_at': 'Обновлено'
    };
    
    var rows = '';
    
    // Сначала обрабатываем поля в заданном порядке
    fieldOrder.forEach(function(key) {
        if (d.hasOwnProperty(key)) {
            var value = d[key];
            if (value === null || value === undefined) { value = ''; }
            
            // Специальная обработка для файлов - показываем как ссылки
            if (key === 'photo_path' || key === 'document_path' || key === 'photo_result_path' || key === 'document_result_path') {
                if (value) {
                    var fileName = getFileName(value);
                    var normalizedPath = value.startsWith('/') ? value : '/' + value;
                    var pathSegments = normalizedPath.substring(1).split('/');
                    var encodedSegments = pathSegments.map(function(segment) {
                        return encodeURIComponent(segment);
                    });
                    var encodedPath = '/' + encodedSegments.join('/');
                    var fileUrl = '/api/diagnostics/files' + encodedPath;
                    var escapedFileUrl = fileUrl.replace(/'/g, "\\'");
                    var escapedFileName = fileName.replace(/'/g, "\\'").replace(/"/g, "&quot;");
                    var fileType = (key === 'photo_path' || key === 'photo_result_path') ? 'photo' : 'document';
                    value = '<a href="javascript:void(0);" onclick="openFileModal(\'' + escapedFileUrl + '\', \'' + escapedFileName + '\', \'' + fileType + '\')" style="color: #007bff; text-decoration: underline; cursor: pointer;">' + fileName + '</a>';
                } else {
                    value = '';
                }
            }
            // Форматируем даты в подробной информации
            else if (key.includes('date') || key === 'created_at' || key === 'updated_at') {
                if (key.includes('_date') || key === 'detection_date' || key === 'elimination_date') {
                    value = formatDate(value);
                } else {
                    value = formatDateTime(value);
                }
            }
            
            var displayKey = keyTranslations[key] || key;
            rows += '<tr><th>'+ displayKey +'</th><td>'+ value +'</td></tr>';
        }
    });
    
    // Затем обрабатываем остальные поля, которых нет в списке
    Object.entries(d || {}).forEach(function(entry) {
        var key = entry[0];
        if (fieldOrder.indexOf(key) === -1) {
            var value = entry[1];
            if (value === null || value === undefined) { value = ''; }
            
            // Форматируем даты
            if (key.includes('date') || key === 'created_at' || key === 'updated_at') {
                if (key.includes('_date') || key === 'detection_date' || key === 'elimination_date') {
                    value = formatDate(value);
                } else {
                    value = formatDateTime(value);
                }
            }
            
            var displayKey = keyTranslations[key] || key;
            rows += '<tr><th>'+ displayKey +'</th><td>'+ value +'</td></tr>';
        }
    });
    
    // Добавляем кнопки управления внизу таблицы
    var actionButtons = '<div style="margin-top: 20px; text-align: left; padding: 15px; border-top: 2px solid #ddd;">' +
        '<button onclick="openUpdateModal(' + d.id + ')" class="btn-apply" style="padding: 10px 20px; margin-right: 10px; cursor: pointer;">Обновить</button>' +
        '<button onclick="deleteReport(' + d.id + ')" class="btn-cancel" style="padding: 10px 20px; cursor: pointer; background-color: #dc3545; color: white; border: none;">Удалить</button>' +
        '</div>';
    
    return '<table class="details-table">' + rows + '</table>' + actionButtons;
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

    var table = $('#diagnosticsTable').DataTable({
        ajax: { 
            url: '/dashboard/diagnostics-reports', 
            dataSrc: function(json) {
                // Фильтруем по текущему году по умолчанию
                const currentYear = new Date().getFullYear();
                return json.filter(row => {
                    const dt = new Date(row.detection_date || row.created_at);
                    return !isNaN(dt.getTime()) && dt.getFullYear() === currentYear;
                });
            }
        },
        autoWidth: true,
        columns: [
            { className: 'details-control', orderable: false, data: null, defaultContent: '' },
            { data: 'id', title: 'ID' },
            { data: 'detection_date', title: 'ДАТА ОБНАРУЖЕНИЯ', render: function(d){ return formatDate(d); } },
            { data: 'diagnostics_type', title: 'ТИП ДИАГНОСТИКИ' },
            { data: 'equipment', title: 'ОБОРУДОВАНИЕ' },
            { data: 'node', title: 'УЗЕЛ' },
            { data: 'area', title: 'УЧАСТОК' },
            { data: 'malfunction', title: 'НЕИСПРАВНОСТЬ' },
            { data: 'additional_kit', title: 'ДОП. КОМПЛЕКТ' },
            { data: 'causes', title: 'ПРИЧИНЫ' },
            { data: 'photo_path', title: 'ФОТО', 
              render: function(data, type, row) {
                  return renderFileThumbnail(data, 'photo');
              }
            },
            { data: 'document_path', title: 'ДОКУМЕНТ',
              render: function(data, type, row) {
                  return renderFileThumbnail(data, 'document');
              }
            },
            { data: 'report', title: 'ОТЧЕТ',
              render: function(data, type, row) {
                  if (type === 'display') {
                      return '<button class="btn-print" onclick="generateReport(' + row.id + ')" style="padding: 5px 10px; cursor: pointer; background-color: #007bff; color: white; border: none; border-radius: 4px; font-size: 12px;">Скачать</button>';
                  }
                  return data;
              }
            },
            { data: 'status', title: 'СТАТУС', 
              render: function(data, type, row) {
                  if (!data) return '';
                  let color = '';
                  let bgColor = '';
                  if (data === 'ОТКРЫТО') {
                      color = '#fff';
                      bgColor = '#dc3545'; // красный
                  } else if (data === 'В РАБОТЕ') {
                      color = '#fff';
                      bgColor = '#6c757d'; // серый
                  } else if (data === 'ЗАКРЫТО') {
                      color = '#fff';
                      bgColor = '#28a745'; // зеленый
                  }
                  if (type === 'display' && color) {
                      return '<span onclick="openStatusModal(' + row.id + ', \'' + data + '\')" style="background-color: ' + bgColor + '; color: ' + color + 
                             '; padding: 4px 8px; border-radius: 4px; font-weight: bold; cursor: pointer;" title="Нажмите для изменения статуса">' + data + '</span>';
                  }
                  return data;
              }
            },
            { data: 'elimination_date', title: 'ДАТА УСТРАНЕНИЯ', render: function(d){ return formatDate(d); } },
            { data: 'condition_after_elimination', title: 'СОСТОЯНИЕ ПОСЛЕ УСТРАНЕНИЯ' },
            { data: 'responsible', title: 'ОТВЕТСТВЕННЫЙ' },
            { data: 'non_elimination_reason', title: 'ПРИЧИНА НЕУСТРАНЕНИЯ' },
            { data: 'measures', title: 'МЕРОПРИЯТИЯ' },
            { data: 'comments', title: 'КОММЕНТАРИИ' },
            { data: 'photo_result_path', title: 'ФОТО ИТОГ',
              render: function(data, type, row) {
                  return renderFileThumbnail(data, 'photo');
              }
            },
            { data: 'document_result_path', title: 'ОТЧЕТ ИТОГ',
              render: function(data, type, row) {
                  if (type === 'display') {
                      // Если есть файл, показываем миниатюру, иначе кнопку для генерации отчета
                      if (data) {
                          return renderFileThumbnail(data, 'document');
                      } else {
                          return '<button class="btn-print" onclick="generateFullReportFromTable(' + row.id + ')" style="padding: 5px 10px; cursor: pointer; background-color: #28a745; color: white; border: none; border-radius: 4px; font-size: 12px;">Отчет</button>';
                      }
                  }
                  return data;
              }
            },
            { data: 'created_at', title: 'Создано', render: function(d){ return formatDateTime(d); } },
            { data: 'updated_at', title: 'Обновлено', render: function(d){ return formatDateTime(d); } }
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
                targets: [7, 9, 10, 12, 15, 16, 17],
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

    // Заполнение фильтров после загрузки данных (нужен namespace .dt и сырой json — не полагаться на ajax.json())
    // json — ответ сервера до dataSrc; table.ajax.json() после функции dataSrc может быть не массивом исходных строк
    $('#diagnosticsTable').on('xhr.dt', function (_e, _settings, json) {
        allData = Array.isArray(json) ? json : [];
        populateFilters();
        // Устанавливаем текущий год по умолчанию
        $('#yearSelect').val(new Date().getFullYear());
    });

    function populateFilters() {
        const data = allData;
        const diagnosticsTypes = Array.from(new Set(data.map(function (r) { return rowDiagnosticsType(r); }).filter(function (v) { return v.trim() !== ''; }))).sort(function (a, b) { return String(a).localeCompare(String(b), 'ru'); });
        const equipment = Array.from(new Set(data.map(r => r.equipment).filter(Boolean))).sort();
        const areas = Array.from(new Set(data.map(r => r.area).filter(Boolean))).sort();
        const years = Array.from(new Set(data.map(r => { 
            const d = new Date(r.detection_date || r.created_at); 
            return isNaN(new Date(d).getTime())? null : new Date(d).getFullYear(); 
        }).filter(Boolean))).sort((a,b)=>a-b);
        $('#diagnosticsTypeSelect').html('<option value="all">Все</option>' + diagnosticsTypes.map(t => `<option value="${t}">${t}</option>`).join(''));
        $('#equipmentSelect').html('<option value="all">Все</option>' + equipment.map(e=>`<option value="${e}">${e}</option>`).join(''));
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
        const diagnosticsType = $('#diagnosticsTypeSelect').val();
        const equipment = $('#equipmentSelect').val();
        const area = $('#areaSelect').val();

        let filteredData = allData;

        if (year !== 'all') {
            filteredData = filteredData.filter(row => {
                const dt = new Date(row.detection_date || row.created_at);
                return !isNaN(dt.getTime()) && dt.getFullYear() === Number(year);
            });
        }

        if (month !== 'all') {
            filteredData = filteredData.filter(row => {
                const dt = new Date(row.detection_date || row.created_at);
                return !isNaN(dt.getTime()) && (dt.getMonth() + 1) === Number(month);
            });
        }

        if (week !== 'all') {
            filteredData = filteredData.filter(row => {
                const dt = new Date(row.detection_date || row.created_at);
                return !isNaN(dt.getTime()) && getWeekNumber(dt) === Number(week);
            });
        }

        if (diagnosticsType !== 'all') {
            filteredData = filteredData.filter(function (row) { return rowDiagnosticsType(row) === diagnosticsType; });
        }

        if (equipment !== 'all') {
            filteredData = filteredData.filter(row => row.equipment === equipment);
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
            const d = new Date(r.detection_date || r.created_at);
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

    $('#diagnosticsTable tbody').on('click', 'td.details-control', function () {
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

