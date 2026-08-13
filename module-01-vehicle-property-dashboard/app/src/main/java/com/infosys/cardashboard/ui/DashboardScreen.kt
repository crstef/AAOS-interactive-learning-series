package com.infosys.cardashboard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infosys.cardashboard.VehicleUiState
import com.infosys.cardashboard.ui.theme.AutomotiveCarPropAppTheme
import com.infosys.cardashboard.ui.theme.BadRed
import com.infosys.cardashboard.ui.theme.CarOrange
import com.infosys.cardashboard.ui.theme.DayBg
import com.infosys.cardashboard.ui.theme.DayMuted
import com.infosys.cardashboard.ui.theme.DaySurface
import com.infosys.cardashboard.ui.theme.DayText
import com.infosys.cardashboard.ui.theme.GoodGreen
import com.infosys.cardashboard.ui.theme.NightBg
import com.infosys.cardashboard.ui.theme.NightMuted
import com.infosys.cardashboard.ui.theme.NightSurface
import com.infosys.cardashboard.ui.theme.NightText
import com.infosys.cardashboard.ui.theme.WarnAmber

/** A small palette that flips with the day/night toggle. */
private data class Pal(val bg: Color, val surface: Color, val text: Color, val muted: Color, val accent: Color = CarOrange)

private fun palette(night: Boolean) =
    if (night) Pal(NightBg, NightSurface, NightText, NightMuted)
    else Pal(DayBg, DaySurface, DayText, DayMuted)

@Composable
fun DashboardScreen(state: VehicleUiState, night: Boolean, onToggleTheme: () -> Unit) {
    val pal = palette(night)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pal.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TopBar(state.connected, night, onToggleTheme, pal)

        // ---- Hero: speed gauge + gear/trend ---------------------------------
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel(Modifier.weight(1f).fillMaxHeight(), pal) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("SPEED", color = pal.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    SpeedGauge(state.speedMph, 160, pal)
                }
            }
            Panel(Modifier.weight(1.25f).fillMaxHeight(), pal) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabelRow(Icons.Filled.DirectionsCar, "Gear", pal)
                    GearSelector(state.gearRaw, pal)
                    Spacer(Modifier.height(2.dp))
                    LabelRow(Icons.Filled.Speed, "Speed trend (live)", pal)
                    Sparkline(state.speedHistory, pal, Modifier.fillMaxWidth().height(70.dp))
                }
            }
        }

        // ---- Bars: fuel + (battery OR range) --------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel(Modifier.weight(1f), pal) {
                MeterBar(
                    icon = Icons.Filled.LocalGasStation,
                    label = "Fuel Level",
                    valueText = if (state.fuelPercent >= 0) "${state.fuelText} ml  (${state.fuelPercent}%)" else "${state.fuelText} ml",
                    fraction = if (state.fuelPercent >= 0) state.fuelPercent / 100f else 0f,
                    barColor = if (state.lowFuel) BadRed else GoodGreen,
                    pal = pal,
                    warn = state.lowFuel,
                    warnText = "LOW FUEL"
                )
            }
            Panel(Modifier.weight(1f), pal) {
                if (state.batteryAvailable) {
                    MeterBar(
                        icon = Icons.Filled.EvStation,
                        label = "Battery",
                        valueText = "${state.batteryPercent}%",
                        fraction = state.batteryPercent / 100f,
                        barColor = if (state.batteryPercent < 15) BadRed else if (state.batteryPercent < 40) WarnAmber else GoodGreen,
                        pal = pal
                    )
                } else {
                    StatContent(Icons.Filled.Route, "Range remaining", state.rangeText, pal)
                }
            }
        }

        // ---- Vehicle info ----------------------------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InfoCard(Icons.Filled.Factory, "Manufacturer", state.manufacturer, pal, Modifier.weight(1f))
            InfoCard(Icons.Filled.DirectionsCar, "Car Model", state.model, pal, Modifier.weight(1f))
            InfoCard(Icons.Filled.CalendarMonth, "Model Year", state.modelYear, pal, Modifier.weight(1f))
        }

        // ---- Trip stats ------------------------------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (state.batteryAvailable) {
                InfoCard(Icons.Filled.Route, "Range", state.rangeText, pal, Modifier.weight(1f))
            }
            InfoCard(Icons.Filled.Straighten, "Odometer", state.odometerText, pal, Modifier.weight(1f))
        }
    }
}

// ---------------------------------------------------------------------------
//  Building blocks
// ---------------------------------------------------------------------------

