package com.cristi.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * CALCULATOR UI
 *
 * The car screen is wide (landscape), so we use a 4 rows × 5 columns grid:
 *
 *   ┌───────────────────────────────────────┐
 *   │                              12 +     │  ← expression
 *   │                                 3     │  ← display
 *   ├───────┬───────┬───────┬───────┬───────┤
 *   │   7   │   8   │   9   │   ÷   │   C   │
 *   │   4   │   5   │   6   │   ×   │   ⌫   │
 *   │   1   │   2   │   3   │   −   │   %   │
 *   │   ±   │   0   │   .   │   +   │   =   │
 *   └───────┴───────┴───────┴───────┴───────┘
 *
 * All sizes use `weight` (proportions), so the grid stretches to fit
 * any screen. The buttons end up large (~90dp), above the 76dp minimum
 * Google recommends for car screens.
 */

/** The button type; decides its color. */
private enum class KeyStyle { Number, Operation, Function, Equals }

/** Describes one button: what it shows, which key it sends, which color it has. */
private data class KeySpec(val label: String, val key: CalculatorKey, val style: KeyStyle)

private fun digit(value: Int) = KeySpec(value.toString(), CalculatorKey.Digit(value), KeyStyle.Number)
private fun operation(op: Operation) = KeySpec(op.symbol, CalculatorKey.Op(op), KeyStyle.Operation)

/** The button grid, row by row. To move a button, change only this list. */
private val keypad: List<List<KeySpec>> = listOf(
    listOf(digit(7), digit(8), digit(9), operation(Operation.Divide), KeySpec("C", CalculatorKey.Clear, KeyStyle.Function)),
    listOf(digit(4), digit(5), digit(6), operation(Operation.Multiply), KeySpec("⌫", CalculatorKey.Delete, KeyStyle.Function)),
    listOf(digit(1), digit(2), digit(3), operation(Operation.Subtract), KeySpec("%", CalculatorKey.Percent, KeyStyle.Function)),
    listOf(
        KeySpec("±", CalculatorKey.ToggleSign, KeyStyle.Function),
        digit(0),
        KeySpec(".", CalculatorKey.Decimal, KeyStyle.Number),
        operation(Operation.Add),
        KeySpec("=", CalculatorKey.Equals, KeyStyle.Equals),
    ),
)

/**
 * The whole screen.
 * It is "stateless": it holds no state. It receives the state (`state`) and
 * reports presses (`onKey`). Whoever uses it (MainActivity) decides what happens.
 */
@Composable
fun CalculatorScreen(
    state: CalculatorState,
    onKey: (CalculatorKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The display takes 1.4 "parts" of the height, each button row takes 1.
        Display(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.4f),
        )

        keypad.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { spec ->
                    CalculatorButton(
                        spec = spec,
                        onClick = { onKey(spec.key) },
                        // Each button takes an equal share of the row's width.
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/** The top area: the expression (small, grey) and the current number (large). Right-aligned. */
@Composable
private fun Display(state: CalculatorState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = state.expression,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = state.display,
            // Long numbers get a smaller font, so they fit.
            fontSize = if (state.display.length > 10) 44.sp else 60.sp,
            fontWeight = FontWeight.Light,
            color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** One button. Its color comes from the theme (MaterialTheme), based on the button type. */
@Composable
private fun CalculatorButton(spec: KeySpec, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = when (spec.style) {
        KeyStyle.Number -> ButtonDefaults.buttonColors(
            containerColor = scheme.surfaceVariant,
            contentColor = scheme.onSurfaceVariant,
        )
        KeyStyle.Operation -> ButtonDefaults.buttonColors(
            containerColor = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer,
        )
        KeyStyle.Function -> ButtonDefaults.buttonColors(
            containerColor = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer,
        )
        KeyStyle.Equals -> ButtonDefaults.buttonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = colors,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(text = spec.label, fontSize = 32.sp, fontWeight = FontWeight.Medium)
    }
}
