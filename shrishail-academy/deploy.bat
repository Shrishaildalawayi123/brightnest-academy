@echo off
:: ============================================================
:: BrightNest Academy - Deploy Script
:: Syncs static files to target and pushes to GitHub
:: ============================================================
cd /d "%~dp0"

echo.
echo ====================================================
echo  BrightNest Academy - Deploy
echo ====================================================
echo.

:: Step 1: Sync static files from src to target
echo [1/4] Syncing static files to target...
if not exist "target\classes\static" mkdir "target\classes\static"
xcopy /E /Y /Q "src\main\resources\static\*" "target\classes\static\"
if %errorlevel% neq 0 (
    echo ERROR: File sync failed!
    pause
    exit /b 1
)
echo       Done. Files synced.
echo.

:: Step 2: Git add all changes
echo [2/4] Staging all changes...
git add .
if %errorlevel% neq 0 (
    echo ERROR: git add failed!
    pause
    exit /b 1
)
echo       Done.
echo.

:: Step 3: Git commit with timestamp
set TIMESTAMP=%date:~6,4%-%date:~3,2%-%date:~0,2% %time:~0,8%
echo [3/4] Committing changes...
git commit -m "Deploy: %TIMESTAMP%"
if %errorlevel% neq 0 (
    echo       No changes to commit (or commit failed). Continuing...
)
echo.

:: Step 4: Push to GitHub
echo [4/4] Pushing to GitHub (origin/main)...
git push origin main
if %errorlevel% neq 0 (
    echo ERROR: git push failed! Check your credentials and network.
    pause
    exit /b 1
)
echo.
echo ====================================================
echo  SUCCESS! Deployed to GitHub at %TIMESTAMP%
echo  View at: https://github.com/Shrishaildalawayi123/brightnest-academy
echo ====================================================
echo.
pause
