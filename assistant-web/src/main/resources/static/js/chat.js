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
    
    fetch('http://localhost:8080/api/analyze', {
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
    feedbackDiv.style.alignItems = 'center';
    feedbackDiv.dataset.request = request;
    feedbackDiv.dataset.response = response;

    // Apply bold and green styling to assistant feedback
    feedbackDiv.style.fontWeight = 'bold';
    feedbackDiv.style.color = 'green';
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
        fetch('http://localhost:8080/api/feedback', {
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