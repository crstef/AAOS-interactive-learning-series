# 04 — Car API walkthrough

> Connecting, subscribing, and the permission model — with the code that actually runs.

## Connecting

Three lines get you from an `Application` context to a live vehicle-property interface:

```kotlin
car = Car.createCar(getApplication())
cpm = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
if (cpm == null) { Log.e(TAG, "CarPropertyManager unavailable"); return }
```

The `as?` is doing real work. On a non-automotive image `getCarManager` returns null, and a hard cast
would throw before any of the error handling had a chance to run. Here the app logs the cause and stays
alive showing `--`.

The whole `connect()` body is wrapped in a `try`, and guarded against double-connecting:

```kotlin
fun connect() {
    if (uiState.connected) return
    try {
        ...
        uiState = uiState.copy(connected = true)
    } catch (e: Exception) {
        Log.e(TAG, "connect() failed", e)
    }
}
```

`connected` is what drives the green "● Live" dot in the top bar — it reflects the real service
connection, not an assumption.

## Subscribing

```kotlin
for (p in liveProps) {
    try {
        cpm?.registerCallback(callback, p, CarPropertyManager.SENSOR_RATE_ONCHANGE)
    } catch (e: Exception) { Log.w(TAG, "register failed for $p", e) }
}
```

`SENSOR_RATE_ONCHANGE` is the right rate for a dashboard: the platform pushes a value when it changes,
rather than at a fixed frequency. Nothing polls, and an idle vehicle costs nothing.

Each property is registered in its own `try` so that one unsupported ID cannot abort the loop and cost
you the remaining five.

## Receiving

A single callback serves every property; the dispatch is on `value.propertyId`:

```kotlin
private val callback = object : CarPropertyManager.CarPropertyEventCallback {
    override fun onChangeEvent(value: CarPropertyValue<*>) {
        val num = asFloat(value.value)
        when (value.propertyId) {

            VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                val mph = ((num ?: 0f) * 2.23694f).roundToInt()
                val history = (uiState.speedHistory + mph).takeLast(40)
                uiState = uiState.copy(speedMph = mph, speedText = mph.toString(), speedHistory = history)
            }

            VehiclePropertyIds.GEAR_SELECTION -> {
                val g = (value.value as? Int) ?: 0
                uiState = uiState.copy(gearRaw = g, gearText = gearLabel(g))
            }
            ...
        }
    }

    override fun onErrorEvent(propId: Int, areaId: Int) {
        Log.w(TAG, "error event for $propId")
    }
}
```

`CarPropertyValue<*>` is generic and its `value` arrives boxed, so it is normalised before use:

```kotlin
private fun asFloat(v: Any?): Float? = when (v) {
    is Float  -> v
    is Int    -> v.toFloat()
    is Number -> v.toFloat()
    else      -> null
}
```

The speed history line is the entire implementation of the optional "historical trends" enhancement — an
append and a `takeLast(40)`, bounded so it cannot grow without limit, feeding the sparkline in
`DashboardScreen`.

## Permissions are a two-step process

**This is the single most common way to lose an afternoon.** Declaring a car permission in the manifest
is necessary but not sufficient.

Declared in [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml):

```xml
<uses-permission android:name="android.car.permission.CAR_INFO" />
<uses-permission android:name="android.car.permission.CAR_SPEED" />
<uses-permission android:name="android.car.permission.CAR_POWERTRAIN" />
<uses-permission android:name="android.car.permission.CAR_ENERGY" />
<uses-permission android:name="android.car.permission.CAR_MILEAGE" />
```

`CAR_INFO` is a normal permission — granted at install, nothing more to do. The other four are
**dangerous** permissions and must *also* be requested at runtime, or every read throws
`SecurityException`.

So [`MainActivity.kt`](../app/src/main/java/com/infosys/cardashboard/MainActivity.kt) requests them and
only connects once the user has answered:

```kotlin
private val requiredPermissions = arrayOf(
    "android.car.permission.CAR_SPEED",
    "android.car.permission.CAR_POWERTRAIN",
    "android.car.permission.CAR_ENERGY",
    "android.car.permission.CAR_MILEAGE"
)

private val permissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.connect()
    }

override fun onCreate(savedInstanceState: Bundle?) {
    ...
    if (hasAllPermissions()) viewModel.connect()
    else permissionLauncher.launch(requiredPermissions)
}
```

Note that `connect()` is called from the result callback regardless of the answer. A partial grant should
still produce a partly-working dashboard rather than nothing at all — the properties that were granted
work, the rest show `--`.

Which permission unlocks which property:

| Permission | Properties |
|---|---|
| `CAR_INFO` | `INFO_MAKE`, `INFO_MODEL`, `INFO_MODEL_YEAR`, `INFO_FUEL_CAPACITY` |
| `CAR_SPEED` | `PERF_VEHICLE_SPEED` |
| `CAR_POWERTRAIN` | `GEAR_SELECTION` |
| `CAR_ENERGY` | `FUEL_LEVEL`, `EV_BATTERY_LEVEL`, `RANGE_REMAINING` |
| `CAR_MILEAGE` | `PERF_ODOMETER` |

## Driving safety

An AAOS app that does not declare itself distraction-optimised is blocked while the vehicle is moving —
the user gets "Can't use this app while driving" exactly when a dashboard is most useful. This is a
read-only display with no interaction required, so it qualifies:

```xml
<meta-data android:name="distractionOptimized" android:value="true" />
```

## Disconnecting

```kotlin
fun disconnect() {
    try { cpm?.unregisterCallback(callback) } catch (e: Exception) { Log.w(TAG, "unregister", e) }
    car?.disconnect(); car = null; cpm = null
    uiState = uiState.copy(connected = false)
}
```

Called from `onCleared()`. Unregister before disconnecting, and null the references so a stale manager
cannot be used after the connection is gone.

---

**Previous:** [03 — Architecture](03-architecture.md) · **Next:** [05 — Properties and units](05-properties-and-units.md)
