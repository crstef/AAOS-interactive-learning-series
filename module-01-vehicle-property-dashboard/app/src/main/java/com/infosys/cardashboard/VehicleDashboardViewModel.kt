package com.infosys.cardashboard

import android.app.Application
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlin.math.roundToInt

/**
 * The "brain" of the app.
 *
 * Connects to the car through the AAOS Car API, reads vehicle properties with
 * [CarPropertyManager], and exposes them to the UI as [uiState].
 *
 * Static values (make / model / year / capacities) are read once on connect.
 * Live values (speed / gear / fuel / battery / range / odometer) arrive through
 * a callback, so the dashboard updates by itself whenever a signal changes.
 */
class VehicleDashboardViewModel(app: Application) : AndroidViewModel(app) {

    var uiState by mutableStateOf(VehicleUiState())
        private set

    private var car: Car? = null
    private var cpm: CarPropertyManager? = null

    private val globalArea = 0
    private var fuelCapacityMl = 0f

    // Live properties we subscribe to. Anything the current emulator image does
    // not support simply never fires and shows "--" (handled gracefully).
    private val liveProps = intArrayOf(
        VehiclePropertyIds.PERF_VEHICLE_SPEED,
        VehiclePropertyIds.GEAR_SELECTION,
        VehiclePropertyIds.FUEL_LEVEL,
        VehiclePropertyIds.EV_BATTERY_LEVEL,
        VehiclePropertyIds.RANGE_REMAINING,
        VehiclePropertyIds.PERF_ODOMETER
    )

    fun connect() {
        if (uiState.connected) return
        try {
            car = Car.createCar(getApplication())
            cpm = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            if (cpm == null) { Log.e(TAG, "CarPropertyManager unavailable"); return }

            readStaticInfo()
            for (p in liveProps) {
                try {
                    cpm?.registerCallback(callback, p, CarPropertyManager.SENSOR_RATE_ONCHANGE)
                } catch (e: Exception) { Log.w(TAG, "register failed for $p", e) }
            }
            uiState = uiState.copy(connected = true)
            Log.i(TAG, "Connected and listening")
        } catch (e: Exception) {
            Log.e(TAG, "connect() failed", e)
        }
    }

    private fun readStaticInfo() {
        val mgr = cpm ?: return
        fuelCapacityMl = readNumber(mgr, VehiclePropertyIds.INFO_FUEL_CAPACITY) ?: 0f
        uiState = uiState.copy(
            manufacturer = readText(mgr, VehiclePropertyIds.INFO_MAKE) ?: "--",
            model        = readText(mgr, VehiclePropertyIds.INFO_MODEL) ?: "--",
            modelYear    = (readNumber(mgr, VehiclePropertyIds.INFO_MODEL_YEAR)?.roundToInt())?.toString() ?: "--"
        )
    }

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

                VehiclePropertyIds.FUEL_LEVEL -> {
                    val ml = num ?: 0f
                    val pct = if (fuelCapacityMl > 0f) ((ml / fuelCapacityMl) * 100f).roundToInt().coerceIn(0, 100) else -1
                    val low = if (pct >= 0) pct < 15 else ml < 5000f
                    uiState = uiState.copy(fuelText = ml.roundToInt().toString(), fuelPercent = pct, lowFuel = low)
                }

                VehiclePropertyIds.EV_BATTERY_LEVEL -> {
                    // Reported as a percentage (0..100) on modern images.
                    val pct = (num ?: 0f).roundToInt().coerceIn(0, 100)
                    uiState = uiState.copy(batteryAvailable = true, batteryPercent = pct)
                }

                VehiclePropertyIds.RANGE_REMAINING -> {
                    val meters = num ?: 0f
                    uiState = uiState.copy(rangeText = "${(meters / 1000f).roundToInt()} km")
                }

                VehiclePropertyIds.PERF_ODOMETER -> {
                    val km = (num ?: 0f).roundToInt()
                    uiState = uiState.copy(odometerText = "%,d km".format(km))
                }
            }
        }

        override fun onErrorEvent(propId: Int, areaId: Int) {
            Log.w(TAG, "error event for $propId")
        }
    }

    fun disconnect() {
        try { cpm?.unregisterCallback(callback) } catch (e: Exception) { Log.w(TAG, "unregister", e) }
        car?.disconnect(); car = null; cpm = null
        uiState = uiState.copy(connected = false)
    }

    override fun onCleared() { super.onCleared(); disconnect() }

    // ---- safe readers ---------------------------------------------------------

    /** Reads a numeric property, tolerating Float OR Integer typed images. */
    private fun readNumber(mgr: CarPropertyManager, propId: Int): Float? = try {
        mgr.getFloatProperty(propId, globalArea)
    } catch (e: Exception) {
        try { mgr.getIntProperty(propId, globalArea).toFloat() } catch (e2: Exception) { null }
    }

    private fun readText(mgr: CarPropertyManager, propId: Int): String? = try {
        mgr.getProperty(String::class.java, propId, globalArea)?.value
    } catch (e: Exception) { null }

    /** Turns a boxed property value (Float or Integer) into a Float. */
    private fun asFloat(v: Any?): Float? = when (v) {
        is Float -> v
        is Int -> v.toFloat()
        is Number -> v.toFloat()
        else -> null
    }

    private fun gearLabel(gear: Int): String = when (gear) {
        0x0001 -> "N (Neutral)"
        0x0002 -> "R (Reverse)"
        0x0004 -> "P (Park)"
        0x0008 -> "D (Drive)"
        0x0010 -> "1"; 0x0020 -> "2"; 0x0040 -> "3"
        0x0080 -> "4"; 0x0100 -> "5"; 0x0200 -> "6"
        else -> "Unknown"
    }

    companion object { private const val TAG = "CarDashboard" }
}
