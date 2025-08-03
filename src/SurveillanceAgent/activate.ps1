# GemmaGuardian Virtual Environment Activation
Write-Host "GemmaGuardian Virtual Environment Activation" -ForegroundColor Cyan
& "venv\Scripts\Activate.ps1"
Write-Host "Virtual environment activated!" -ForegroundColor Green
Write-Host ""
Write-Host "You can now run:" -ForegroundColor Yellow
Write-Host "  python main.py --mode ollama" -ForegroundColor White
Write-Host "  python main.py --mode transformer --preview" -ForegroundColor White
Write-Host "  python start_full_system.py --mode transformer" -ForegroundColor White
Write-Host ""
