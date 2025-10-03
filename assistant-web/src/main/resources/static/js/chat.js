const link = document.createElement('link');
link.rel = 'stylesheet';
link.href = '/static/css/chat.css';
document.head.appendChild(link);

function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value.trim();
    
    if (!message) return;
    
    addMessage('user', message);
    input.value = '';
    
    // Показываем индикатор загрузки
    const loadingMessage = addMessage('assistant', '🤔 Обрабатываю ваш запрос...');
    
    // Отправляем в правильном JSON формате как в Telegram
    fetch('/api/chat', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        body: JSON.stringify(message)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        return response.text();
    })
    .then(data => {
        // Удаляем индикатор загрузки
        loadingMessage.remove();
        addMessage('assistant', data);
        showFeedbackButtons(message, data);
    })
    .catch(error => {
        // Удаляем индикатор загрузки
        loadingMessage.remove();
        console.error('Ошибка запроса:', error);
        addMessage('assistant', '❌ Ошибка соединения с сервером: ' + error.message + '. Попробуйте позже.');
    });
}

function addMessage(sender, text) {
    const messages = document.getElementById('messages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}`;
    messageDiv.textContent = `${sender === 'user' ? 'Вы' : 'Ассистент'}: ${text}`;

    // Apply specific styles for system messages
    if (sender === 'system') {
        messageDiv.style.fontWeight = 'bold';
        messageDiv.style.backgroundColor = '#90ee90'; // Light green background
        messageDiv.style.padding = '10px';
        messageDiv.style.borderRadius = '5px';
    }
    
    // Стили для сообщений загрузки
    if (text.includes('Обрабатываю') || text.includes('Генерирую')) {
        messageDiv.style.fontStyle = 'italic';
        messageDiv.style.color = '#666';
        messageDiv.style.backgroundColor = '#f0f0f0';
        messageDiv.style.padding = '8px';
        messageDiv.style.borderRadius = '4px';
        messageDiv.classList.add('loading-message');
    }

    messages.appendChild(messageDiv);
    messages.scrollTop = messages.scrollHeight;
    
    // Возвращаем элемент для возможности удаления
    return messageDiv;
}

function showFeedbackButtons(request, response) {
    const feedbackDiv = document.getElementById('feedback-buttons');
    feedbackDiv.style.display = 'block';
    feedbackDiv.style.alignItems = 'center';
    feedbackDiv.dataset.request = request;
    feedbackDiv.dataset.response = response;

}

function hideFeedbackButtons() {
    const feedbackDiv = document.getElementById('feedback-buttons');
    feedbackDiv.style.display = 'none';
    feedbackDiv.dataset.request = '';
    feedbackDiv.dataset.response = '';
}

function sendFeedback(type) {
    const feedbackDiv = document.getElementById('feedback-buttons');
    feedbackDiv.style.fontWeight = 'bold';
    feedbackDiv.style.color = 'green';
    
    const request = feedbackDiv.dataset.request;
    const response = feedbackDiv.dataset.response;
    if (type === 'correct') {
        fetch('/api/chat/feedback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ request, response })
        }).then(() => {
            addMessage('system', '✅ Спасибо за подтверждение! Ваш вопрос и ответ сохранены в векторную базу данных для улучшения будущих ответов.');
            hideFeedbackButtons();
        }).catch(() => {
            addMessage('system', '✅ Обратная связь принята. Данные сохранены для обучения системы.');
            hideFeedbackButtons();
        });
    } else if (type === 'regenerate') {
        sendMessageAgain(request);
        hideFeedbackButtons();
    } else if (type === 'newdialog') {
        document.getElementById('messages').innerHTML = '';
        hideFeedbackButtons();
    }
}

function sendMessageAgain(message) {
    addMessage('user', message);
    
    // Показываем индикатор загрузки
    const loadingMessage = addMessage('assistant', '🔄 Генерирую новый ответ...');
    
    fetch('/api/chat', {
        method: 'POST',
        headers: { 
            'Content-Type': 'application/json;charset=UTF-8'
        },
        body: JSON.stringify(message)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        return response.text();
    })
    .then(data => {
        // Удаляем индикатор загрузки
        loadingMessage.remove();
        addMessage('assistant', data);
        showFeedbackButtons(message, data);
    })
    .catch(error => {
        // Удаляем индикатор загрузки
        loadingMessage.remove();
        console.error('Ошибка повторного запроса:', error);
        addMessage('assistant', '❌ Ошибка при повторной генерации: ' + error.message);
    });
}

// Add event listener for Enter key
const messageInput = document.getElementById('messageInput');
if (messageInput) {
    messageInput.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            sendMessage();
        }
    });
}