@echo off
setlocal EnableExtensions

set "JAVA_HOME=C:\Users\Dell\.jdk\jdk-21.0.1+12"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JDK 21 was not found at "%JAVA_HOME%".
    echo Update run-java21.bat with the correct JDK 21 path for this machine.
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

if "%~1"=="" (
    echo Usage: run-java21.bat ^<command^> [args...]
    echo Example: run-java21.bat java -version
    echo Example: run-java21.bat javac -version
    echo Example: run-java21.bat mvn clean verify
    echo Example: run-java21.bat mvn spring-boot:run -Dspring-boot.run.profiles=test
    exit /b 1
)

call %*
exit /b %errorlevel%