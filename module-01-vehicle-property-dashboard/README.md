# Module 01 — Simple Vehicle Property Dashboard

An **Android Automotive OS** application that reads real-time vehicle signals through the Car API and
displays them on an automotive-optimised dashboard.

Built with **Kotlin** and **Jetpack Compose** (Material 3), running on the Android Automotive emulator.

<p align="center">
  <img src="screenshots/02-driving-55mph.png" alt="The dashboard at 55 MPH in Drive, gauge filled, speed trend rising" width="880">
</p>

---

## The brief

| | |
|---|---|
| **Type** | Individual project |
| **Duration** | 5 working days |
| **Objective** | Build a basic AAOS app in Android Studio that reads real-time vehicle signals and displays the data on the UI |
| **Constraint** | Must run on the Android Automotive emulator and demonstrate real-time updates |

---

## Quick start

**Prerequisites:** Android Studio, JDK 17, an Android Automotive system image, and CPU virtualisation
enabled (Intel VT-x / AMD-V). The full walkthrough — including how to put the entire toolchain on a
non-system drive — is in [docs/01-environment-setup.md](docs/01-environment-setup.md).

```bash
# 1. Open this folder in Android Studio
#    File ▸ Open ▸ module-01-vehicle-property-dashboard
# 2. Let Gradle sync, then:
./gradlew assembleDebug

# 3. With an Automotive emulator running:
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.infosys.cardashboard/.MainActivity
```

Approve the permission dialog on first launch. Then open the emulator's **Extended controls (`…`) ▸ Car
data** and move **Speed / Gear / Fuel** — the dashboard reacts immediately, with no interaction with the
app itself. That is the real-time requirement being demonstrated.

Signals can also be injected from the command line:

```bash
adb shell cmd car_service inject-vhal-event 0x11600207 55   # PERF_VEHICLE_SPEED, m/s
```

> **Prefer not to install anything?** The
> [interactive presentation](https://crstef.github.io/AAOS-interactive-learning-series/) contains a live
> replica of this dashboard driven by sliders.

---

## What it displays

| Signal | Property | Presentation |
|---|---|---|
| Speed | `PERF_VEHICLE_SPEED` | Circular gauge, 0–160 MPH, colour-graded, plus a 40-sample trend sparkline |
| Gear | `GEAR_SELECTION` | P / R / N / D row with the active gear highlighted |
| Fuel | `FUEL_LEVEL` | Meter bar in ml and %, red **LOW FUEL** badge below 15 % |
| Battery | `EV_BATTERY_LEVEL` | Meter bar, rendered only when the vehicle reports the property |
| Range | `RANGE_REMAINING` | Live stat in km |
| Odometer | `PERF_ODOMETER` | Live stat in km |
| Manufacturer | `INFO_MAKE` | Static info card |
| Model | `INFO_MODEL` | Static info card |
| Model year | `INFO_MODEL_YEAR` | Static info card |

Any property the running system image does not provide simply shows `--` rather than a misleading zero.

---

## Project layout

```
module-01-vehicle-property-dashboard/
├── app/src/main/
│   ├── AndroidManifest.xml                  automotive feature, car library, permissions
│   ├── java/com/infosys/cardashboard/
│   │   ├── MainActivity.kt                  permissions, Compose entry point
│   │   ├── VehicleDashboardViewModel.kt     the Car API layer — connect, subscribe, convert
│   │   ├── VehicleUiState.kt                immutable snapshot of everything displayed
│   │   └── ui/
│   │       ├── DashboardScreen.kt           gauge, sparkline, gear row, meter bars, cards
│   │       └── theme/                       Color.kt · Theme.kt · Type.kt
│   └── res/values/                          strings, theme, colours
├── docs/                                    the write-up (see below)
├── screenshots/                             emulator captures
├── slides/                                  presentation deck
└── tooling/                                 setup / run / rebuild / capture automation
```

Five Kotlin source files, plus three theme files and the resources.

---

## Documentation

| Document | What it covers |
|---|---|
| [01 — Environment setup](docs/01-environment-setup.md) | Moving the whole Android toolchain to a non-system drive, and why order matters |
| [02 — Emulator and AVD](docs/02-emulator-and-avd.md) | The automotive AVD's real configuration, and where `android.car.jar` comes from |
| [03 — Architecture](docs/03-architecture.md) | How one property callback becomes a redrawn screen |
| [04 — Car API walkthrough](docs/04-car-api-walkthrough.md) | Connecting, subscribing, and the two-step permission model |
| [05 — Properties and units](docs/05-properties-and-units.md) | Raw units and every conversion applied |
| [06 — What cannot be read](docs/06-what-cannot-be-read.md) | Engine RPM, coolant temperature and door status — and why they are off limits |

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
| Application ID | `com.infosys.cardashboard` |

The one line that makes the whole thing possible, in [`app/build.gradle.kts`](app/build.gradle.kts):

```kotlin
android {
    useLibrary("android.car")   // puts Car, CarPropertyManager, VehiclePropertyIds on the compile classpath
}
```

Without it, every `android.car.*` import fails to resolve. See
[docs/02](docs/02-emulator-and-avd.md#where-androidcarjar-actually-comes-from) for where that library is
found and why installing the platform matters.

---

## Screenshots

| Parked in P, connected | 55 MPH in Drive | Speed trend |
|---|---|---|
| ![Dashboard parked, gauge at 0, Live indicator green](screenshots/01-parked.png) | ![Dashboard at 55 MPH, D highlighted, gauge partly filled](screenshots/02-driving-55mph.png) | ![Dashboard with the speed-trend sparkline drawn](screenshots/03-speed-trend.png) |

Captured with `adb shell screencap` via [`tooling/capture-shots.ps1`](tooling/capture-shots.ps1).

---

## Tooling

The scripts in [`tooling/`](tooling/) automate the whole loop on Windows — installing the SDK and JDK to a
chosen drive, creating the automotive AVD, building, installing, launching, and capturing screenshots.
See [`tooling/README.md`](tooling/README.md).
