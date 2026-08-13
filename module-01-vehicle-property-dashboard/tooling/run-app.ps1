<#
  ============================================================================
  AAOS Capstone - Launch the emulator and install the app
  ============================================================================
  Run this AFTER setup-aaos-on-D.ps1 has finished successfully.
  It starts the Automotive emulator, waits for it to boot, installs the app,
  grants the vehicle permissions, and opens the dashboard.

  HOW TO RUN:  powershell -ExecutionPolicy Bypass -File .\run-app.ps1
  ============================================================================
#>

$ErrorActionPreference = "Stop"

$Root    = "D:\Android"
$SdkDir  = "$Root\Sdk"
$AvdName = "AAOS_Capstone"
$Pkg     = "com.infosys.cardashboard"
$Apk     = "$Root\Projects\AutomotiveCarPropApp\app\build\outputs\apk\debug\app-debug.apk"

$emulator = "$SdkDir\emulator\emulator.exe"
$adb      = "$SdkDir\platform-tools\adb.exe"

function Say($m) { $t=(Get-Date).ToString("HH:mm:ss"); Write-Host "$t  $m" }

if (-not (Test-Path $emulator)) { Write-Host "Emulator not found. Run setup-aaos-on-D.ps1 first."; exit 1 }
if (-not (Test-Path $Apk))      { Write-Host "APK not found. Run setup-aaos-on-D.ps1 first."; exit 1 }

# Quick acceleration check (warn only).
Say "Checking hardware acceleration..."
& $emulator -accel-check 2>&1 | ForEach-Object { Say "  $_" }

# Start the emulator in its own window (non-blocking).
Say "Starting the Automotive emulator (first cold boot can take a few minutes)..."
Start-Process -FilePath $emulator -ArgumentList "@$AvdName","-gpu","auto","-no-snapshot-load"

# Wait for the device, then for full boot.
Say "Waiting for the emulator to connect..."
& $adb start-server | Out-Null
& $adb wait-for-device

Say "Waiting for Android to finish booting..."
do {
    Start-Sleep -Seconds 5
    $booted = (& $adb shell getprop sys.boot_completed 2>$null).Trim()
    Say "  boot_completed = '$booted'"
} until ($booted -eq "1")
Say "Emulator is up."

# Install the app.
Say "Installing the app..."
& $adb install -r "$Apk"
if ($LASTEXITCODE -ne 0) { Write-Host "Install failed."; exit 1 }

# Grant the runtime vehicle permissions so values show immediately.
Say "Granting vehicle permissions..."
foreach ($p in @(
    "android.car.permission.CAR_SPEED",
    "android.car.permission.CAR_POWERTRAIN",
    "android.car.permission.CAR_ENERGY")) {
    & $adb shell pm grant $Pkg $p 2>$null
}

# Launch the dashboard.
Say "Launching the dashboard..."
& $adb shell am start -n "$Pkg/.MainActivity" | Out-Null

Say ""
Say "Done! The dashboard is on the emulator screen."
Say "To see values change in real time: on the emulator, click the '...'"
Say "(Extended controls) button -> 'Car data' tab -> move Speed / Gear / Fuel."
Say "Take 2-3 screenshots with the camera button for your capstone."
