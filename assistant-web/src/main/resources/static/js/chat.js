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