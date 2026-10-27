package com.example.closet.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// =========================================================================
// WIDGET OROLOGIO (DIGITALE & ANALOGICO)
// =========================================================================

@Composable
private fun rememberCurrentTime(): LocalTime {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000L)
        }
    }
    return now
}

@Composable
private fun rememberCurrentMinuteTime(): LocalTime {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalTime.now()
            now = current
            val millisUntilNextMinute = ((60 - current.second) * 1000L) - (current.nano / 1_000_000L)
            delay(millisUntilNextMinute.coerceAtLeast(1000L))
        }
    }
    return now
}

@Composable
fun ClockDigitalWidget1x2() {
    val colors = LocalOneUIColors.current
    val now = rememberCurrentMinuteTime()
    val timeStr = remember(now.hour, now.minute) {
        String.format(Locale.getDefault(), "%02d:%02d", now.hour, now.minute)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timeStr,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ClockDigitalWidget2x2() {
    val colors = LocalOneUIColors.current
    val now = rememberCurrentMinuteTime()
    val today = remember { LocalDate.now() }
    val dateStr = remember(today) {
        val dow = today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
        val month = today.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
        "$dow ${today.dayOfMonth} $month"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = dateStr,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accent
        )

        Column {
            Text(
                text = String.format(Locale.getDefault(), "%02d", now.hour),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                lineHeight = 36.sp
            )
            Text(
                text = String.format(Locale.getDefault(), "%02d", now.minute),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = colors.accent,
                lineHeight = 36.sp
            )
        }
    }
}

@Composable
fun ClockDigitalWidget2x4() {
    val colors = LocalOneUIColors.current
    val now = rememberCurrentTime()
    val today = remember { LocalDate.now() }
    val fullDateStr = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN))
            .replaceFirstChar { it.uppercase() }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", now.hour, now.minute),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    lineHeight = 44.sp
                )
                Text(
                    text = String.format(Locale.getDefault(), ":%02d", now.second),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    modifier = Modifier.padding(bottom = 5.dp, start = 4.dp)
                )
            }
            Text(
                text = fullDateStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun ClockAnalogWidget1x1() {
    val colors = LocalOneUIColors.current
    val now = rememberCurrentTime()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        AnalogClockCanvas(
            hour = now.hour,
            minute = now.minute,
            second = now.second,
            accentColor = colors.accent,
            primaryColor = colors.textPrimary,
            tickColor = colors.textMuted,
            showSeconds = false
        )
    }
}

@Composable
fun ClockAnalogWidget2x2() {
    val colors = LocalOneUIColors.current
    val now = rememberCurrentTime()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnalogClockCanvas(
            hour = now.hour,
            minute = now.minute,
            second = now.second,
            accentColor = colors.accent,
            primaryColor = colors.textPrimary,
            tickColor = colors.textMuted,
            showSeconds = true
        )
    }
}

@Composable
fun ClockAnalogWidget2x4(viewModel: ClosetViewModel? = null) {
    val colors = LocalOneUIColors.current
    val now = rememberCurrentTime()
    val today = remember { LocalDate.now() }
    val dayName = remember(today) {
        today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN).replaceFirstChar { it.uppercase() }
    }
    val dateStr = remember(today) {
        "${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN).replaceFirstChar { it.uppercase() }}"
    }
    val todayEventsCount = remember(viewModel?.eventsMap, today) {
        viewModel?.eventsMap?.get(today)?.size ?: 0
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(116.dp)
        ) {
            AnalogClockCanvas(
                hour = now.hour,
                minute = now.minute,
                second = now.second,
                accentColor = colors.accent,
                primaryColor = colors.textPrimary,
                tickColor = colors.textMuted,
                showSeconds = true
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = dayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent
            )
            Text(
                text = dateStr,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
            Text(
                text = if (todayEventsCount == 0) "Nessun impegno oggi" else "$todayEventsCount impegni in programma",
                fontSize = 11.5.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun AnalogClockCanvas(
    hour: Int,
    minute: Int,
    second: Int,
    accentColor: Color,
    primaryColor: Color,
    tickColor: Color,
    showSeconds: Boolean
) {
    val colors = LocalOneUIColors.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = colors.surfaceSecondary,
            radius = radius,
            center = center
        )

        for (i in 0 until 12) {
            val angleDeg = i * 30f
            val isQuarter = i % 3 == 0
            val tickLen = if (isQuarter) 8.dp.toPx() else 4.dp.toPx()
            val rad = Math.toRadians((angleDeg - 90).toDouble())
            val startR = radius - 4.dp.toPx()
            val endR = startR - tickLen
            drawLine(
                color = if (isQuarter) primaryColor else tickColor.copy(alpha = 0.5f),
                start = Offset(
                    center.x + (startR * cos(rad)).toFloat(),
                    center.y + (startR * sin(rad)).toFloat()
                ),
                end = Offset(
                    center.x + (endR * cos(rad)).toFloat(),
                    center.y + (endR * sin(rad)).toFloat()
                ),
                strokeWidth = if (isQuarter) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val hourAngle = ((hour % 12) + minute / 60f) * 30f
        val minAngle = (minute + second / 60f) * 6f
        val secAngle = second * 6f

        rotate(degrees = hourAngle, pivot = center) {
            drawLine(
                color = primaryColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.48f),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        rotate(degrees = minAngle, pivot = center) {
            drawLine(
                color = primaryColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.70f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        if (showSeconds) {
            rotate(degrees = secAngle, pivot = center) {
                drawLine(
                    color = accentColor,
                    start = Offset(center.x, center.y + radius * 0.14f),
                    end = Offset(center.x, center.y - radius * 0.78f),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        drawCircle(
            color = accentColor,
            radius = 3.5.dp.toPx(),
            center = center
        )
    }
}
