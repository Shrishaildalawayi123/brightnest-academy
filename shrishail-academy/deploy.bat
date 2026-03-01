@echo off
:: ============================================================
:: BrightNest Academy - Deploy Script
:: Builds (optional) and pushes to GitHub
:: ============================================================
cd /d "%~dp0"

echo.
echo ====================================================
echo  BrightNest Academy - Deploy
echo ====================================================
echo.

:: Step 1: Optional build (avoids committing anything under /target)
echo [1/4] Building project (optional)...
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo       Maven not found. Skipping build.
) else (
    call mvn -q -DskipTests package
    if %errorlevel% neq 0 (
        echo ERROR: Maven build failed!
        pause
        exit /b 1
    )
    echo       Done. Build succeeded.
)
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
