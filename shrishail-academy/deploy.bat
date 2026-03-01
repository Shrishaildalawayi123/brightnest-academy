@echo off
setlocal EnableExtensions EnableDelayedExpansion
:: ============================================================
:: BrightNest Academy - Deploy Script
:: Builds (optional) and pushes to GitHub
:: ============================================================
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%" >nul

for /f "delims=" %%G in ('git rev-parse --show-toplevel 2^>nul') do set "REPO_ROOT=%%G"
if not defined REPO_ROOT (
    echo ERROR: Not inside a git repository.
    popd >nul
    pause
    exit /b 1
)

echo.
echo ====================================================
echo  BrightNest Academy - Deploy
echo ====================================================
echo.

:: Step 1: Optional build (avoids committing anything under /target)
echo [1/4] Building project (optional)...
where mvn >nul 2>nul
if errorlevel 1 (
    echo       Maven not found. Skipping build.
) else (
    call mvn -q -DskipTests package
    if errorlevel 1 (
        echo ERROR: Maven build failed!
        popd >nul
        pause
        exit /b 1
    )
    echo       Done. Build succeeded.
)
echo.

:: Step 2: Git add all changes
echo [2/4] Staging all changes...
pushd "%REPO_ROOT%" >nul
git add -A
if errorlevel 1 (
    echo ERROR: git add failed!
    popd >nul
    popd >nul
    pause
    exit /b 1
)
popd >nul
echo       Done.
echo.

:: Step 3: Git commit with timestamp
set TIMESTAMP=%date:~6,4%-%date:~3,2%-%date:~0,2% %time:~0,8%
echo [3/4] Committing changes...
pushd "%REPO_ROOT%" >nul
git commit -m "Deploy: %TIMESTAMP%"
if errorlevel 1 (
    echo       No changes to commit ^(or commit failed^). Continuing...
)
popd >nul
echo.

:: Step 4: Push to GitHub
echo [4/4] Pushing to GitHub (origin/main)...
pushd "%REPO_ROOT%" >nul
git push origin main
if errorlevel 1 (
    echo ERROR: git push failed! Check your credentials and network.
    popd >nul
    popd >nul
    pause
    exit /b 1
)
popd >nul
echo.
echo ====================================================
echo  SUCCESS! Deployed to GitHub at %TIMESTAMP%
echo  View at: https://github.com/Shrishaildalawayi123/brightnest-academy
echo ====================================================
echo.
pause
popd >nul
endlocal
