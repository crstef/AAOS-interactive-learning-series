# Module 02 — Calculator

A simple calculator for **Android Automotive OS**, built from an empty folder to a working app on the
automotive emulator. Unlike Module 01 it uses **no Car API**: the focus is on the fundamentals of an AAOS
app (project structure, Gradle, the Manifest, Compose UI, state, testable logic) and on the
**driver-distraction rules** that AAOS enforces.

Built with **Kotlin** and **Jetpack Compose** (Material 3), running on the Android Automotive emulator.

---

## What it does

| Button | Action |
|---|---|
| `0`–`9`, `.` | Type a number (up to 12 digits, one decimal point) |
| `+` `−` `×` `÷` | Choose the operation; chained operations work (`2 + 3 ×` shows `5`) |
| `=` | Compute the result; the line above shows the full expression (`12 + 3 =`) |
| `C` / `⌫` | Clear everything / delete the last digit |
| `±` / `%` | Change the sign / divide by 100 |

- Exact decimal arithmetic with `BigDecimal`: `0.1 + 0.2 = 0.3`, not `0.30000000000000004`.
- Division by zero shows `Error`.
- Landscape 5 × 4 keypad with large touch targets (above the 76 dp automotive minimum), a dark theme and
  color-coded keys.
- A custom adaptive launcher icon.

---

## Quick start

**Prerequisites:** JDK 17, the Android SDK and an Android Automotive emulator. The environment from
[Module 01](../module-01-vehicle-property-dashboard/docs/01-environment-setup.md) works as-is.

```bash
cd module-02-calculator
./gradlew testDebugUnitTest      # 23 unit tests, no emulator needed
./gradlew installDebug           # build + install on the running emulator
adb shell am start -n com.cristi.calculator/.MainActivity
```

If the emulator shows **"Can't use this app while driving"**, the simulated car isn't parked.
Set speed 0 and gear P:

```bash
adb shell cmd car_service inject-vhal-event 0x11600207 0 0   # PERF_VEHICLE_SPEED = 0 m/s
adb shell cmd car_service inject-vhal-event 0x11400400 0 4   # GEAR_SELECTION = P
```

---

## Driver-distraction demo

The calculator deliberately does **not** declare `distractionOptimized`: typing on a keypad is exactly the
kind of interaction AAOS keeps away from a moving driver.

1. Parked: the calculator works.
2. Gear D, 30 km/h (`inject-vhal-event 0x11400400 0 8`, then `0x11600207 0 8.33`): AAOS replaces it with
   **"Can't use this app while driving"**.
3. The Module 01 dashboard, which *is* `distractionOptimized`, keeps working.
4. Back to speed 0 and gear P: the calculator is available again.

---

## Project layout

```
module-02-calculator/
├── app/src/main/
│   ├── AndroidManifest.xml              automotive feature, launcher activity, icon
│   ├── java/com/cristi/calculator/
│   │   ├── MainActivity.kt              theme (MaterialTheme + Surface) and entry point
│   │   ├── CalculatorScreen.kt          display + 5 × 4 keypad (stateless Compose UI)
│   │   ├── CalculatorViewModel.kt       holds the state across Activity recreation
│   │   └── CalculatorEngine.kt          the logic: pure Kotlin, no Android
│   └── res/                             strings, colors, adaptive icon
├── app/src/test/.../CalculatorEngineTest.kt   23 JUnit tests for the engine
├── EXPLANATIONS.md                      detailed write-up
└── COMMANDS.md                          PowerShell cheat sheet
```

The logic, the state and the UI live in separate files. That's why the whole engine can be unit tested
on the PC in seconds:

```
CalculatorScreen ──onKey──▶ CalculatorViewModel ──press(state, key)──▶ CalculatorEngine
        ▲                            │
        └──────────state─────────────┘
```

---

## Documentation

| Document | What it covers |
|---|---|
| [EXPLANATIONS.md](EXPLANATIONS.md) | Every configuration file, the architecture, each Kotlin file, the tests, what happens on a button press, the problems hit along the way and the lessons, extension ideas, a glossary |
| [COMMANDS.md](COMMANDS.md) | PowerShell commands to start the emulator, park the car, build, test, launch and run the safety demo |

> `COMMANDS.md` uses the author's local paths (`D:\Android\...`). Adjust them to your own SDK and
> project locations.

---

## Build configuration

| | |
|---|---|
| `compileSdk` / `targetSdk` | 34 |
| `minSdk` | 29 |
| Java / JVM target | 17 |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.6.1 |
| Compose BOM | 2024.09.03 |
| Application ID | `com.cristi.calculator` |

Compared with Module 01, this project removes `useLibrary("android.car")`, the vehicle permissions and
`distractionOptimized`, and adds JUnit for the unit tests.
