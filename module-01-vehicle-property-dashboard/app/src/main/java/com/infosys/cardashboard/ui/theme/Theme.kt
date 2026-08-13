package com.infosys.cardashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = CarOrange, background = NightBg, surface = NightSurface,
    onBackground = NightText, onSurface = NightText
)

private val LightColors = lightColorScheme(
    primary = CarOrange, background = DayBg, surface = DaySurface,
    onBackground = DayText, onSurface = DayText
)

@Composable
fun AutomotiveCarPropAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
