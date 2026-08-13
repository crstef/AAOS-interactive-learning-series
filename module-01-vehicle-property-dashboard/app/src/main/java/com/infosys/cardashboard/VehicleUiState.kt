package com.infosys.cardashboard

/**
 * An immutable snapshot of everything the dashboard shows.
 *
 * The ViewModel keeps one of these in Compose state; whenever a signal changes
 * it publishes a fresh copy and the UI redraws automatically.
 *
 * Values are kept both as numbers (for gauges/bars) and as ready-to-print
 * strings (for text), so the UI stays simple.
 */
data class VehicleUiState(
    val connected: Boolean = false,

    // Speed
    val speedMph: Int = 0,                 // numeric, for the gauge
    val speedText: String = "--",          // "0"

    // Gear
    val gearRaw: Int = 0,                   // raw VehicleGear value, for the P R N D row
    val gearText: String = "--",           // "P (Park)"

    // Fuel
    val fuelText: String = "--",           // milliliters, e.g. "15000"
    val fuelPercent: Int = -1,             // 0..100, or -1 if capacity unknown
    val lowFuel: Boolean = false,          // true -> fuel card/bar turns red

    // Battery (only shown when the vehicle actually reports it)
    val batteryAvailable: Boolean = false,
    val batteryPercent: Int = 0,           // 0..100

    // Extra live metrics
    val rangeText: String = "--",          // "310 km"
    val odometerText: String = "--",       // "12,345 km"

    // Static info
    val manufacturer: String = "--",
    val model: String = "--",
    val modelYear: String = "--",

    // Rolling history of the last speed readings, for the live sparkline.
    val speedHistory: List<Int> = emptyList()
)
