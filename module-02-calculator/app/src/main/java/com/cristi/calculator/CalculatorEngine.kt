package com.cristi.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/*
 * CALCULATOR LOGIC
 *
 * This file knows NOTHING about Android or Compose: it is pure Kotlin.
 * That's why we can test it with unit tests, without an emulator
 * (see app/src/test/.../CalculatorEngineTest.kt).
 *
 * The idea: the calculator's state is a "snapshot" (CalculatorState).
 * When you press a key, CalculatorEngine.press(oldState, key)
 * returns a NEW snapshot. The old state is never modified.
 */

/** The four operations. `symbol` is the text shown on the button and on screen. */
enum class Operation(val symbol: String) {
    Add("+"),
    Subtract("−"),
    Multiply("×"),
    Divide("÷"),
}

/**
 * Every possible key.
 * `sealed` = the list is closed: the compiler knows every variant and forces us
 * to handle all of them in `when` (if we forget one, it doesn't compile).
 */
sealed interface CalculatorKey {
    data class Digit(val value: Int) : CalculatorKey          // 0..9
    data class Op(val operation: Operation) : CalculatorKey   // + − × ÷
    data object Decimal : CalculatorKey                       // .
    data object Equals : CalculatorKey                        // =
    data object Clear : CalculatorKey                         // C  (clear everything)
    data object Delete : CalculatorKey                        // ⌫  (delete the last digit)
    data object ToggleSign : CalculatorKey                    // ±
    data object Percent : CalculatorKey                       // %
}

/**
 * Everything the calculator needs to know at a given moment.
 * `data class` = Kotlin generates equals(), toString() and copy() automatically.
 * `val` everywhere = immutable; to get a new state we use copy(...).
 */
data class CalculatorState(
    /** The big number on screen (what you type or the result). Kept as text to preserve exactly what was typed, e.g. "0." */
    val display: String = "0",
    /** The small line above, e.g. "12 +" or "12 + 3 =". */
    val expression: String = "",
    /** The first number, stored when you press an operator. */
    val firstOperand: BigDecimal? = null,
    /** The operation waiting for the second number. */
    val pendingOperation: Operation? = null,
    /** true = the next digit starts a new number (replaces the display instead of being appended). */
    val startNewNumber: Boolean = true,
    /** true after an error (e.g. division by zero). */
    val isError: Boolean = false,
)

object CalculatorEngine {

    const val ERROR_TEXT = "Error"

    /** How many digits the user can type in one number. */
    private const val MAX_DIGITS = 12

    /**
     * Precision of the results: 12 significant digits.
     * We use BigDecimal (not Double) to avoid errors like
     * 0.1 + 0.2 = 0.30000000000000004.
     */
    private val PRECISION = MathContext(12, RoundingMode.HALF_UP)

    /** The single entry point: old state + key → new state. */
    fun press(state: CalculatorState, key: CalculatorKey): CalculatorState {
        // After an error, only keys that start something new are accepted (C, digit, point).
        if (state.isError) {
            val startsFresh = key is CalculatorKey.Digit ||
                key == CalculatorKey.Decimal ||
                key == CalculatorKey.Clear
            if (!startsFresh) return state
            return press(CalculatorState(), key)
        }

        return when (key) {
            is CalculatorKey.Digit -> inputDigit(state, key.value)
            is CalculatorKey.Op -> chooseOperation(state, key.operation)
            CalculatorKey.Decimal -> inputDecimal(state)
            CalculatorKey.Equals -> equals(state)
            CalculatorKey.Clear -> CalculatorState()
            CalculatorKey.Delete -> delete(state)
            CalculatorKey.ToggleSign -> toggleSign(state)
            CalculatorKey.Percent -> percent(state)
        }
    }

    // ----------------------------------------------------------------- keys

