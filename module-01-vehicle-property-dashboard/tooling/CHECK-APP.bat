@echo off
REM ===========================================================================
REM  If the app misbehaves: reproduce the problem on the emulator, THEN
REM  double-click this file. It saves the app log so the error can be inspected.
REM ===========================================================================
echo Saving the app log to D:\Android\app-log.txt ...
"D:\Android\Sdk\platform-tools\adb.exe" logcat -d -v time -t 4000 > "D:\Android\app-log.txt"
echo.
echo Done. Open D:\Android\app-log.txt and look for the error.
pause
