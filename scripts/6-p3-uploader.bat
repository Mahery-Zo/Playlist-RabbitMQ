@echo off
REM P3 - envoie fichier+metadonnees a l'API puis supprime du repertoire.
cd /d "%~dp0.."
title P3 Uploader
java -jar uploader\target\uploader.jar
pause
