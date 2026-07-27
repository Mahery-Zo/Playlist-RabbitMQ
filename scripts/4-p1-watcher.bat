@echo off
REM P1 - surveille repertoire\ et publie les nouveaux mp3.
cd /d "%~dp0.."
title P1 Watcher
java -jar watcher\target\watcher.jar
pause
