# PowerShell commands: Calculator AAOS

Command cheat sheet for working **without Claude**, using only PowerShell.
Detailed explanations of the project are in `EXPLANATIONS.md` (same folder).

> Copy the commands **exactly** as written, including `&` and the quotes.
> All of them were tested in PowerShell 7.

---

## 0. Setup (once, each time you open PowerShell)

```powershell
$adb = "D:\Android\Sdk\platform-tools\adb.exe"
cd D:\Android\repos\AAOS-interactive-learning-series\module-02-calculator
```

- `$adb = ...` creates a **variable** holding the path to adb, so you don't have to type the long path every time.
  You call it with `& $adb ...`. `&` means "run the program stored in this variable".
- The variable lives only while this PowerShell window is open. If you open another one, run this block again.
- `cd` moves you into the project folder, where you run `gradlew.bat`.

---

## 1. Start the AAOS emulator

```powershell
Start-Process D:\Android\Sdk\emulator\emulator.exe -ArgumentList '@AAOS_Capstone', '-gpu', 'auto'
```

- `Start-Process` launches the emulator **separately** from PowerShell, so closing the PowerShell window does not close the emulator.
- `'@AAOS_Capstone'` is the emulator (AVD) name. The quotes are required, because `@` means something else in PowerShell.

**Wait for it to boot** (the command finishes on its own when it's ready):
```powershell
& $adb wait-for-device; while ((& $adb shell getprop sys.boot_completed) -ne "1") { Start-Sleep 3 }; "Emulator ready"
```

**Check the connection:**
```powershell
& $adb devices
```
You should see `emulator-5554   device`.

---

## 2. Put the car in Park (if you see "Can't use this app while driving")

```powershell
& $adb shell cmd car_service inject-vhal-event 0x11600207 0 0
& $adb shell cmd car_service inject-vhal-event 0x11400400 0 4
```

| Code | Property | Value |
|---|---|---|
| `0x11600207` | Speed (m/s) | `0` = stopped |
| `0x11400400` | Gear | `4` = **P** (Park) |

**Check the state:**
```powershell
& $adb shell dumpsys car_service --services CarDrivingStateService | Select-String "Current Driving State"
```
`0` = parked ✅, `1` = idling (stopped, but not in P), `2` = moving 🚫

---

## 3. Build + install (after any code change)

```powershell
.\gradlew.bat installDebug
```

- Compiles the app and installs it on the emulator. You should see `Installed on 1 device` and `BUILD SUCCESSFUL`.
- The emulator must be running, otherwise you get the error `No connected devices!`.
- Build only, without installing: `.\gradlew.bat assembleDebug`
  (the APK goes to `app\build\outputs\apk\debug\app-debug.apk`).

---

## 4. Start / stop the apps

| What | Command |
|---|---|
| Start the **Calculator** | `& $adb shell am start -n com.cristi.calculator/.MainActivity` |
| Start the **Dashboard** | `& $adb shell am start -n com.infosys.cardashboard/.MainActivity` |
| Go to the **Home screen** | `& $adb shell input keyevent KEYCODE_HOME` |
| **Force stop** the Calculator | `& $adb shell am force-stop com.cristi.calculator` |
| **Uninstall** the Calculator | `& $adb uninstall com.cristi.calculator` |

`com.cristi.calculator` is the `applicationId` from `app\build.gradle.kts`, and `.MainActivity` is the Activity declared in the Manifest.

The message `Warning: Activity not started, its current task has been brought to the front` is **normal**: the app was already open and was just brought to the front.

---

## 5. Unit tests

```powershell
.\gradlew.bat testDebugUnitTest
```
Runs the 23 tests for the calculator logic **on the PC, without the emulator**.

**The report, opened in the browser:**
```powershell
Start-Process app\build\reports\tests\testDebugUnitTest\index.html
```

---

## 6. Meeting demo: AAOS driver-distraction safety

**Step 1: start the Calculator (parked, it works):**
```powershell
& $adb shell am start -n com.cristi.calculator/.MainActivity
```

**Step 2: "drive" the car (gear D, 30 km/h):**
```powershell
& $adb shell cmd car_service inject-vhal-event 0x11400400 0 8
& $adb shell cmd car_service inject-vhal-event 0x11600207 0 8.33
```
`8` = gear **D** (Drive). `8.33` m/s = **30 km/h** (km/h ÷ 3.6).
The Calculator is **blocked**: "Can't use this app while driving".

**Step 3: show that the Dashboard still works (it has `distractionOptimized`):**
```powershell
& $adb shell am start -n com.infosys.cardashboard/.MainActivity
```

**Step 4: stop the car and put it in Park:**
```powershell
& $adb shell cmd car_service inject-vhal-event 0x11600207 0 0
& $adb shell cmd car_service inject-vhal-event 0x11400400 0 4
& $adb shell am start -n com.cristi.calculator/.MainActivity
```
The Calculator works again.

You can do the same from the emulator UI: the **⋯** button (Extended controls) → **Car data**.

---

## 7. Troubleshooting

| Problem | Command / fix |
|---|---|
| `no devices/emulators found` | The emulator is closed. See section 1 |
| `emulator-5554 offline` | Still booting. Wait and run `& $adb devices` again |
| `No connected devices!` during build | Same: start the emulator before `installDebug` |
| "Can't use this app while driving" | Section 2 (Park) |
| The app closes by itself (crash) | `& $adb logcat -d -b crash` shows the error |
| adb stuck or acting weird | `& $adb kill-server; & $adb start-server` |
| Black screen when the emulator starts | The snapshot restored an open app. Press 🏠 or `& $adb shell input keyevent KEYCODE_HOME` |

---

## 8. Close the emulator

```powershell
& $adb shell input keyevent KEYCODE_HOME
& $adb emu kill
```

Go Home first, so the emulator doesn't open straight into an app next time.
`emu kill` closes the emulator and saves the snapshot, so the next start is fast.
You can also click **✕** on the emulator window: the effect is the same.

---

## Quick sheet

```powershell
$adb = "D:\Android\Sdk\platform-tools\adb.exe"; cd D:\Android\repos\AAOS-interactive-learning-series\module-02-calculator
Start-Process D:\Android\Sdk\emulator\emulator.exe -ArgumentList '@AAOS_Capstone','-gpu','auto'
& $adb wait-for-device; while ((& $adb shell getprop sys.boot_completed) -ne "1") { Start-Sleep 3 }; "Emulator ready"
& $adb shell cmd car_service inject-vhal-event 0x11600207 0 0; & $adb shell cmd car_service inject-vhal-event 0x11400400 0 4
.\gradlew.bat installDebug
& $adb shell am start -n com.cristi.calculator/.MainActivity
```
