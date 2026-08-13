@echo off
REM ===========================================================================
REM  Double-click this file to run the automated AAOS setup (everything on D:).
REM  It just launches the PowerShell script with the right permissions.
REM ===========================================================================
echo Starting AAOS setup... a PowerShell window will open. Keep it open until done.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-aaos-on-D.ps1"
echo.
echo If setup finished OK, run RUN-APP.bat next to start the emulator.
pause
