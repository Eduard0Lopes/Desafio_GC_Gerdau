@echo off
rem ============================================================================
rem  Desafio GC - Gerdau: sobe TUDO com Docker (mongo + backend + frontend)
rem
rem  Sem Docker instalado? Use o dev.bat (modo nativo) - veja docs\COMO-RODAR.md.
rem  Pode ser executado de QUALQUER lugar (duplo clique ou linha de comando):
rem  o script sempre volta para a raiz do repositorio antes de rodar o compose.
rem ============================================================================
cd /d "%~dp0"

rem 1) o Docker precisa estar instalado
where docker >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERRO] O Docker nao esta instalado nesta maquina.
    echo        Sem Docker, use o dev.bat ^(modo nativo^) - veja docs\COMO-RODAR.md.
    echo.
    pause
    exit /b 1
)

rem 2) o motor do Docker precisa estar no ar
docker info >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERRO] O Docker esta instalado, mas o motor nao esta rodando.
    echo        Abra o Docker Desktop, espere o icone da baleia ficar estavel
    echo        e execute o start.bat de novo. Sem Docker? Use o dev.bat.
    echo.
    pause
    exit /b 1
)

rem 3) portas que os processos nativos costumam ocupar (backend java = 8080, npm = 80)
netstat -ano | findstr /C:":8080 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [AVISO] A porta 8080 ja esta em uso - provavelmente o backend nativo ^(java^) esta no ar.
    echo          Pare-o antes de continuar, senao o backend do Docker nao sobe.
    echo.
)
netstat -ano | findstr /C:":80 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [AVISO] A porta 80 ja esta em uso. Pare o servico que a ocupa antes de continuar.
    echo.
)

rem 3) sobe tudo a partir da raiz do repositorio (onde esta o docker-compose.yml)
docker compose version >nul 2>&1
if errorlevel 1 (
    docker-compose up --build
) else (
    docker compose up --build
)
