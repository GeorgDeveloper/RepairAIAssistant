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

    // Apply specific styles for system messages
    if (sender === 'system') {
        messageDiv.style.fontWeight = 'bold';
        messageDiv.style.backgroundColor = '#90ee90'; // Light green background
        messageDiv.style.padding = '10px';
        messageDiv.style.borderRadius = '5px';
    }

    messages.appendChild(messageDiv);
    messages.scrollTop = messages.scrollHeight;
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
            addMessage('system', 'Спасибо за подтверждение! Пара добавлена для обучения. Можете задать новый вопрос.');
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

// Add event listener for Enter key
const messageInput = document.getElementById('messageInput');
if (messageInput) {
    messageInput.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            sendMessage();
        }
    });
}