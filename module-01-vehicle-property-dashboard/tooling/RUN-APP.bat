@echo off
REM ===========================================================================
REM  Double-click this AFTER setup finished, to start the emulator and app.
REM ===========================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-app.ps1"
pause
