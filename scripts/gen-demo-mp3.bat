@echo off
REM Genere un mp3 de demo (avec tags) dans repertoire\ pour tester le pipeline.
cd /d "%~dp0.."
java -cp "metadata\target\test-classes;metadata\target\metadata.jar" mg.itu.naina.metadata.SampleMp3 repertoire\demo.mp3
echo Genere : repertoire\demo.mp3
pause
