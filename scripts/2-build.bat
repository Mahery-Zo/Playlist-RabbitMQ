@echo off
REM Compile et package tous les modules (jars executables).
cd /d "%~dp0.."
echo === Build de tous les modules ===
call mvn clean install -DskipTests
echo.
echo Termine. Jars dans chaque module sous target\.
pause
