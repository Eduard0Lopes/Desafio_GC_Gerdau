@echo off
REM Abre o aplicativo desktop. Requer JDK 21+ e Maven. A API precisa estar no ar (docker compose up).
cd /d "%~dp0"
if "%API_BASE_URL%"=="" set API_BASE_URL=http://localhost:8080
mvn -q javafx:run
