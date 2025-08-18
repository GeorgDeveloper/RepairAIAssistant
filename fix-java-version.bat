@echo off
echo Исправление проблем с версией Java...

echo Очистка проекта...
call mvn clean

echo Обновление зависимостей...
call mvn dependency:resolve

echo Компиляция с Java 17...
call mvn compile -Dmaven.compiler.source=17 -Dmaven.compiler.target=17

echo Готово! Проблемы с версией Java исправлены.
pause