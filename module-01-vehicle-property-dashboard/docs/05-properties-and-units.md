# 05 — Properties and units

> The Car API returns raw machine values. Every one of them needs work before it can be shown.

## The properties read

**Live** — subscribed with `SENSOR_RATE_ONCHANGE`, pushed by the platform:

| Property | Raw type & unit | Conversion applied | Displayed as |
|---|---|---|---|
| `PERF_VEHICLE_SPEED` | `Float`, metres/second | `× 2.23694`, rounded | `55` MPH on the gauge |
| `GEAR_SELECTION` | `Int` bit constant | mapped to a label | `D (Drive)`, and the P/R/N/D highlight |
| `FUEL_LEVEL` | `Float`, millilitres | `÷ INFO_FUEL_CAPACITY × 100` | `15000 ml (100%)` + bar |
| `EV_BATTERY_LEVEL` | `Float`, percent on this image | clamped to `0..100` | `100%` + bar |
| `RANGE_REMAINING` | `Float`, metres | `÷ 1000`, rounded | `310 km` |
| `PERF_ODOMETER` | `Float`, kilometres | rounded, thousands-separated | `12,345 km` |

**Static** — read once at connect:

| Property | Type | Used for |
|---|---|---|
| `INFO_MAKE` | `String` | Manufacturer card |
| `INFO_MODEL` | `String` | Car Model card |
| `INFO_MODEL_YEAR` | numeric | Model Year card |
| `INFO_FUEL_CAPACITY` | `Float`, millilitres | Denominator for the fuel percentage — never displayed |

## Speed: m/s → MPH

```kotlin
val mph = ((num ?: 0f) * 2.23694f).roundToInt()
```

The gauge is scaled to 160 MPH and colour-graded by fraction of full scale, in
[`DashboardScreen.kt`](../app/src/main/java/com/infosys/cardashboard/ui/DashboardScreen.kt):

```kotlin
val arcColor = when {
    target > 0.8f  -> BadRed      // above 128 MPH
    target > 0.55f -> WarnAmber   // above 88 MPH
    else           -> CarOrange
}
```

The arc sweeps 270° starting at 135°, leaving the familiar gap at the bottom of the dial, and animates
between values with `animateFloatAsState` so a jump in the signal reads as motion rather than a snap.

## Gear: an int constant, not a number

`GEAR_SELECTION` is a bit flag. The values are not sequential and cannot be compared as magnitudes:

| Constant | Gear |
|---|---|
| `0x0001` | Neutral |
| `0x0002` | Reverse |
| `0x0004` | Park |
| `0x0008` | Drive |
| `0x0010` … `0x0200` | Manual gears 1–6 |

```kotlin
private fun gearLabel(gear: Int): String = when (gear) {
    0x0001 -> "N (Neutral)"
    0x0002 -> "R (Reverse)"
    0x0004 -> "P (Park)"
    0x0008 -> "D (Drive)"
    0x0010 -> "1"; 0x0020 -> "2"; 0x0040 -> "3"
    0x0080 -> "4"; 0x0100 -> "5"; 0x0200 -> "6"
    else -> "Unknown"
}
```

The selector row is deliberately ordered **P R N D** for the driver, not in numeric order:

```kotlin
val gears = listOf(0x0004 to "P", 0x0002 to "R", 0x0001 to "N", 0x0008 to "D")
```

The raw value is kept in state alongside the label, so the row can highlight by exact match rather than
by parsing a display string.

## Fuel: millilitres are meaningless alone

`FUEL_LEVEL` reports millilitres remaining. `15000` tells a driver nothing without knowing the tank size,
which is why `INFO_FUEL_CAPACITY` is read at connect and cached:

```kotlin
val ml  = num ?: 0f
val pct = if (fuelCapacityMl > 0f) ((ml / fuelCapacityMl) * 100f).roundToInt().coerceIn(0, 100) else -1
val low = if (pct >= 0) pct < 15 else ml < 5000f
```

Three details worth noting:

- `-1` is the sentinel for "capacity unknown". The UI checks `fuelPercent >= 0` and falls back to showing
  raw millilitres without a percentage, instead of printing a fabricated one.
- The low-fuel threshold degrades too — 15 % when capacity is known, an absolute 5000 ml when it is not.
- `coerceIn(0, 100)` guards against a reported level exceeding the reported capacity, which some images do.

The alert itself is the optional "low fuel warning" enhancement: the bar turns `BadRed` and a `LOW FUEL`
badge appears beside the label.

## Battery: only if it is really there

```kotlin
uiState = uiState.copy(batteryAvailable = true, batteryPercent = pct)
```

`batteryAvailable` flips to `true` only when an `EV_BATTERY_LEVEL` event actually arrives. Until then the
panel shows range remaining instead. A combustion vehicle never reports the property, so it never gets a
permanently-empty battery meter.

Battery uses three colour bands rather than the speed gauge's two:

```kotlin
barColor = if (state.batteryPercent < 15) BadRed
           else if (state.batteryPercent < 40) WarnAmber
           else GoodGreen
```

## A caveat worth stating

`EV_BATTERY_LEVEL` is documented in watt-hours, and on many images must be divided by
`INFO_EV_BATTERY_CAPACITY` to become a percentage. On the API 33 automotive image used here it arrives
already as `0..100`, so it is consumed directly and clamped. Code targeting other images should read the
capacity property and divide, exactly as the fuel path does.

---

**Previous:** [04 — Car API walkthrough](04-car-api-walkthrough.md) · **Next:** [06 — What cannot be read](06-what-cannot-be-read.md)
