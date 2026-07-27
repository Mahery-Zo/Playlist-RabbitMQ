@echo off
REM P2 - extrait les metadonnees des mp3.
cd /d "%~dp0.."
title P2 Metadata
java -jar metadata\target\metadata.jar
pause
