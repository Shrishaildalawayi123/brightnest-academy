@echo off
setlocal

REM WARNING:
REM This file contains local credentials and is ignored by git via .gitignore.
REM Do NOT commit this file to a public repository.

cd /d "%~dp0shrishail-academy"

REM Pin Java to JDK 21 for consistent builds
set "JAVA_HOME=C:\Users\Dell\.jdk\jdk-21.0.1+12"
set "Path=%JAVA_HOME%\bin;%Path%"

set "ADMIN_EMAIL=admin@brightnest.com"
set "ADMIN_PASSWORD=Admin@123"

REM Use default profile unless you set something else.
set "SPRING_PROFILES_ACTIVE="

REM Force a clean rebuild (Java 21 bytecode) then start the app
call mvn -DskipTests clean compile
if errorlevel 1 exit /b %errorlevel%
echo Starting Spring Boot...
call mvn -DskipTests spring-boot:run

endlocal
