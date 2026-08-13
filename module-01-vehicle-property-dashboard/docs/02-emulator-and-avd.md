# 02 — Emulator and AVD

> The automotive virtual device, its real configuration, and where `android.car.jar` actually comes from.

## Why an automotive image is mandatory

An ordinary phone AVD will install this APK and launch it without complaint. It will then never deliver a
single vehicle property — `Car.createCar()` returns a car whose `PROPERTY_SERVICE` manager is null, and
the dashboard sits at `--` forever.

There is no error message for this. The app is fine; the platform simply has no vehicle HAL. It is worth
confirming the running image really is automotive before debugging anything else:

```bash
adb shell pm list features | findstr automotive
# expect: feature:android.hardware.type.automotive
```

The app declares the same requirement, so a real device without automotive hardware will not accept it at
all:

```xml
<uses-feature android:name="android.hardware.type.automotive" android:required="true" />
```

## The virtual device as configured

Created by [`tooling/setup-aaos-on-D.ps1`](../tooling/setup-aaos-on-D.ps1) and tuned for a 20 GB machine.
These are the values from the generated `config.ini`:

| Setting | Value |
|---|---|
| System image | `system-images/android-33/android-automotive/x86_64` |
| Tags | `android-automotive`, `google_apis` |
| ABI | `x86_64` |
| Screen | 1024 × 768, density 160 — landscape, as a head unit is |
| RAM | 3072 MB |
| VM heap | 512 MB |
| CPU cores | 4 |
| Data partition | 7000 MB |
| GPU | enabled, mode `auto` |
| Hardware keyboard | yes |

3072 MB of RAM is a deliberate compromise. The automotive image is heavier than a phone image, but the
host only has 20 GB and Android Studio and Gradle want their share — leaving the emulator at a default
larger allocation made the whole machine swap.

## Where `android.car.jar` actually comes from

This is the least obvious part of AAOS development.

The `Car`, `CarPropertyManager` and `VehiclePropertyIds` classes are **not** part of the standard Android
SDK, and they are **not** provided by the system image either. The system image is what the emulator
*runs*; it contributes nothing to the compile classpath.

They ship inside the installed **platform**, as an optional library:

```
Sdk/platforms/android-34/optional/android.car.jar
```

alongside an `optional.json` that registers it under the name `android.car`:

```json
{ "name": "android.car", "jar": "android.car.jar", "manifest": false }
```

That registered name is exactly what the Gradle declaration resolves:

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 34
    useLibrary("android.car")
}
```

Omit `useLibrary` and every `android.car.*` import fails to resolve at compile time, no matter which
emulator is running. Install only the system image without `platforms;android-34` and the jar is not on
disk at all.

At runtime the pairing is completed in the manifest, which asks the platform to load the shared library
into the app's process:

```xml
<uses-library android:name="android.car" android:required="false" />
```

`required="false"` is intentional — it keeps the same APK installable on a non-automotive device during
development instead of failing at install time with a missing-library error.

## Driving the emulator

With the app running, signals are injected without touching the app itself — which is precisely what
demonstrates that updates are event-driven rather than polled:

**From the UI:** Extended controls (the `…` button on the side toolbar) ▸ **Car data** ▸ move Speed,
Gear or Fuel.

**From the command line:**

```bash
adb shell cmd car_service inject-vhal-event 0x11600207 55    # PERF_VEHICLE_SPEED (m/s)
adb shell cmd car_service inject-vhal-event 0x11400400 8     # GEAR_SELECTION (8 = Drive)
```

**Capturing the result:**

```bash
adb shell screencap -p /sdcard/shot.png && adb pull /sdcard/shot.png
```

[`tooling/capture-shots.ps1`](../tooling/capture-shots.ps1) wraps that last pair — it uses
`screencap` + `pull` rather than piping `exec-out` through PowerShell, which corrupts the PNG on Windows
because the pipeline re-encodes the binary stream.

---

**Previous:** [01 — Environment setup](01-environment-setup.md) · **Next:** [03 — Architecture](03-architecture.md)
