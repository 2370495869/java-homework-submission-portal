@echo off
chcp 65001 >nul
cd /d "%~dp0"
call mvnw.cmd -B -ntp -DskipTests package
if errorlevel 1 exit /b %errorlevel%
java -jar "target\homework-submission-portal.jar" %*
