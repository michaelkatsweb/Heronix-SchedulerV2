@echo off
title Heronix SchedulerV2 - JDK 21 Development Mode
echo ============================================
echo    Heronix SchedulerV2 - JDK 21 Dev Mode
echo    AI-Powered Scheduling Engine
echo ============================================
echo.

:: Navigate to project directory
cd /d "%~dp0"

:: Set JDK 21
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Verify JDK
echo Using Java:
java -version 2>&1
echo.

:: Clean compile
echo Cleaning and compiling project...
echo.

mvn clean compile -q
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ============================================
    echo    COMPILATION FAILED - Check errors above
    echo ============================================
    pause
    exit /b 1
)

echo.
echo Compilation successful! Starting Heronix SchedulerV2...
echo.
echo Using H2 database profile for local development
echo API will be available at: http://localhost:8090
echo.
echo Press Ctrl+C to stop the application
echo ============================================
echo.

:: Use spring-boot:run with H2 profile (no PostgreSQL required)
:: Note: javafx:run spawns a child JVM that doesn't inherit env vars,
:: but spring-boot:run properly passes the profile via -D flag
mvn spring-boot:run -Dspring-boot.run.profiles=h2

pause
