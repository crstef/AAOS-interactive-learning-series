# 06 — What cannot be read

> The brief lists engine RPM, engine temperature and door status among the signals to fetch. They are not
> on the dashboard. This is why.

## The short answer

They are guarded by **`signature|privileged`** permissions. An app installed the normal way — side-loaded
with `adb install`, or delivered through a store — cannot hold them, on any emulator, no matter what it
declares in its manifest and no matter what the user taps in a dialog.

| Signal | Property | Required permission | Protection level |
|---|---|---|---|
| Engine RPM | `ENGINE_RPM` | `android.car.permission.CAR_ENGINE_DETAILED` | `signature\|privileged` |
| Engine coolant temperature | `ENGINE_COOLANT_TEMP` | `android.car.permission.CAR_ENGINE_DETAILED` | `signature\|privileged` |
| Engine oil temperature | `ENGINE_OIL_TEMP` | `android.car.permission.CAR_ENGINE_DETAILED` | `signature\|privileged` |
| Door open/closed | `DOOR_POS` | `android.car.permission.CONTROL_CAR_DOORS` | `signature\|privileged` |
| Door lock state | `DOOR_LOCK` | `android.car.permission.CONTROL_CAR_DOORS` | `signature\|privileged` |

Compare with the permissions this app *does* hold — `CAR_SPEED`, `CAR_POWERTRAIN`, `CAR_ENERGY`,
`CAR_MILEAGE` are **`dangerous`**, which is a level a normal app can request and a user can grant. That
single word in the platform manifest is the whole difference.

## What "signature|privileged" means in practice

A permission at that level is granted only if the requesting app either:

1. **is signed with the same key as the platform** — that is, built as part of the AOSP image; or
2. **is installed as a privileged system app** — placed in `/system/priv-app` on a writable system
   partition, *and* allow-listed in the device's privileged-permission XML.

Neither is available to a capstone app on a stock emulator image. `adb shell pm grant` will not do it
either: `pm grant` only works on `dangerous` runtime permissions, and returns an error for these.

The route that *would* work is building a custom AOSP automotive image with the app bundled as a
privileged app — a different project entirely, and well outside a five-day brief.

## How the app behaves instead

It does not declare permissions it cannot obtain, and it does not subscribe to properties it cannot read.
Attempting either produces a `SecurityException` at registration, which — unhandled — takes down the
app on launch.

The six live properties it *does* subscribe to are all reachable with `dangerous` permissions:

```kotlin
private val liveProps = intArrayOf(
    VehiclePropertyIds.PERF_VEHICLE_SPEED,
    VehiclePropertyIds.GEAR_SELECTION,
    VehiclePropertyIds.FUEL_LEVEL,
    VehiclePropertyIds.EV_BATTERY_LEVEL,
    VehiclePropertyIds.RANGE_REMAINING,
    VehiclePropertyIds.PERF_ODOMETER
)
```

And where a property in that list is still unsupported by the running image — `EV_BATTERY_LEVEL` on a
combustion profile, for example — the registration is individually wrapped, the callback never fires, and
the field stays at its `"--"` default:

```kotlin
for (p in liveProps) {
    try { cpm?.registerCallback(callback, p, CarPropertyManager.SENSOR_RATE_ONCHANGE) }
    catch (e: Exception) { Log.w(TAG, "register failed for $p", e) }
}
```

Showing `--` is a deliberate choice over showing `0`. A zero on a dashboard is a reading; a dash is an
absence. Conflating the two is how a driver ends up trusting a gauge that was never connected.

## Verifying it yourself

To see what the running image will actually give you, before writing any UI:

```bash
# What the platform thinks this app holds
adb shell dumpsys package com.infosys.cardashboard | grep -A 20 "requested permissions"

# Confirm a privileged permission cannot simply be granted
adb shell pm grant com.infosys.cardashboard android.car.permission.CAR_ENGINE_DETAILED
# → Operation not allowed: not a changeable permission type
```

## The takeaway

AAOS property access is a three-way gate, and all three have to open:

1. **Does the hardware/HAL provide the property?** — varies by system image.
2. **Does a permission exist for it, at a level this app can hold?** — `dangerous` yes, `signature|privileged` no.
3. **Has the user actually granted it at runtime?** — see [04](04-car-api-walkthrough.md#permissions-are-a-two-step-process).

Most of the time lost to "why is my property null" comes from assuming the first gate is the only one.

---

**Previous:** [05 — Properties and units](05-properties-and-units.md) · **Back to** [module README](../README.md)
