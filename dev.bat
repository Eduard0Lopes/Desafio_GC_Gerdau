@echo off
rem ============================================================================
rem  Desafio GC - Gerdau: sobe SEM DOCKER (backend Java + frontend Node)
rem
rem  Use este script em maquinas que NAO tem o Docker Desktop instalado.
rem  Pode ser executado de QUALQUER lugar (duplo clique ou linha de comando):
rem  ele sempre volta para a raiz do repositorio antes de rodar.
rem ============================================================================
cd /d "%~dp0"

rem 1) pre-requisitos instalados na maquina
where java >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERRO] Java nao encontrado nesta maquina ^(e preciso Java 17 ou superior^).
    echo        Instale em https://adoptium.net e rode o dev.bat de novo.
    echo.
    pause
    exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERRO] Node.js nao encontrado nesta maquina ^(e preciso Node 20 ou superior^).
    echo        Instale em https://nodejs.org e rode o dev.bat de novo.
    echo.
    pause
    exit /b 1
)

rem 2) aviso se a porta do backend ja estiver ocupada
netstat -ano | findstr /C:":8080 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [AVISO] A porta 8080 ja esta em uso - talvez o backend ja esteja rodando.
    echo.
)

rem 3) dependencias do frontend (instala so na primeira vez)
if not exist "JAVA\frontend\node_modules" (
    echo Instalando as dependencias do frontend ^(primeira vez, pode demorar^)...
    pushd "JAVA\frontend"
    call npm install --no-audit --no-fund
    if errorlevel 1 (
        popd
        echo [ERRO] Falha ao instalar as dependencias do frontend.
        pause
        exit /b 1
    )
    popd
)

rem 4) sobe backend e frontend em janelas separadas
echo.
echo Iniciando:
echo   Backend  ^> http://localhost:8080
echo   Frontend ^> http://localhost:5173  ^(abra esta no navegador^)
echo.
start "Backend - http://localhost:8080" /D "%~dp0JAVA\backend" cmd /k mvnw.cmd spring-boot:run
start "Frontend - http://localhost:5173" /D "%~dp0JAVA\frontend" cmd /k npm run dev
echo.
echo Pronto! Feche as janelas "Backend" e "Frontend" para parar a aplicacao.
pause
