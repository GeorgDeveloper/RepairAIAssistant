function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value.trim();
    
    if (!message) return;
    
    addMessage('user', message);
    input.value = '';
    
    fetch('/api/chat', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(message)
    })
    .then(response => response.text())
    .then(data => {
        addMessage('assistant', data);
        showFeedbackButtons(message, data);
    })
    .catch(error => {
        addMessage('assistant', 'Ошибка: ' + error.message);
    });
}

function addMessage(sender, text) {
    const messages = document.getElementById('messages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}`;
    messageDiv.textContent = `${sender === 'user' ? 'Вы' : 'Ассистент'}: ${text}`;
    messages.appendChild(messageDiv);
    messages.scrollTop = messages.scrollHeight;
}

function showFeedbackButtons(request, response) {
    const feedbackDiv = document.getElementById('feedback-buttons');
    feedbackDiv.style.display = 'block';
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
    const request = feedbackDiv.dataset.request;
    const response = feedbackDiv.dataset.response;
    if (type === 'correct') {
        fetch('/api/chat/feedback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ request, response })
        }).then(() => {
            addMessage('system', 'Спасибо за подтверждение! Пара добавлена для обучения.');
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
    fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(message)
    })
    .then(response => response.text())
    .then(data => {
        addMessage('assistant', data);
        showFeedbackButtons(message, data);
    })
    .catch(error => {
        addMessage('assistant', 'Ошибка: ' + error.message);
    });
}