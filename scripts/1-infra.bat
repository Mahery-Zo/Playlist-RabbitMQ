@echo off
REM Demarre RabbitMQ + MySQL via Docker (necessite Docker Desktop lance).
cd /d "%~dp0.."
echo === Demarrage de l'infra (RabbitMQ + MySQL) ===
docker compose up -d
echo.
echo UI RabbitMQ : http://localhost:15672  (guest / guest)
echo MySQL       : localhost:3306  (naina / naina)
pause
