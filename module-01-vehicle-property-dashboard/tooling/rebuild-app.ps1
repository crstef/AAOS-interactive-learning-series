<#
  ============================================================================
  AAOS Capstone - Rebuild the app with the latest code and reinstall it
  ============================================================================
  Use this after the project sources change. It:
    1. Refreshes the project files from the new AutomotiveCarPropApp.zip on D:
    2. Rebuilds the app (APK)
    3. Makes sure the emulator is running, then installs + launches the new app

  HOW TO RUN:  double-click REBUILD-APP.bat
  Everything stays on D:. A log is written to D:\Android\rebuild-log.txt
  ============================================================================
#>

$ErrorActionPreference = "Continue"
$ProgressPreference    = "SilentlyContinue"
$PSNativeCommandUseErrorActionPreference = $false

$Root    = "D:\Android"
$SdkDir  = "$Root\Sdk"
$Zip     = "$Root\AutomotiveCarPropApp.zip"
$ProjDir = "$Root\Projects\AutomotiveCarPropApp"
$Pkg     = "com.infosys.cardashboard"
$Apk     = "$ProjDir\app\build\outputs\apk\debug\app-debug.apk"
$Log     = "$Root\rebuild-log.txt"
$AvdName = "AAOS_Capstone"
$adb      = "$SdkDir\platform-tools\adb.exe"
$emulator = "$SdkDir\emulator\emulator.exe"

function Say($m) { $t=(Get-Date).ToString("HH:mm:ss"); "$t  $m" | Tee-Object -FilePath $Log -Append }
"" | Out-File $Log

# Make sure JAVA_HOME points at the JDK we installed on D:.
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $j = Get-ChildItem "$Root\jdk" -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path "$($_.FullName)\bin\java.exe" } | Select-Object -First 1
    if ($j) { $env:JAVA_HOME = $j.FullName }
}
$env:ANDROID_HOME = $SdkDir
Say "JAVA_HOME = $env:JAVA_HOME"

# 1. Refresh project files from the latest zip (overwrites the source files).
if (Test-Path $Zip) {
    Say "Updating project files from $Zip ..."
    Expand-Archive -Path $Zip -DestinationPath "$Root\Projects" -Force
} else { Say "No zip found; building whatever is already in $ProjDir" }

# 2. Rebuild. cmd handles redirection so tool warnings never abort us.
Say "Building the app (this can take a minute)..."
cmd /c "cd /d $ProjDir && gradlew.bat assembleDebug --no-daemon 1>> $Log 2>&1"
if ($LASTEXITCODE -ne 0) {
    Say "[FAIL] Build failed. Open $Log and check the last ~25 lines."
    Read-Host "Press Enter to close"
    exit 1
}
if (-not (Test-Path $Apk)) { Say "[FAIL] Build finished but APK missing."; Read-Host "Press Enter"; exit 1 }
Say "[ OK ] APK built."

# 3. Make sure an emulator is running.
& $adb start-server | Out-Null
$state = (& $adb get-state) 2>$null
if ($state -ne "device") {
    Say "No emulator running - starting it (first boot can take a few minutes)..."
    Start-Process -FilePath $emulator -ArgumentList "@$AvdName","-gpu","auto","-no-snapshot-load"
    & $adb wait-for-device
    do { Start-Sleep -Seconds 5; $b = (& $adb shell getprop sys.boot_completed 2>$null).Trim() } until ($b -eq "1")
}
Say "Emulator is up."

# Install + grant permissions + launch.
Say "Installing the updated app..."
& $adb install -r "$Apk" 1>> $Log 2>&1
foreach ($p in @(
    "android.car.permission.CAR_SPEED",
    "android.car.permission.CAR_POWERTRAIN",
    "android.car.permission.CAR_ENERGY",
    "android.car.permission.CAR_MILEAGE")) {
    & $adb shell pm grant $Pkg $p 2>$null
}
& $adb shell am start -n "$Pkg/.MainActivity" 1>> $Log 2>&1

Say "Done! The updated dashboard should be on the emulator."
Say "Open the emulator's '...' -> Car data panel and move Speed/Gear/Fuel to see it react."
Read-Host "Press Enter to close"
