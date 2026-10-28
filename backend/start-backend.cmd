@echo off
setlocal EnableExtensions

cd /d "%~dp0"
set "ENV_FILE=%~dp0.env.local"

if not exist "%ENV_FILE%" (
    echo No .env.local found.
    echo Copy .env.example to .env.local and fill in the LLM values for AI mode.
    echo Starting with the application defaults and rule strategies.
) else (
    echo Loading environment from "%ENV_FILE%"
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
        if not "%%A"=="" set "%%A=%%B"
    )
)

echo.
echo Starting StockPulse backend on port %SERVER_PORT%...
echo Stop the server with Ctrl+C.
echo.

call mvn -B -ntp spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"
echo.
echo Backend stopped with exit code %EXIT_CODE%.
exit /b %EXIT_CODE%