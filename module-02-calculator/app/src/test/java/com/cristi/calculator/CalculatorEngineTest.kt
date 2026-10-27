package com.cristi.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the calculator logic.
 * They run on the PC, without an emulator:  gradlew.bat testDebugUnitTest
 *
 * Each test "types" a sequence of keys and checks what appears on screen.
 */
class CalculatorEngineTest {

    /**
     * Helper: turns a string into key presses, so the tests are easy to write.
     * Digits and . + - * / = as on a keyboard; C = clear, < = delete, % = percent, ~ = ±
     */
    private fun type(input: String): CalculatorState =
        input.fold(CalculatorState()) { state, char ->
            val key = when (char) {
                in '0'..'9' -> CalculatorKey.Digit(char - '0')
                '.' -> CalculatorKey.Decimal
                '+' -> CalculatorKey.Op(Operation.Add)
                '-' -> CalculatorKey.Op(Operation.Subtract)
                '*' -> CalculatorKey.Op(Operation.Multiply)
                '/' -> CalculatorKey.Op(Operation.Divide)
                '=' -> CalculatorKey.Equals
                'C' -> CalculatorKey.Clear
                '<' -> CalculatorKey.Delete
                '%' -> CalculatorKey.Percent
                '~' -> CalculatorKey.ToggleSign
                else -> error("Unknown character: $char")
            }
            CalculatorEngine.press(state, key)
        }

    @Test fun `addition`() = assertEquals("5", type("2+3=").display)
    @Test fun `subtraction with negative result`() = assertEquals("-3", type("9-12=").display)
    @Test fun `multiplication`() = assertEquals("36", type("12*3=").display)
    @Test fun `round result without scientific notation`() = assertEquals("100", type("25*4=").display)
    @Test fun `division with decimals`() = assertEquals("3.5", type("7/2=").display)
    @Test fun `repeating division rounded to 12 digits`() = assertEquals("0.333333333333", type("1/3=").display)
    @Test fun `no floating point errors`() = assertEquals("0.3", type("0.1+0.2=").display)

    @Test fun `division by zero gives an error`() {
        val state = type("5/0=")
        assertEquals(CalculatorEngine.ERROR_TEXT, state.display)
        assertTrue(state.isError)
    }

    @Test fun `C resets after an error`() = assertEquals("0", type("5/0=C").display)
    @Test fun `a digit after an error starts a new number`() = assertEquals("7", type("5/0=7").display)

    @Test fun `chained calculation`() {
        val state = type("2+3*")
        assertEquals("5", state.display)
        assertEquals("5 ×", state.expression)
        assertEquals("20", type("2+3*4=").display)
    }

    @Test fun `expression after equals`() = assertEquals("12 + 3 =", type("12+3=").expression)
    @Test fun `leading zeros are dropped`() = assertEquals("7", type("007").display)
    @Test fun `only one decimal point`() = assertEquals("1.5", type("1..5").display)
    @Test fun `point at the start becomes zero point`() = assertEquals("0.5", type(".5").display)
    @Test fun `delete removes the last digit`() = assertEquals("12", type("123<").display)
    @Test fun `delete on the last digit leaves zero`() = assertEquals("0", type("5<").display)
    @Test fun `percent`() = assertEquals("0.5", type("50%").display)
    @Test fun `toggle sign`() = assertEquals("-8", type("8~").display)
    @Test fun `toggle sign twice`() = assertEquals("8", type("8~~").display)
    @Test fun `a digit after a result starts a new number`() = assertEquals("7", type("2+3=7").display)
    @Test fun `the result can continue the calculation`() = assertEquals("10", type("2+3=*2=").display)
    @Test fun `changing the operator`() = assertEquals("6", type("2+*3=").display)
}
