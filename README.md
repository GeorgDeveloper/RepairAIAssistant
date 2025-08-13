# Repair AI Assistant

## Запуск приложения

### Windows
```bash
start.bat
```

### Linux/Mac
```bash
chmod +x start.sh
./start.sh
```

## Остановка приложения

### Windows
```bash
stop.bat
```

### Linux/Mac
```bash
./stop.sh
```

## Предварительные требования

1. **Java 17+** - установлен и доступен в PATH
2. **Maven** - установлен и доступен в PATH
3. **Ollama** - запущен на localhost:11434
4. **MySQL** - база данных repair_assistant
5. **Переменная окружения** TELEGRAM_BOT_TOKEN

## Порядок запуска

1. assistant-core (порт 8080) - основной сервис
2. assistant-web (порт 8081) - веб-интерфейс
3. assistant-telegram (порт 8082) - телеграм бот

## Доступ к сервисам

- Веб-интерфейс: http://localhost:8081
- API: http://localhost:8080
- Телеграм бот: автоматически подключается

## Логи

Логи сохраняются в директории `logs/` (только для Linux/Mac)