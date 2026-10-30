@echo off
rem ============================================================
rem  LayjMod compile script
rem  Usage: double-click this file, or run `compile.bat` inside
rem  the layjmod folder. Extra args are passed to gradle, e.g.:
rem      compile.bat clean build
rem ============================================================
setlocal EnableDelayedExpansion

call :resolveJdk
if errorlevel 1 exit /b 1
set "JAVA_HOME=%JDK%"
set "Path=%JAVA_HOME%\bin;%Path%"

echo Using JDK: %JDK%
echo.

cd /d "%~dp0"
call gradlew.bat build --console=plain %*

if errorlevel 1 (
    echo.
    echo ============================
    echo BUILD FAILED - see errors above
    echo ============================
    pause
    exit /b 1
)

echo.
echo ============================
echo BUILD OK. Files:
echo ============================
for %%f in ("build\libs\*.jar") do echo %%~nxf

echo.
echo Deploy the mod jar (layjmodX.Y.Z.jar, NOT the -sources one) to the server's mods\ folder.
pause
exit /b 0

:resolveJdk
rem 1) JAVA_HOME from the environment, if it is a JDK 25+ install
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JDK=%JAVA_HOME%"
    exit /b 0
)
rem 2) the JDK this project was built with first
set "JDK=C:\Users\Nataho\AppData\Local\Temp\opencode\jdk\jdk-25.0.5+8"
if exist "!JDK!\bin\javac.exe" exit /b 0
rem 3) anything on PATH with a compiler
for /f "delims=" %%j in ('where javac 2^>nul') do (
    set "JDK=%%~dpj.."
    if exist "!JDK!\bin\javac.exe" (
        set "JDK=%%~dpj.."
        exit /b 0
    )
)
echo ERROR: no JDK found. Install JDK 25 or set JAVA_HOME.
pause
exit /b 1
