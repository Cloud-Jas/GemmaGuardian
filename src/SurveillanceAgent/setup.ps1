# GemmaGuardian Surveillance System Setup Script
# PowerShell version for Windows

Write-Host "🔒 GemmaGuardian Surveillance System Setup" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check if Python is available
try {
    $pythonVersion = py --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Python found: $pythonVersion" -ForegroundColor Green
    } else {
        throw "Python not found"
    }
} catch {
    Write-Host "❌ Python not found. Please install Python 3.8+ first." -ForegroundColor Red
    Write-Host "   Download from: https://www.python.org/downloads/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""

# Check if we're in the right directory
if (!(Test-Path "main.py")) {
    Write-Host "❌ Please run this script from the SurveillanceAgent directory" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Run the setup menu
Write-Host "🚀 Starting setup menu..." -ForegroundColor Green
try {
    py scripts\setup_menu.py
    
    Write-Host ""
    Write-Host "Setup completed! You can now run:" -ForegroundColor Green
    Write-Host "  py main.py --mode ollama" -ForegroundColor Yellow
    Write-Host "  py main.py --mode transformer" -ForegroundColor Yellow
    Write-Host ""
    
} catch {
    Write-Host "❌ Setup failed: $_" -ForegroundColor Red
}

Read-Host "Press Enter to exit"
