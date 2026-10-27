@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ToolsScreen(
    viewModel: ClosetViewModel,
    onBack: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val toolsAccent = if (viewModel.useDistinctFunctionColors) Color(0xFFEAB308) else colors.accent

    val (isTorchOn, toggleTorch) = rememberTorchState()
    val compassReading = rememberCompassReading()

    // Stato per Cronometro & Orologio nel quarto slot
    var isStopwatchMode by remember { mutableStateOf(false) }
    var isStopwatchRunning by remember { mutableStateOf(false) }
    var elapsedDeciseconds by remember { mutableLongStateOf(0L) }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000L)
        }
    }

    LaunchedEffect(isStopwatchRunning) {
        while (isStopwatchRunning) {
            delay(100L)
            elapsedDeciseconds++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 100.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(52.dp))

                Column {
                    Text(
                        text = "Strumenti",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Display interattivi rapidi e sensori in tempo reale",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TORCIA LED
            item(key = "tool_torch") {
                ToolDisplayGridCard(
                    title = "Torcia LED",
                    subtitle = if (isTorchOn) "Accesa • Tocca per spegnere" else "Spenta • Tocca per attivare",
                    accentColor = Color(0xFFEAB308),
                    isActive = isTorchOn,
                    onClick = { toggleTorch() }
                ) {
                    val torchColor = Color(0xFFEAB308)
                    val bgAnim by animateColorAsState(
                        targetValue = if (isTorchOn) torchColor.copy(alpha = 0.22f) else colors.surfaceSecondary,
                        animationSpec = tween(220),
                        label = "toolTorchBg"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgAnim),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isTorchOn) torchColor else colors.cardBackground)
                                    .border(
                                        width = if (isTorchOn) 2.5.dp else 1.dp,
                                        color = if (isTorchOn) Color.White.copy(alpha = 0.6f) else colors.cardBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOff,
                                    contentDescription = "Torcia",
                                    tint = if (isTorchOn) Color(0xFF1A1600) else colors.textPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isTorchOn) torchColor else colors.cardBackground,
                                border = BorderStroke(0.5.dp, if (isTorchOn) torchColor else colors.cardBorder)
                            ) {
                                Text(
                                    text = if (isTorchOn) "ON" else "OFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isTorchOn) Color(0xFF1A1600) else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. BUSSOLA MAGNETICA
            item(key = "tool_compass") {
                val deg = compassReading.azimuthDegrees.roundToInt() % 360
                val (shortDir, fullDir) = remember(compassReading.azimuthDegrees) {
                    val norm = ((compassReading.azimuthDegrees % 360f) + 360f) % 360f
                    when {
                        norm >= 337.5f || norm < 22.5f -> "N" to "Nord"
                        norm < 67.5f -> "NE" to "Nord-Est"
                        norm < 112.5f -> "E" to "Est"
                        norm < 157.5f -> "SE" to "Sud-Est"
                        norm < 202.5f -> "S" to "Sud"
                        norm < 247.5f -> "SO" to "Sud-Ovest"
                        norm < 292.5f -> "O" to "Ovest"
                        else -> "NO" to "Nord-Ovest"
                    }
                }

                ToolDisplayGridCard(
                    title = "Bussola",
                    subtitle = "$deg° $fullDir",
                    accentColor = Color(0xFF388AF6),
                    isActive = true,
                    onClick = {}
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surfaceSecondary)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            drawCircle(
                                color = colors.cardBackground,
                                radius = radius,
                                center = center
                            )
                            drawCircle(
                                color = colors.cardBorder,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            drawLine(
                                color = Color(0xFF388AF6),
                                start = Offset(center.x, center.y - radius + 2.dp.toPx()),
                                end = Offset(center.x, center.y - radius + 8.dp.toPx()),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            rotate(-compassReading.azimuthDegrees, pivot = center) {
                                for (angle in 0 until 360 step 15) {
                                    val isCardinal = angle % 90 == 0
                                    val isNorth = angle == 0
                                    val rad = Math.toRadians((angle - 90).toDouble())
                                    val outerR = radius - 6.dp.toPx()
                                    val innerR = if (isCardinal) outerR - 8.dp.toPx() else outerR - 4.dp.toPx()

                                    val startX = center.x + (outerR * cos(rad)).toFloat()
                                    val startY = center.y + (outerR * sin(rad)).toFloat()
                                    val endX = center.x + (innerR * cos(rad)).toFloat()
                                    val endY = center.y + (innerR * sin(rad)).toFloat()

                                    drawLine(
                                        color = when {
                                            isNorth -> Color(0xFFE55353)
                                            isCardinal -> colors.textPrimary
                                            else -> colors.textMuted.copy(alpha = 0.6f)
                                        },
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = if (isCardinal) 2.2.dp.toPx() else 1.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }

                                val northTipY = center.y - radius + 16.dp.toPx()
                                val northBaseY = northTipY + 8.dp.toPx()
                                val northTriangle = Path().apply {
                                    moveTo(center.x, northTipY)
                                    lineTo(center.x - 5.dp.toPx(), northBaseY)
                                    lineTo(center.x + 5.dp.toPx(), northBaseY)
                                    close()
                                }
                                drawPath(northTriangle, Color(0xFFE55353))
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = shortDir,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (shortDir == "N") Color(0xFFE55353) else Color(0xFF388AF6),
                                style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                            Text(
                                text = "$deg°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }
                }
            }

            // 3. LIVELLA A BOLLA 2D
            item(key = "tool_level") {
                val pitch = compassReading.pitchDegrees
                val roll = compassReading.rollDegrees
                val isLevel = abs(pitch) < 1.5f && abs(roll) < 1.5f
                val levelColor = if (isLevel) Color(0xFF10B981) else colors.accent

                ToolDisplayGridCard(
                    title = "Livella a Bolla",
                    subtitle = "X: ${roll.roundToInt()}° • Y: ${pitch.roundToInt()}°",
                    accentColor = levelColor,
                    isActive = isLevel,
                    onClick = {}
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surfaceSecondary)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            drawCircle(
                                color = colors.cardBackground,
                                radius = radius,
                                center = center
                            )
                            drawCircle(
                                color = if (isLevel) Color(0xFF10B981).copy(alpha = 0.7f) else colors.cardBorder,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            // Cerchio centrale di riferimento
                            drawCircle(
                                color = colors.textMuted.copy(alpha = 0.35f),
                                radius = radius * 0.28f,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // Croce centrale
                            drawLine(
                                color = colors.textMuted.copy(alpha = 0.25f),
                                start = Offset(center.x - radius * 0.75f, center.y),
                                end = Offset(center.x + radius * 0.75f, center.y),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = colors.textMuted.copy(alpha = 0.25f),
                                start = Offset(center.x, center.y - radius * 0.75f),
                                end = Offset(center.x, center.y + radius * 0.75f),
                                strokeWidth = 1.dp.toPx()
                            )

                            val maxTilt = 45f
                            val normX = (roll / maxTilt).coerceIn(-1f, 1f)
                            val normY = (pitch / maxTilt).coerceIn(-1f, 1f)
                            val maxOffset = radius * 0.65f
                            val bubbleCenter = Offset(
                                x = center.x + normX * maxOffset,
                                y = center.y + normY * maxOffset
                            )

                            drawCircle(
                                color = levelColor.copy(alpha = 0.3f),
                                radius = 13.dp.toPx(),
                                center = bubbleCenter
                            )
                            drawCircle(
                                color = levelColor,
                                radius = 9.dp.toPx(),
                                center = bubbleCenter
                            )
                        }
                    }
                }
            }

            // 4. OROLOGIO & CRONOMETRO
            item(key = "tool_clock_stopwatch") {
                val mins = (elapsedDeciseconds / 600L)
                val secs = (elapsedDeciseconds % 600L) / 10L
                val decis = elapsedDeciseconds % 10L
                val swText = String.format("%02d:%02d.%d", mins, secs, decis)

                ToolDisplayGridCard(
                    title = if (isStopwatchMode) "Cronometro" else "Orologio Digitale",
                    subtitle = if (isStopwatchMode) {
                        if (isStopwatchRunning) "In corso • Tocca per pausa" else "Tocca per avviare"
                    } else {
                        "Tocca per usare il cronometro"
                    },
                    accentColor = Color(0xFF8B5CF6),
                    isActive = isStopwatchRunning,
                    onClick = {
                        if (!isStopwatchMode) {
                            isStopwatchMode = true
                        } else {
                            isStopwatchRunning = !isStopwatchRunning
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surfaceSecondary)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isStopwatchMode) swText else currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isStopwatchRunning) Color(0xFF8B5CF6) else colors.textPrimary,
                                style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.cardBackground,
                                    border = BorderStroke(0.5.dp, colors.cardBorder),
                                    modifier = Modifier.clickable {
                                        isStopwatchMode = !isStopwatchMode
                                    }
                                ) {
                                    Text(
                                        text = if (isStopwatchMode) "Ora" else "Timer",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (isStopwatchMode && elapsedDeciseconds > 0L) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE55353).copy(alpha = 0.18f),
                                        modifier = Modifier.clickable {
                                            isStopwatchRunning = false
                                            elapsedDeciseconds = 0L
                                        }
                                    ) {
                                        Text(
                                            text = "Reset",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE55353),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Tasto Indietro Fluttuante e Fisso sullo schermo
        Surface(
            shape = CircleShape,
            color = colors.surfaceSecondary,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 18.dp)
                .size(40.dp)
                .zIndex(20f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Torna alle funzioni",
                    tint = toolsAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ToolDisplayGridCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    displayContent: @Composable BoxScope.() -> Unit
) {
    val colors = LocalOneUIColors.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) accentColor.copy(alpha = 0.7f) else colors.cardBorder
        ),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceSecondary),
                contentAlignment = Alignment.Center,
                content = displayContent
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isActive) accentColor else colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
