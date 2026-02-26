@echo off
echo ====================================
echo   LIB Education Website Launcher
echo ====================================
echo.
echo Opening website in your default browser...
echo.

start "" "index.html"

echo.
echo Website opened successfully!
echo.
echo To start a local development server instead:
echo   1. Install Python (if not already installed)
echo   2. Run: python -m http.server 8000
echo   3. Open: http://localhost:8000
echo.
echo Press any key to exit...
pause >nul
