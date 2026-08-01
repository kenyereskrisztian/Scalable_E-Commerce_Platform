@echo off
title Scalable E-Commerce Platform - osszes service inditasa
setlocal
cd /d "%~dp0.."

echo ============================================================
echo   Scalable E-Commerce Platform - service-ek inditasa
echo ============================================================
echo.

rem ---------- JDK 17 kereses ----------
set "JAVA_EXE="
if exist "%USERPROFILE%\.jdks\corretto-17.0.19\bin\java.exe" set "JAVA_EXE=%USERPROFILE%\.jdks\corretto-17.0.19\bin\java.exe"
if not defined JAVA_EXE if exist "%USERPROFILE%\.jdks\corretto-17.0.18\bin\java.exe" set "JAVA_EXE=%USERPROFILE%\.jdks\corretto-17.0.18\bin\java.exe"
if not defined JAVA_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
    echo [HIBA] JDK 17 nem talalhato. Telepitsd vagy allitsd be a JAVA_HOME valtozot!
    pause
    exit /b 1
)
set "JAVA_HOME=%JAVA_EXE%\..\.."

rem ---------- Maven kereses ----------
set "MAVEN_CMD="
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MAVEN_CMD if exist "C:\apache-maven\apache-maven-3.9.16\bin\mvn.cmd" set "MAVEN_CMD=C:\apache-maven\apache-maven-3.9.16\bin\mvn.cmd"
if not defined MAVEN_CMD (
    echo [HIBA] Maven nem talalhato. Telepitsd vagy allitsd be a MAVEN_HOME valtozot!
    pause
    exit /b 1
)

echo  Java:   %JAVA_EXE%
echo  Maven:  %MAVEN_CMD%
echo.
echo  1. lepes: osszes modul forditasa es jar keszites (skip tesztek)...
echo.

call "%MAVEN_CMD%" -DskipTests install
if errorlevel 1 (
    echo.
    echo  [HIBA] A build sikertelen volt, a service-ek NEM indulnak el!
    pause
    exit /b 1
)

echo.
echo  2. lepes: service-ek inditasa kulon ablakokban...
echo.

start "user-service         [8081]" cmd /k ""%JAVA_EXE%" -jar "user-service\target\user-service-0.0.1-SNAPSHOT.jar""
start "product-service      [8082]" cmd /k ""%JAVA_EXE%" -jar "product-service\target\product-service-0.0.1-SNAPSHOT.jar""
start "cart-service         [8083]" cmd /k ""%JAVA_EXE%" -jar "cart-service\target\cart-service-0.0.1-SNAPSHOT.jar""
start "order-service        [8084]" cmd /k ""%JAVA_EXE%" -jar "order-service\target\order-service-0.0.1-SNAPSHOT.jar""
start "payment-service      [8085]" cmd /k ""%JAVA_EXE%" -jar "payment-service\target\payment-service-0.0.1-SNAPSHOT.jar""
start "notification-service [8086]" cmd /k ""%JAVA_EXE%" -jar "notification-service\target\notification-service-0.0.1-SNAPSHOT.jar""

echo.
echo  3. lepes: frontend inditasa (http://localhost:5500)...
echo.

start "frontend [5500]" cmd /k "python -m http.server 5500 --directory frontend"

echo.
echo  Az osszes service inditasi parancs kiadva. Varj 30-60 masodpercet,
echo  amig mindegyik teljesen elindul (az adatbazisok mar leteznek).
echo.
echo  Ellenorzes:
echo    - http://localhost:8081/api/auth/login   (user-service)
echo    - http://localhost:8082/api/products     (product-service)
echo    - stb. 8083-8086
echo.
echo  Frontend (kulon ablakban indult):
echo    http://localhost:5500
echo.
echo  Demo fiok: demo1@shop.hu / password123
echo.
echo  A service-ek bezarasa: csukd be a hozzatartozo ablakokat.
echo.
pause
