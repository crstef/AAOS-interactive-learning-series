# 03 — Architecture

> How one property callback becomes a redrawn screen.

## The files

Five Kotlin sources, plus three theme files.

| File | Responsibility |
|---|---|
| [`MainActivity.kt`](../app/src/main/java/com/infosys/cardashboard/MainActivity.kt) | Requests the runtime permissions, then tells the ViewModel to connect. Hosts the Compose tree and owns the day/night flag. |
| [`VehicleDashboardViewModel.kt`](../app/src/main/java/com/infosys/cardashboard/VehicleDashboardViewModel.kt) | The only file that imports `android.car`. Connects to the Car service, subscribes to properties, converts units, publishes state. |
| [`VehicleUiState.kt`](../app/src/main/java/com/infosys/cardashboard/VehicleUiState.kt) | An immutable snapshot of everything the dashboard shows. |
| [`ui/DashboardScreen.kt`](../app/src/main/java/com/infosys/cardashboard/ui/DashboardScreen.kt) | Renders the state — gauge, sparkline, gear row, meter bars, info cards. Contains no vehicle logic. |
| [`ui/theme/`](../app/src/main/java/com/infosys/cardashboard/ui/theme/) | `Color.kt`, `Theme.kt`, `Type.kt` — the day and night colour schemes. |

The boundary that matters: **`DashboardScreen` has no knowledge of the Car API.** It receives a
`VehicleUiState` and draws it. That is what makes the `@Preview` at the bottom of the file work — the
whole dashboard renders in Android Studio, with no emulator, from a hand-written state object.

## The data path

```
Vehicle HAL  (emulator's simulated vehicle)
      │
      ▼
Car service  (android.car system service)
      │  Car.createCar(context)
      ▼
CarPropertyManager
      │  registerCallback(cb, propId, SENSOR_RATE_ONCHANGE)
      ▼
CarPropertyEventCallback.onChangeEvent(CarPropertyValue)
      │  unit conversion, gear mapping, history append
      ▼
uiState = uiState.copy(...)          ← a Compose MutableState
      │  assignment marks the state dirty
      ▼
Compose recomposition               ← automatic; nothing calls "update the UI"
      │
      ▼
DashboardScreen redraws only what changed
```

## State handling

State is held in a single Compose `MutableState`, exposed read-only:

```kotlin
var uiState by mutableStateOf(VehicleUiState())
    private set
```

`VehicleUiState` is a `data class`, so every update is a `copy()` that replaces the whole snapshot:

```kotlin
uiState = uiState.copy(speedMph = mph, speedText = mph.toString(), speedHistory = history)
```

Because `DashboardScreen` reads `viewModel.uiState` during composition, Compose records the dependency
and re-runs exactly the composables that read the changed values. There is no observer wiring, no
`invalidate()`, no `findViewById` — the update path from a vehicle signal to a redrawn pixel is a single
assignment.

This is the "real-time data binding" the brief lists as an expected outcome, and it is why the screenshots
change while nobody is touching the app.

## Static versus live properties

Two categories, handled differently:

**Static** — read once, at connect time, with a direct `get`:

```kotlin
private fun readStaticInfo() {
    val mgr = cpm ?: return
    fuelCapacityMl = readNumber(mgr, VehiclePropertyIds.INFO_FUEL_CAPACITY) ?: 0f
    uiState = uiState.copy(
        manufacturer = readText(mgr, VehiclePropertyIds.INFO_MAKE) ?: "--",
        ...
    )
}
```

`INFO_FUEL_CAPACITY` is read here and kept, because the fuel percentage cannot be computed without it —
`FUEL_LEVEL` arrives in millilitres, which means nothing on its own.

**Live** — subscribed once, then pushed by the platform:

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

## Degrading gracefully

Not every image supports every property. Three layers keep that from becoming a crash:

1. **Registration is individually guarded.** One unsupported property does not prevent the others from
   subscribing:

   ```kotlin
   for (p in liveProps) {
       try { cpm?.registerCallback(callback, p, CarPropertyManager.SENSOR_RATE_ONCHANGE) }
       catch (e: Exception) { Log.w(TAG, "register failed for $p", e) }
   }
   ```

2. **Defaults are `"--"`, not `0`.** Every text field in `VehicleUiState` starts as `"--"`, so a property
   that never arrives is visibly absent rather than a plausible-looking zero.

3. **Reads tolerate either numeric type.** Different images type the same property as `Float` or `Integer`;
   `readNumber()` tries float first and falls back:

   ```kotlin
   private fun readNumber(mgr: CarPropertyManager, propId: Int): Float? = try {
       mgr.getFloatProperty(propId, globalArea)
   } catch (e: Exception) {
       try { mgr.getIntProperty(propId, globalArea).toFloat() } catch (e2: Exception) { null }
   }
   ```

The UI reflects this too — the battery meter is rendered only once a battery reading has actually
arrived, and its slot shows range remaining until then:

```kotlin
if (state.batteryAvailable) { MeterBar(...) } else { StatContent(Icons.Filled.Route, "Range remaining", ...) }
```

## Cleanup

The callback and the car connection are released when the ViewModel is cleared, so a rotation or an exit
does not leave a registered callback pointing at a dead object:

```kotlin
override fun onCleared() { super.onCleared(); disconnect() }
```

---

**Previous:** [02 — Emulator and AVD](02-emulator-and-avd.md) · **Next:** [04 — Car API walkthrough](04-car-api-walkthrough.md)
