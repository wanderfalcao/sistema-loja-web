@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul

echo.
echo ╔══════════════════════════════════════════════════════╗
echo ║           SISTEMA LOJA WEB — Inicialização           ║
echo ╚══════════════════════════════════════════════════════╝
echo.

REM ─────────────────────────────────────────────────────────
REM Verifica se Docker está rodando
REM ─────────────────────────────────────────────────────────
docker info > nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ✗ Docker não está rodando. Inicie o Docker Desktop e tente novamente.
    pause
    exit /b 1
)

REM ─────────────────────────────────────────────────────────
REM Sobe a stack completa
REM ─────────────────────────────────────────────────────────
echo Construindo imagens e subindo todos os serviços...
echo (na primeira execução pode levar alguns minutos)
echo.
docker-compose up --build -d
if %ERRORLEVEL% neq 0 (
    echo.
    echo ✗ Falha ao iniciar os containers. Verifique os logs:
    echo   docker-compose logs
    pause
    exit /b 1
)
echo.

REM ─────────────────────────────────────────────────────────
REM Aguarda o api-gateway ficar saudável
REM ─────────────────────────────────────────────────────────
echo Aguardando serviços ficarem disponíveis...
echo.

set MAX=30
set N=0

:aguarda
set /a N+=1
if %N% gtr %MAX% (
    echo.
    echo ✗ Timeout. Verifique o status com: docker-compose ps
    echo   Logs: docker-compose logs api-gateway
    pause
    exit /b 1
)

docker inspect --format "{{.State.Health.Status}}" api-gateway 2>nul | findstr /i "healthy" > nul
if %ERRORLEVEL% neq 0 (
    echo   [%N%/%MAX%] Aguardando api-gateway...
    timeout /t 10 /nobreak > nul
    goto aguarda
)

REM ─────────────────────────────────────────────────────────
REM Exibe endereços e abre o Swagger
REM ─────────────────────────────────────────────────────────
echo.
echo ╔══════════════════════════════════════════════════════╗
echo ║                  SISTEMA NO AR                       ║
echo ╠══════════════════════════════════════════════════════╣
echo ║  Pedidos    http://localhost:8080/pedidos            ║
echo ║  Produtos   http://localhost:8080/produtos           ║
echo ║  Swagger    http://localhost:8080/swagger-ui.html    ║
echo ║  Eureka     http://localhost:8761                    ║
echo ║  SonarQube  http://localhost:9000  (admin/admin)     ║
echo ║  PgAdmin    http://localhost:5050                    ║
echo ╠══════════════════════════════════════════════════════╣
echo ║  Para parar:  docker-compose stop                    ║
echo ║  Para limpar: docker-compose down -v                 ║
echo ╚══════════════════════════════════════════════════════╝
echo.

start http://localhost:8080/swagger-ui.html

pause
