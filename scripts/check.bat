@echo off
REM Affiche l'etat : chansons en base, contenu du repertoire, fichiers stockes, queues.
cd /d "%~dp0.."
echo === Chansons en base (GET /api/songs) ===
curl -s http://localhost:8090/api/songs
echo.
echo.
echo === Fichiers restants dans repertoire\ (doit se vider apres traitement) ===
dir /b repertoire 2>nul
echo.
echo === Fichiers stockes par l'API (storage\songs\) ===
dir /b storage\songs 2>nul
echo.
echo === Etat des queues RabbitMQ ===
docker exec naina-rabbitmq rabbitmqctl list_queues name messages
pause