    private fun inputDigit(state: CalculatorState, value: Int): CalculatorState {
        val digit = value.toString()
        // New number or "0" on screen → the digit replaces it (avoids "007").
        if (state.startNewNumber || state.display == "0") {
            return state.copy(display = digit, startNewNumber = false)
        }
        if (state.display == "-0") {
            return state.copy(display = "-$digit")
        }
        // Length limit, so the number fits on screen.
        if (state.display.count { it.isDigit() } >= MAX_DIGITS) return state
        return state.copy(display = state.display + digit)
    }

    private fun inputDecimal(state: CalculatorState): CalculatorState {
        if (state.startNewNumber) {
            return state.copy(display = "0.", startNewNumber = false)
        }
        if ('.' in state.display) return state      // only one point per number
        return state.copy(display = state.display + ".")
    }

    private fun chooseOperation(state: CalculatorState, operation: Operation): CalculatorState {
        val first = state.firstOperand
        val pending = state.pendingOperation

        // Chained calculation: "2 + 3 ×" → first compute 2 + 3 = 5, then 5 × ...
        if (first != null && pending != null && !state.startNewNumber) {
            val result = calculate(first, state.display.toBigDecimal(), pending) ?: return errorState()
            return state.copy(
                display = format(result),
                expression = "${format(result)} ${operation.symbol}",
                firstOperand = result,
                pendingOperation = operation,
                startNewNumber = true,
            )
        }

        // Normal case (or changing the operator, e.g. "+" then "×"):
        // store the number on screen and the operation, then wait for the second number.
        val current = state.display.toBigDecimal()
        return state.copy(
            expression = "${format(current)} ${operation.symbol}",
            firstOperand = current,
            pendingOperation = operation,
            startNewNumber = true,
        )
    }

    private fun equals(state: CalculatorState): CalculatorState {
        // With no pending operation, "=" has nothing to do.
        val first = state.firstOperand ?: return state
        val operation = state.pendingOperation ?: return state
        val second = state.display.toBigDecimal()

        val result = calculate(first, second, operation) ?: return errorState()
        return CalculatorState(
            display = format(result),
            expression = "${format(first)} ${operation.symbol} ${format(second)} =",
            startNewNumber = true,
        )
    }

    private fun delete(state: CalculatorState): CalculatorState {
        // Don't delete from results or the stored number, only from what is being typed.
        if (state.startNewNumber) return state
        val shorter = state.display.dropLast(1)
        val newDisplay = if (shorter.isEmpty() || shorter == "-") "0" else shorter
        return state.copy(display = newDisplay)
    }

    private fun toggleSign(state: CalculatorState): CalculatorState {
        if (state.display.toBigDecimal().signum() == 0) return state   // no "-0"
        val newDisplay = if (state.display.startsWith("-")) {
            state.display.drop(1)
        } else {
            "-" + state.display
        }
        return state.copy(display = newDisplay)
    }

    private fun percent(state: CalculatorState): CalculatorState {
        val value = state.display.toBigDecimal().divide(BigDecimal(100))
        return state.copy(display = format(value))
    }

    // -------------------------------------------------------------- helpers

    /** The actual calculation. Returns null on division by zero. */
    private fun calculate(first: BigDecimal, second: BigDecimal, operation: Operation): BigDecimal? =
        when (operation) {
            Operation.Add -> first.add(second, PRECISION)
            Operation.Subtract -> first.subtract(second, PRECISION)
            Operation.Multiply -> first.multiply(second, PRECISION)
            Operation.Divide ->
                if (second.signum() == 0) null else first.divide(second, PRECISION)
        }

    /**
     * BigDecimal → text for the screen.
     * stripTrailingZeros: 3.500 → 3.5 ; toPlainString: 1E+2 → 100.
     * Huge numbers stay in scientific notation (1.5E+20) so they fit.
     */
    fun format(value: BigDecimal): String {
        val rounded = value.round(PRECISION).stripTrailingZeros()
        val plain = rounded.toPlainString()
        return if (plain.count { it.isDigit() } <= 15) plain else rounded.toString()
    }

    private fun errorState() = CalculatorState(display = ERROR_TEXT, isError = true)
}
