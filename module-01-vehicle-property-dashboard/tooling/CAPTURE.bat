@echo off
REM ===========================================================================
REM  Saves a picture of the emulator screen to D:\Android\screenshots
REM  Run it a few times with the dashboard in different states.
REM ===========================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0capture-shots.ps1"
