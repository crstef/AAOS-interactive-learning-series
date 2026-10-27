package com.cristi.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * The link between the UI (CalculatorScreen) and the logic (CalculatorEngine).
 *
 * Why a ViewModel and not a variable directly in the screen?
 * The ViewModel survives when the Activity is recreated (e.g. screen rotation,
 * switching between day and night theme in the car). Without it, the calculation
 * in progress would be lost.
 */
class CalculatorViewModel : ViewModel() {

    /**
     * The current state.
     * `mutableStateOf` = a value Compose "observes": when it changes,
     * the screen that reads it redraws automatically.
     * `private set` = only the ViewModel can change it; the screen only reads it.
     */
    var state by mutableStateOf(CalculatorState())
        private set

    /** Called by the screen on every button press. */
    fun onKey(key: CalculatorKey) {
        state = CalculatorEngine.press(state, key)
    }
}
