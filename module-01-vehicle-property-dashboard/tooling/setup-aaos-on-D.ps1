<#
  ============================================================================
  AAOS Capstone - Automated setup, everything on the D: drive
  ============================================================================
  Installs (all onto D:, no Android Studio needed):
    Java JDK 17, Android SDK + command-line tools, platform-tools, emulator,
    the AAOS system image, a tuned emulator (AVD), and builds the app (APK).

  HOW TO RUN:  right-click INSTALL.bat -> Run.   Safe to run again; finished
  steps are skipped. No administrator rights needed. Needs internet (~3-4 GB).
  ============================================================================
#>

$ErrorActionPreference = "Stop"        # PowerShell cmdlets stop on real errors
$ProgressPreference    = "SilentlyContinue"
# Never let a native tool's non-zero exit or stderr text auto-throw; we check
# exit codes ourselves. (Harmless no-op on Windows PowerShell 5.1.)
$PSNativeCommandUseErrorActionPreference = $false
try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 } catch {}

# ---- Where everything goes (all on D:; none of these paths contain spaces) --
$Root      = "D:\Android"
$SdkDir    = "$Root\Sdk"
$JdkDir    = "$Root\jdk"
$AndHome   = "$Root\.android"
$AvdHome   = "$Root\.android\avd"
$GradleDir = "$Root\.gradle"
$DlDir     = "$Root\downloads"
$ProjDir   = "$Root\Projects\AutomotiveCarPropApp"
$Zip       = "$Root\AutomotiveCarPropApp.zip"
$Log       = "$Root\setup-log.txt"
$AvdName   = "AAOS_Capstone"

function Say($m)  { $t = (Get-Date).ToString("HH:mm:ss"); "$t  $m" | Tee-Object -FilePath $Log -Append }
function Ok($m)   { Say "[ OK ] $m" }
function Step($m) { Say ""; Say "==== $m ====" }
function Fail($m) { Say "[FAIL] $m"; Say "Full log: $Log  - check the last ~20 lines."; exit 1 }

# Run a native tool through cmd.exe, sending its output (including harmless
# 'deprecated' warnings) to the log, and return the real exit code. Because cmd
# does the redirection, PowerShell never sees the tool's stderr as an error.
# (Our D: paths have no spaces, so no extra quoting is required.)
function NativeRun([string]$CmdLine) {
    cmd /c "$CmdLine 1>> $Log 2>&1"
    return $LASTEXITCODE
}

"" | Out-File $Log
Say "AAOS setup starting. Log: $Log"

