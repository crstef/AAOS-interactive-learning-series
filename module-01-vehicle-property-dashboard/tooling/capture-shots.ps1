<#
  Captures the current emulator screen to D:\Android\screenshots\shotN.png
  Uses adb pull (no binary corruption). Run it several times for several shots.
#>
$ErrorActionPreference = "Continue"
$Root = "D:\Android"
$adb  = "$Root\Sdk\platform-tools\adb.exe"
$dir  = "$Root\screenshots"

if (-not (Test-Path $adb)) { Write-Host "adb not found - run INSTALL.bat first."; Read-Host "Enter"; exit 1 }

& $adb start-server | Out-Null
$state = (& $adb get-state) 2>$null
if ($state -ne "device") {
    Write-Host "No emulator running. Start it first with RUN-APP.bat, then try again."
    Read-Host "Press Enter to close"; exit 1
}

New-Item -ItemType Directory -Force -Path $dir | Out-Null
$n = (@(Get-ChildItem $dir -Filter "shot*.png" -ErrorAction SilentlyContinue)).Count + 1
$out = "$dir\shot$n.png"

& $adb shell screencap -p /sdcard/aaos_shot.png
& $adb pull /sdcard/aaos_shot.png "$out" | Out-Null
& $adb shell rm /sdcard/aaos_shot.png

if (Test-Path $out) {
    Write-Host ""
    Write-Host "  Saved: $out"
    Write-Host ""
    Write-Host "  Change the dashboard (speed / gear / fuel via '...' -> Car data)"
    Write-Host "  and run CAPTURE.bat again for another shot."
} else {
    Write-Host "Could not save the screenshot. Is the emulator on screen?"
}
Read-Host "Press Enter to close"