@Composable
private fun TopBar(connected: Boolean, night: Boolean, onToggle: () -> Unit, pal: Pal) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).background(CarOrange, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(10.dp))
        Text("Vehicle Dashboard", color = pal.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        // Live indicator
        Box(Modifier.size(9.dp).background(if (connected) GoodGreen else pal.muted, CircleShape))
        Spacer(Modifier.size(6.dp))
        Text(if (connected) "Live" else "Connecting…", color = if (connected) GoodGreen else pal.muted, fontSize = 14.sp)
        Spacer(Modifier.size(10.dp))
        // Day/night toggle — a standard IconButton (large, reliable tap target).
        Box(Modifier.size(48.dp).background(pal.surface, CircleShape), contentAlignment = Alignment.Center) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (night) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle day or night theme",
                    tint = CarOrange
                )
            }
        }
    }
}

@Composable
private fun SpeedGauge(speed: Int, max: Int, pal: Pal) {
    val target = (speed.toFloat() / max).coerceIn(0f, 1f)
    val frac by animateFloatAsState(targetValue = target, label = "speed")
    val arcColor = when {
        target > 0.8f -> BadRed
        target > 0.55f -> WarnAmber
        else -> CarOrange
    }
    val track = pal.muted.copy(alpha = 0.25f)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
            val tl = Offset(stroke / 2, stroke / 2)
            val sz = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 135f, 270f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(arcColor, 135f, 270f * frac, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$speed", color = pal.text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Text("MPH", color = CarOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GearSelector(gearRaw: Int, pal: Pal) {
    val gears = listOf(0x0004 to "P", 0x0002 to "R", 0x0001 to "N", 0x0008 to "D")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        gears.forEach { (code, label) ->
            val active = gearRaw == code
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(if (active) CarOrange else pal.bg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (active) Color.White else pal.muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Sparkline(points: List<Int>, pal: Pal, modifier: Modifier) {
    Box(modifier.background(pal.bg, RoundedCornerShape(12.dp)).padding(8.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            if (points.size < 2) return@Canvas
            val maxV = (points.maxOrNull() ?: 1).coerceAtLeast(10).toFloat()
            val stepX = size.width / (points.size - 1)
            val path = Path()
            points.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - (v / maxV) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, CarOrange, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun MeterBar(
    icon: ImageVector, label: String, valueText: String, fraction: Float, barColor: Color, pal: Pal,
    warn: Boolean = false, warnText: String = ""
) {
    val frac by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "bar")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, pal)
            Spacer(Modifier.size(10.dp))
            Text(label, color = pal.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            if (warn) {
                Box(Modifier.background(BadRed, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(warnText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // track + fill
        Box(Modifier.fillMaxWidth().height(16.dp).background(pal.muted.copy(alpha = 0.2f), RoundedCornerShape(8.dp))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(barColor, RoundedCornerShape(8.dp)))
        }
        Text(valueText, color = pal.muted, fontSize = 14.sp)
    }
}

@Composable
private fun StatContent(icon: ImageVector, label: String, value: String, pal: Pal) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelRow(icon, label, pal)
        Text(value, color = pal.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(icon: ImageVector, label: String, value: String, pal: Pal, modifier: Modifier) {
    Panel(modifier, pal) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            IconBadge(icon, pal)
            Text(label, color = pal.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            Text(value, color = pal.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun LabelRow(icon: ImageVector, label: String, pal: Pal) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, pal)
        Spacer(Modifier.size(10.dp))
        Text(label, color = pal.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun IconBadge(icon: ImageVector, pal: Pal) {
    Box(Modifier.size(40.dp).background(CarOrange, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** A rounded surface panel. */
@Composable
private fun Panel(modifier: Modifier, pal: Pal, content: @Composable () -> Unit) {
    Box(modifier.background(pal.surface, RoundedCornerShape(20.dp)).padding(16.dp)) { content() }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 620)
@Composable
private fun DashboardPreviewDark() {
    AutomotiveCarPropAppTheme(darkTheme = true) {
        DashboardScreen(
            state = VehicleUiState(
                connected = true, speedMph = 64, speedText = "64", gearRaw = 0x0008, gearText = "D (Drive)",
                fuelText = "15000", fuelPercent = 42, manufacturer = "Toy Vehicle", model = "Speedy Model",
                modelYear = "2020", rangeText = "310 km", odometerText = "12,345 km",
                speedHistory = listOf(0, 5, 12, 20, 28, 33, 40, 52, 60, 64, 62, 58, 64)
            ),
            night = true, onToggleTheme = {}
        )
    }
}