# ---- 0. Preflight ----------------------------------------------------------
Step "0/7  Checking D: drive"
if (-not (Test-Path "D:\")) { Fail "There is no D: drive on this machine." }
foreach ($d in @($Root,$SdkDir,$JdkDir,$AndHome,$AvdHome,$GradleDir,$DlDir,"$Root\Projects")) {
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}
$free = [math]::Round((Get-PSDrive D).Free/1GB,1)
Say "Free space on D: = $free GB"
if ($free -lt 25) { Say "[WARN] Less than 25 GB free on D:. Keep an eye on it." }
Ok "Folders ready under $Root"

# Answer files for tool prompts (many 'y' for licenses, one 'no' for AVD).
$YesFile = "$DlDir\yes.txt"; (("y`r`n") * 100) | Out-File -FilePath $YesFile -Encoding ascii
$NoFile  = "$DlDir\no.txt";  "no"              | Out-File -FilePath $NoFile  -Encoding ascii

# ---- 1. Environment variables (persist to User scope + this process) -------
Step "1/7  Setting D: environment variables"
$vars = @{
    "ANDROID_HOME"      = $SdkDir
    "ANDROID_SDK_ROOT"  = $SdkDir
    "ANDROID_USER_HOME" = $AndHome
    "ANDROID_AVD_HOME"  = $AvdHome
    "GRADLE_USER_HOME"  = $GradleDir
}
foreach ($k in $vars.Keys) {
    [Environment]::SetEnvironmentVariable($k, $vars[$k], "User")
    Set-Item -Path "Env:$k" -Value $vars[$k]
    Say "  $k = $($vars[$k])"
}
Ok "Environment variables set"

# ---- 2. Java JDK 17 --------------------------------------------------------
Step "2/7  Java JDK 17 on D:"
$haveJdk = Get-ChildItem $JdkDir -Directory -ErrorAction SilentlyContinue |
           Where-Object { Test-Path "$($_.FullName)\bin\java.exe" }
if (-not $haveJdk) {
    $jdkZip = "$DlDir\jdk17.zip"
    $jdkUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
    Say "  Downloading JDK..."
    Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkZip -UseBasicParsing
    Say "  Extracting JDK..."
    Expand-Archive -Path $jdkZip -DestinationPath $JdkDir -Force
} else { Say "  JDK already present, skipping" }

$JavaHome = (Get-ChildItem $JdkDir -Directory | Where-Object { Test-Path "$($_.FullName)\bin\java.exe" } | Select-Object -First 1).FullName
if (-not $JavaHome) { Fail "Could not find java.exe after extracting the JDK." }
[Environment]::SetEnvironmentVariable("JAVA_HOME", $JavaHome, "User")
$env:JAVA_HOME = $JavaHome
$env:Path      = "$JavaHome\bin;$env:Path"
Ok "JDK ready: $JavaHome"

# ---- 3. Android command-line tools -----------------------------------------
Step "3/7  Android command-line tools on D:"
$cmdlineBin = "$SdkDir\cmdline-tools\latest\bin"
if (-not (Test-Path "$cmdlineBin\sdkmanager.bat")) {
    $base = "https://dl.google.com/android/repository/"
    $file = "commandlinetools-win-11076708_latest.zip"   # fallback
    try {
        $xml = (Invoke-WebRequest ($base + "repository2-1.xml") -UseBasicParsing).Content
        if ($xml -match "commandlinetools-win-\d+_latest\.zip") { $file = $Matches[0] }
    } catch { Say "  (Using fallback tools version)" }
    $ctZip = "$DlDir\cmdline-tools.zip"
    Say "  Downloading $file ..."
    Invoke-WebRequest -Uri ($base + $file) -OutFile $ctZip -UseBasicParsing
    Say "  Extracting..."
    $tmp = "$DlDir\ct-extract"
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    Expand-Archive -Path $ctZip -DestinationPath $tmp -Force
    New-Item -ItemType Directory -Force -Path "$SdkDir\cmdline-tools" | Out-Null
    Remove-Item "$SdkDir\cmdline-tools\latest" -Recurse -Force -ErrorAction SilentlyContinue
    Move-Item "$tmp\cmdline-tools" "$SdkDir\cmdline-tools\latest"
} else { Say "  command-line tools already present, skipping" }

$sdkmanager = "$cmdlineBin\sdkmanager.bat"
$avdmanager = "$cmdlineBin\avdmanager.bat"
if (-not (Test-Path $sdkmanager)) { Fail "sdkmanager not found after extraction." }
Ok "Command-line tools ready"

# ---- 4. Accept licenses + install SDK packages -----------------------------
Step "4/7  Installing SDK packages (the big download - be patient)"
Say "  Accepting SDK licenses..."
NativeRun "$sdkmanager --licenses --sdk_root=$SdkDir < $YesFile" | Out-Null

Say "  Looking up available Automotive system images..."
$imagePkg = $null
try {
    $list = cmd /c "$sdkmanager --list --sdk_root=$SdkDir 2>nul"
    $cand = $list | Select-String -Pattern "system-images;android-\d+;android-automotive(-playstore)?;x86_64" -AllMatches |
            ForEach-Object { $_.Matches.Value } | Sort-Object -Unique
    $imagePkg = $cand | Where-Object { $_ -match "android-automotive;x86_64" } | Sort-Object -Descending | Select-Object -First 1
    if (-not $imagePkg) { $imagePkg = $cand | Sort-Object -Descending | Select-Object -First 1 }
} catch {}
if (-not $imagePkg) { $imagePkg = "system-images;android-33;android-automotive;x86_64" }
Say "  Chosen system image: $imagePkg"

Say "  Installing platform-tools, emulator, platform 34, build-tools, AAOS image..."
Say "  (No progress shown here - it is downloading to D:. Peek at the log if curious.)"
$code = NativeRun "$sdkmanager --sdk_root=$SdkDir platform-tools emulator platforms;android-34 build-tools;34.0.0 $imagePkg < $YesFile"
if ($code -ne 0) { Fail "SDK package install failed. Often a network drop - just run INSTALL.bat again." }
Ok "SDK packages installed"

# ---- 5. Create the emulator (AVD), tuned for a 20 GB laptop ----------------
Step "5/7  Creating the emulator (AVD): $AvdName"
NativeRun "$avdmanager delete avd -n $AvdName" | Out-Null
$device = "automotive_1024p_landscape"
$code = NativeRun "$avdmanager create avd -n $AvdName -k $imagePkg -d $device < $NoFile"
if ($code -ne 0) {
    Say "  Retrying AVD creation without a specific device profile..."
    $code = NativeRun "$avdmanager create avd -n $AvdName -k $imagePkg < $NoFile"
    if ($code -ne 0) { Fail "Could not create the AVD (see log)." }
}

$cfg = "$AvdHome\$AvdName.avd\config.ini"
if (Test-Path $cfg) {
    $tune = @{
        "hw.ramSize"              = "3072"
        "vm.heapSize"             = "512"
        "hw.cpu.ncore"            = "4"
        "disk.dataPartition.size" = "7000M"
        "hw.gpu.enabled"          = "yes"
        "hw.gpu.mode"             = "auto"
        "hw.keyboard"             = "yes"
    }
    $lines = Get-Content $cfg
    foreach ($key in $tune.Keys) {
        if ($lines -match "^$([regex]::Escape($key))=") {
            $lines = $lines -replace "^$([regex]::Escape($key))=.*", "$key=$($tune[$key])"
        } else { $lines += "$key=$($tune[$key])" }
    }
    $lines | Set-Content $cfg
    Ok "Emulator created and tuned (3 GB RAM, 4 cores)"
} else { Say "[WARN] AVD created but config.ini not found; defaults will be used." }

# ---- 6. Extract the project ------------------------------------------------
Step "6/7  Extracting the app project"
if (Test-Path $ProjDir) { Say "  Project folder already exists, leaving as-is" }
elseif (Test-Path $Zip)  { Expand-Archive -Path $Zip -DestinationPath "$Root\Projects" -Force; Ok "Project extracted to $ProjDir" }
else { Fail "Neither $Zip nor $ProjDir found. Put the project on D: first." }

# ---- 7. Build the app ------------------------------------------------------
Step "7/7  Building the app (first build downloads Gradle + libraries to D:)"
$code = NativeRun "cd /d $ProjDir && gradlew.bat assembleDebug --no-daemon"
if ($code -ne 0) { Fail "Gradle build failed. Check the last ~20 lines of $Log." }
$apk = "$ProjDir\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) { Ok "APK built: $apk" } else { Fail "Build finished but APK not found." }

# ---- Done ------------------------------------------------------------------
Say ""
Say "============================================================"
Ok  "ALL DONE. Everything installed on D:."
Say "============================================================"
Say "Next:"
Say "  1) Check acceleration:  $SdkDir\emulator\emulator.exe -accel-check"
Say "     If NOT enabled: turn on Intel Virtualization (VT-x) in BIOS, or the"
Say "     'Windows Hypervisor Platform' Windows feature."
Say "  2) Start the emulator + app:  double-click RUN-APP.bat"
Say "============================================================"
