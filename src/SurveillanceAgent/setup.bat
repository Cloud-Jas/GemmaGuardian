@echo off
echo 🔒 GemmaGuardian Surveillance System Setup
echo =====================================
echo.

REM Check if Python is available
py --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Python not found. Please install Python 3.8+ first.
    echo    Download from: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo ✅ Python found
echo.

REM Run the setup menu
echo 🚀 Starting setup menu...
py scripts\setup_menu.py

echo.
echo Setup completed. You can now run:
echo   py main.py --mode ollama
echo   py main.py --mode transformer
echo.
pause
