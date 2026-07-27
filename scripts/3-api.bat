@echo off
REM Demarre l'API Spring Boot sur http://localhost:8090
cd /d "%~dp0.."
title API (8090)
echo === API sur http://localhost:8090 ===
java -jar api\target\api.jar
pause
