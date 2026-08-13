@echo off
REM ===========================================================================
REM  Double-click to rebuild the app with the newest code and reinstall it.
REM ===========================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rebuild-app.ps1"
