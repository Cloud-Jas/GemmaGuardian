@echo off
echo GemmaGuardian Virtual Environment Activation
call "venv\Scripts\activate.bat"
echo Virtual environment activated!
echo.
echo You can now run:
echo   python main.py --mode ollama
echo   python main.py --mode transformer --preview
echo   python start_full_system.py --mode transformer
echo.
