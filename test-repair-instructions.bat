@echo off
echo Testing Repair Instructions with Database Context...

echo.
echo 1. Testing general query...
curl -s -X POST -H "Content-Type: application/json" -d "Кто ты?" http://localhost:8080/api/analyze

echo.
echo.
echo 2. Testing repair instruction with database context...
curl -s -X POST -H "Content-Type: application/json" -d "Утечка азота. Что делать?" http://localhost:8080/api/analyze

echo.
echo.
echo 3. Testing statistics query...
curl -s -X POST -H "Content-Type: application/json" -d "Найди ремонты со статусом закрыто" http://localhost:8080/api/analyze

echo.
echo Test completed!
pause