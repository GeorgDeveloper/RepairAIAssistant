# 🗄️ Настройка внешнего доступа к MySQL

## 📋 Обзор

Скрипт установки автоматически настраивает MySQL для внешних подключений, включая доступ через DBeaver и другие клиенты баз данных.

## 🔧 Автоматические настройки

### 1. Пользователи MySQL

Скрипт создает три типа пользователей:

| Пользователь | Пароль | Права доступа | Назначение |
|-------------|--------|---------------|------------|
| `admin` | `AdminPass123!` | Полные права на все БД | Администрирование через DBeaver |
| `dba` | `dbaPass` | Полные права на `monitoring_bd` | Приложение |
| `readonly` | `ReadOnlyPass123!` | Только чтение `monitoring_bd` | Безопасный просмотр данных |

### 2. Настройки сервера

- **Bind Address:** `0.0.0.0` (принимает подключения извне)
- **Порт:** `3306` (стандартный порт MySQL)
- **Кодировка:** `utf8mb4` с `utf8mb4_unicode_ci`
- **Аутентификация:** `mysql_native_password`

### 3. Файрвол

- Порт `3306/tcp` открыт для внешних подключений
- UFW правило: `'MySQL external access'`

## 🔌 Подключение через DBeaver

### Настройки подключения:

1. **Тип соединения:** MySQL
2. **Хост:** `IP_АДРЕС_СЕРВЕРА`
3. **Порт:** `3306`
4. **База данных:** `monitoring_bd`
5. **Пользователь:** `admin`
6. **Пароль:** `AdminPass123!`

### Пошаговая инструкция:

1. **Откройте DBeaver**
2. **Создайте новое соединение:**
   - Нажмите "Новое соединение" или Ctrl+Shift+N
   - Выберите "MySQL"

3. **Заполните параметры:**
   ```
   Хост: 192.168.1.100  (замените на IP вашего сервера)
   Порт: 3306
   База данных: monitoring_bd
   Имя пользователя: admin
   Пароль: AdminPass123!
   ```

4. **Тестируйте соединение:**
   - Нажмите "Тест соединения"
   - Должно появиться сообщение об успешном подключении

5. **Сохраните и подключитесь**

## 🛡️ Безопасность

### Рекомендации по безопасности:

1. **Смените пароли по умолчанию:**
   ```sql
   ALTER USER 'admin'@'%' IDENTIFIED BY 'ВАШ_НОВЫЙ_ПАРОЛЬ';
   ALTER USER 'readonly'@'%' IDENTIFIED BY 'ВАШ_НОВЫЙ_ПАРОЛЬ';
   ```

2. **Ограничьте доступ по IP (опционально):**
   ```bash
   # Разрешить доступ только с определенного IP
   sudo ufw delete allow 3306/tcp
   sudo ufw allow from ВАША_IP to any port 3306
   ```

3. **Используйте SSL соединения:**
   - В DBeaver включите SSL в настройках соединения
   - Вкладка "SSL" → "Использовать SSL"

## 🔍 Проверка настроек

### 1. Проверка пользователей MySQL:

```bash
# Подключение к MySQL
docker exec -it repair_mysql mysql -u root -prootPass

# Просмотр пользователей
SELECT User, Host FROM mysql.user;

# Просмотр прав пользователя admin
SHOW GRANTS FOR 'admin'@'%';
```

### 2. Проверка настроек сервера:

```bash
# Проверка bind-address
docker exec repair_mysql mysql -u root -prootPass -e "SHOW VARIABLES LIKE 'bind_address';"

# Проверка порта
docker exec repair_mysql mysql -u root -prootPass -e "SHOW VARIABLES LIKE 'port';"
```

### 3. Проверка файрвола:

```bash
# Статус UFW
sudo ufw status

# Проверка открытых портов
netstat -tlnp | grep 3306
```

## 🧪 Тестирование подключения

### Из командной строки:

```bash
# С сервера (локально)
mysql -h localhost -u admin -pAdminPass123! monitoring_bd

# С удаленного компьютера
mysql -h IP_СЕРВЕРА -u admin -pAdminPass123! monitoring_bd
```

### Через telnet:

```bash
# Проверка доступности порта
telnet IP_СЕРВЕРА 3306
```

## 📊 Структура базы данных

После установки будут созданы таблицы:

- `equipment_maintenance_records` - Записи обслуживания оборудования
- `summary_of_solutions` - Сводка решений
- `breakdown_reports` - Отчеты о поломках  
- `manuals` - Руководства

### Тестовые данные:

Скрипт автоматически вставляет тестовые данные для проверки работы системы.

## 🚨 Устранение неполадок

### Проблема: Не удается подключиться извне

**Проверьте:**

1. **Файрвол сервера:**
   ```bash
   sudo ufw status
   # Должно быть: 3306/tcp ALLOW Anywhere
   ```

2. **Статус MySQL контейнера:**
   ```bash
   docker ps | grep mysql
   # Должен быть: Up
   ```

3. **Настройки bind-address:**
   ```bash
   docker exec repair_mysql mysql -u root -prootPass -e "SHOW VARIABLES LIKE 'bind_address';"
   # Должно быть: 0.0.0.0
   ```

4. **Права пользователя:**
   ```bash
   docker exec repair_mysql mysql -u root -prootPass -e "SELECT User, Host FROM mysql.user WHERE User='admin';"
   # Должно быть: admin | %
   ```

### Проблема: Ошибка аутентификации

**Решение:**
```sql
# Сброс пароля пользователя
ALTER USER 'admin'@'%' IDENTIFIED WITH mysql_native_password BY 'AdminPass123!';
FLUSH PRIVILEGES;
```

### Проблема: Доступ запрещен к базе данных

**Решение:**
```sql
# Предоставление прав заново
GRANT ALL PRIVILEGES ON monitoring_bd.* TO 'admin'@'%';
FLUSH PRIVILEGES;
```

## 📝 Дополнительные команды

### Создание нового пользователя:

```sql
CREATE USER 'newuser'@'%' IDENTIFIED BY 'password';
GRANT SELECT, INSERT, UPDATE, DELETE ON monitoring_bd.* TO 'newuser'@'%';
FLUSH PRIVILEGES;
```

### Резервное копирование:

```bash
# Создание дампа базы данных
docker exec repair_mysql mysqldump -u admin -pAdminPass123! monitoring_bd > backup.sql

# Восстановление из дампа
docker exec -i repair_mysql mysql -u admin -pAdminPass123! monitoring_bd < backup.sql
```

### Мониторинг подключений:

```sql
-- Просмотр активных подключений
SHOW PROCESSLIST;

-- Статистика подключений
SHOW STATUS LIKE 'Connections';
SHOW STATUS LIKE 'Threads_connected';
```

## ✅ Готовность к работе

После выполнения скрипта установки MySQL будет:

- ✅ Доступен для внешних подключений
- ✅ Настроен с правильными пользователями
- ✅ Защищен файрволом
- ✅ Готов для работы с DBeaver
- ✅ Содержит тестовые данные

**MySQL готов для подключения через DBeaver!** 🎉

---

**Важно:** Не забудьте сменить пароли по умолчанию в продакшн среде!
