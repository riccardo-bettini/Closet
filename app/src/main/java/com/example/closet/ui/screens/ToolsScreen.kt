@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    // Stato orologio
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000L)
        }
    }

    // Stato cronometro separato
    var isStopwatchRunning by remember { mutableStateOf(false) }
    var elapsedDeciseconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isStopwatchRunning) {
        while (isStopwatchRunning) {
            delay(100L)
            elapsedDeciseconds++
        }
    }

    // Stato timer countdown
    var timerTotalSeconds by remember { mutableIntStateOf(300) }
    var timerRemainingSeconds by remember { mutableIntStateOf(300) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showTimerPickerDialog by remember { mutableStateOf(false) }
    LaunchedEffect(isTimerRunning, timerRemainingSeconds) {
        if (isTimerRunning && timerRemainingSeconds > 0) {
            delay(1000L)
            timerRemainingSeconds--
            if (timerRemainingSeconds <= 0) {
                isTimerRunning = false
            }
        }
    }

    // Modalità modifica ordine strumenti
    var isEditMode by remember { mutableStateOf(false) }
    val defaultToolsOrder = listOf(
        "tool_torch",
        "tool_compass",
        "tool_level",
        "tool_clock",
        "tool_stopwatch",
        "tool_timer"
    )
    val workingTools = remember(viewModel.toolsOrder) {
        mutableStateListOf<String>().apply {
            val loaded = viewModel.toolsOrder.filter { it in defaultToolsOrder }
            addAll(loaded)
            defaultToolsOrder.forEach { if (it !in loaded) add(it) }
        }
    }

    LaunchedEffect(viewModel.toolsOrder) {
        val loaded = viewModel.toolsOrder.filter { it in defaultToolsOrder }
        val currentSet = workingTools.toSet()
        if (workingTools.toList() != loaded) {
            workingTools.clear()
            workingTools.addAll(loaded)
            defaultToolsOrder.forEach { if (it !in workingTools) workingTools.add(it) }
        }
    }

    BackHandler(enabled = isEditMode) {
        isEditMode = false
    }

    val density = LocalDensity.current
    val gridState = rememberLazyGridState()
    var draggedToolKey by remember { mutableStateOf<String?>(null) }
    var dragInitialIndex by remember { mutableIntStateOf(-1) }
    var dragAccumulatedX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }
    var dragFingerViewportY by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    var cellTotalWidthPx by remember { mutableFloatStateOf(0f) }
    var cellTotalHeightPx by remember { mutableFloatStateOf(0f) }

    // Auto-scroll continuo durante il trascinamento degli strumenti
    LaunchedEffect(draggedToolKey) {
        if (draggedToolKey == null) return@LaunchedEffect
        val topScrollZonePx = with(density) { 110.dp.toPx() }
        val bottomScrollZonePx = with(density) { 150.dp.toPx() }
        val maxScrollStepPx = with(density) { 12.dp.toPx() }

        while (draggedToolKey != null) {
            if (viewportHeightPx > 0f) {
                val fingerY = dragFingerViewportY
                val topZone = topScrollZonePx
                val bottomZone = viewportHeightPx - bottomScrollZonePx

                val canScrollUp = gridState.canScrollBackward
                val canScrollDown = gridState.canScrollForward

                val scrollDelta: Float = when {
                    fingerY > 0f && fingerY < topZone && canScrollUp -> {
                        val factor = ((topZone - fingerY) / topZone).coerceIn(0.2f, 1f)
                        -maxScrollStepPx * factor
                    }
                    fingerY > bottomZone && canScrollDown -> {
                        val factor = ((fingerY - bottomZone) / bottomScrollZonePx).coerceIn(0.2f, 1f)
                        maxScrollStepPx * factor
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    gridState.scrollBy(scrollDelta)
                }
            }
            delay(16L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewportHeightPx = size.height.toFloat()
            }
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 100.dp)
        ) {
            // Intestazione con pulsante modifica/fatto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(52.dp))
                    Text(
                        text = if (isEditMode) "Modifica Ordine" else "Strumenti",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                if (isEditMode) {
                    Surface(
                        shape = CircleShape,
                        color = toolsAccent,
                        modifier = Modifier.clickable { isEditMode = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Fatto",
                                tint = Color(0xFF13171E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Fatto",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF13171E)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceSecondary,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        modifier = Modifier.clickable { isEditMode = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Modifica ordine",
                                tint = toolsAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Modifica",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        val topFadePx = 28.dp.toPx()
                        val bottomFadePx = 48.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    (topFadePx / size.height).coerceIn(0f, 1f) to Color.Black,
                                    ((size.height - bottomFadePx) / size.height).coerceIn(0f, 1f) to Color.Black,
                                    1f to Color.Transparent
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = workingTools, key = { it }) { toolKey ->
                    val isDraggedItem = draggedToolKey == toolKey
                    val currentScale by animateFloatAsState(
                        targetValue = when {
                            isDraggedItem -> 1.06f
                            isEditMode -> 0.95f
                            else -> 1f
                        },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        ),
                        label = "toolScale_$toolKey"
                    )
                    var itemRootY by remember { mutableFloatStateOf(0f) }

                    val animModifier = if (isDraggedItem) Modifier else Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    )

                    Box(
                        modifier = animModifier
                            .zIndex(if (isDraggedItem) 100f else 1f)
                            .graphicsLayer {
                                scaleX = currentScale
                                scaleY = currentScale
                            }
                            .onGloballyPositioned { coords ->
                                itemRootY = coords.localToRoot(Offset.Zero).y
                            }
                            .onSizeChanged { size ->
                                if (size.width > 0 && size.height > 0) {
                                    val spacingPx = with(density) { 12.dp.toPx() }
                                    cellTotalWidthPx = size.width.toFloat() + spacingPx
                                    cellTotalHeightPx = size.height.toFloat() + spacingPx
                                }
                            }
                            .then(
                                if (isEditMode) {
                                    Modifier.pointerInput(toolKey) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                draggedToolKey = toolKey
                                                dragInitialIndex = workingTools.indexOf(toolKey)
                                                dragAccumulatedX = 0f
                                                dragAccumulatedY = 0f
                                                dragFingerViewportY = itemRootY + offset.y
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAccumulatedX += dragAmount.x
                                                dragAccumulatedY += dragAmount.y
                                                dragFingerViewportY += dragAmount.y

                                                val curIdx = workingTools.indexOf(toolKey)
                                                if (curIdx != -1 && cellTotalHeightPx > 0f && cellTotalWidthPx > 0f && dragInitialIndex != -1) {
                                                    val dragInitialRow = dragInitialIndex / 2
                                                    val dragInitialCol = dragInitialIndex % 2
                                                    val maxRow = (workingTools.size - 1) / 2

                                                    val minAllowedY = (0 - dragInitialRow) * cellTotalHeightPx - cellTotalHeightPx * 0.35f
                                                    val maxAllowedY = (maxRow - dragInitialRow) * cellTotalHeightPx + cellTotalHeightPx * 0.35f
                                                    val effectiveY = dragAccumulatedY.coerceIn(minAllowedY, maxAllowedY)

                                                    val minAllowedX = (0 - dragInitialCol) * cellTotalWidthPx - cellTotalWidthPx * 0.35f
                                                    val maxAllowedX = (1 - dragInitialCol) * cellTotalWidthPx + cellTotalWidthPx * 0.35f
                                                    val effectiveX = dragAccumulatedX.coerceIn(minAllowedX, maxAllowedX)

                                                    val targetRow = (dragInitialRow + (effectiveY / cellTotalHeightPx).roundToInt()).coerceIn(0, maxRow)
                                                    val targetCol = (dragInitialCol + (effectiveX / cellTotalWidthPx).roundToInt()).coerceIn(0, 1)
                                                    val targetIdx = (targetRow * 2 + targetCol).coerceIn(0, workingTools.size - 1)
                                                    if (targetIdx != curIdx) {
                                                        val moved = workingTools.removeAt(curIdx)
                                                        workingTools.add(targetIdx, moved)
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                viewModel.updateToolsOrder(workingTools.toList())
                                                draggedToolKey = null
                                                dragInitialIndex = -1
                                                dragAccumulatedX = 0f
                                                dragAccumulatedY = 0f
                                                dragFingerViewportY = 0f
                                            },
                                            onDragCancel = {
                                                viewModel.updateToolsOrder(workingTools.toList())
                                                draggedToolKey = null
                                                dragInitialIndex = -1
                                                dragAccumulatedX = 0f
                                                dragAccumulatedY = 0f
                                                dragFingerViewportY = 0f
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                            .graphicsLayer {
                                if (isDraggedItem && dragInitialIndex != -1 && cellTotalHeightPx > 0f && cellTotalWidthPx > 0f) {
                                    val curIdx = workingTools.indexOf(toolKey)
                                    val effectiveIdx = if (curIdx != -1) curIdx else dragInitialIndex
                                    val curRow = effectiveIdx / 2
                                    val curCol = effectiveIdx % 2
                                    val dragInitialRow = dragInitialIndex / 2
                                    val dragInitialCol = dragInitialIndex % 2
                                    val maxRow = (workingTools.size - 1) / 2

                                    val minAllowedY = (0 - dragInitialRow) * cellTotalHeightPx - cellTotalHeightPx * 0.35f
                                    val maxAllowedY = (maxRow - dragInitialRow) * cellTotalHeightPx + cellTotalHeightPx * 0.35f
                                    val effectiveY = dragAccumulatedY.coerceIn(minAllowedY, maxAllowedY)

                                    val minAllowedX = (0 - dragInitialCol) * cellTotalWidthPx - cellTotalWidthPx * 0.35f
                                    val maxAllowedX = (1 - dragInitialCol) * cellTotalWidthPx + cellTotalWidthPx * 0.35f
                                    val effectiveX = dragAccumulatedX.coerceIn(minAllowedX, maxAllowedX)

                                    translationX = effectiveX - (curCol - dragInitialCol) * cellTotalWidthPx
                                    translationY = effectiveY - (curRow - dragInitialRow) * cellTotalHeightPx
                                    shadowElevation = 24f
                                } else {
                                    translationX = 0f
                                    translationY = 0f
                                    shadowElevation = 0f
                                }
                            }
                    ) {
                        val cardModifier = Modifier.fillMaxWidth()
                        when (toolKey) {
                        "tool_torch" -> {
                            val torchColor = Color(0xFFEAB308)
                            ToolDisplayGridCard(
                                modifier = cardModifier,
                                title = "Torcia LED",
                                subtitle = if (isTorchOn) "Accesa • Tocca per spegnere" else "Spenta • Tocca per attivare",
                                accentColor = torchColor,
                                isActive = isTorchOn,
                                isEditMode = isEditMode,
                                isDragging = isDraggedItem,
                                onClick = { if (!isEditMode) toggleTorch() },
                                onLongClick = { isEditMode = true }
                            ) {
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

                        "tool_compass" -> {
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
                                modifier = cardModifier,
                                title = "Bussola",
                                subtitle = "$deg° $fullDir",
                                accentColor = Color(0xFF388AF6),
                                isActive = true,
                                isEditMode = isEditMode,
                                isDragging = isDraggedItem,
                                onClick = {},
                                onLongClick = { isEditMode = true }
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

                        "tool_level" -> {
                            val pitch = compassReading.pitchDegrees
                            val roll = compassReading.rollDegrees
                            val isLevel = abs(pitch) < 1.5f && abs(roll) < 1.5f
                            val levelColor = if (isLevel) Color(0xFF10B981) else colors.accent

                            ToolDisplayGridCard(
                                modifier = cardModifier,
                                title = "Livella a Bolla",
                                subtitle = if (isLevel) "Perfettamente in bolla" else "X: ${roll.roundToInt()}° • Y: ${pitch.roundToInt()}°",
                                accentColor = levelColor,
                                isActive = isLevel,
                                isEditMode = isEditMode,
                                isDragging = isDraggedItem,
                                onClick = {},
                                onLongClick = { isEditMode = true }
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

                        "tool_clock" -> {
                            val clockColor = Color(0xFF06B6D4)
                            val formattedDate = remember(currentTime) {
                                val date = LocalDate.now()
                                val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
                                date.format(formatter).replaceFirstChar { it.uppercase() }
                            }

                            ToolDisplayGridCard(
                                modifier = cardModifier,
                                title = "Orologio",
                                subtitle = formattedDate,
                                accentColor = clockColor,
                                isActive = true,
                                isEditMode = isEditMode,
                                isDragging = isDraggedItem,
                                onClick = {},
                                onLongClick = { isEditMode = true }
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
                                            text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = colors.textPrimary,
                                            style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = clockColor.copy(alpha = 0.16f),
                                            border = BorderStroke(0.5.dp, clockColor.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = "Ora esatta",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = clockColor,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "tool_stopwatch" -> {
                            val stopwatchColor = Color(0xFF8B5CF6)
                            val mins = (elapsedDeciseconds / 600L)
                            val secs = (elapsedDeciseconds % 600L) / 10L
                            val decis = elapsedDeciseconds % 10L
                            val swText = String.format(Locale.getDefault(), "%02d:%02d.%d", mins, secs, decis)

                            ToolDisplayGridCard(
                                modifier = cardModifier,
                                title = "Cronometro",
                                subtitle = if (isStopwatchRunning) {
                                    "In corso • Tocca per pausa"
                                } else if (elapsedDeciseconds > 0L) {
                                    "In pausa • Tocca per riprendere"
                                } else {
                                    "Pronto • Tocca per avviare"
                                },
                                accentColor = stopwatchColor,
                                isActive = isStopwatchRunning,
                                isEditMode = isEditMode,
                                isDragging = isDraggedItem,
                                onClick = {
                                    if (!isEditMode) {
                                        isStopwatchRunning = !isStopwatchRunning
                                    }
                                },
                                onLongClick = { isEditMode = true }
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
                                            text = swText,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isStopwatchRunning) stopwatchColor else colors.textPrimary,
                                            style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isStopwatchRunning) stopwatchColor.copy(alpha = 0.18f) else colors.cardBackground,
                                                border = BorderStroke(0.5.dp, if (isStopwatchRunning) stopwatchColor else colors.cardBorder),
                                                modifier = Modifier.clickable {
                                                    if (!isEditMode) isStopwatchRunning = !isStopwatchRunning
                                                }
                                            ) {
                                                Text(
                                                    text = if (isStopwatchRunning) "Pausa" else "Avvia",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isStopwatchRunning) stopwatchColor else colors.textPrimary,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                )
                                            }

                                            if (elapsedDeciseconds > 0L) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFE55353).copy(alpha = 0.18f),
                                                    modifier = Modifier.clickable {
                                                        if (!isEditMode) {
                                                            isStopwatchRunning = false
                                                            elapsedDeciseconds = 0L
                                                        }
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

                        "tool_timer" -> {
                            val timerColor = Color(0xFFF97316)
                            val tHours = timerRemainingSeconds / 3600
                            val tMins = (timerRemainingSeconds % 3600) / 60
                            val tSecs = timerRemainingSeconds % 60
                            val timerText = if (timerTotalSeconds >= 3600 || tHours > 0) {
                                String.format(Locale.getDefault(), "%02d:%02d:%02d", tHours, tMins, tSecs)
                            } else {
                                String.format(Locale.getDefault(), "%02d:%02d", tMins, tSecs)
                            }
                            val isCompleted = timerRemainingSeconds <= 0

                            ToolDisplayGridCard(
                                modifier = cardModifier,
                                title = "Timer",
                                subtitle = when {
                                    isCompleted -> "Finito! Tocca per riavviare"
                                    isTimerRunning -> "In corso • Tocca per pausa"
                                    timerRemainingSeconds < timerTotalSeconds -> "In pausa • Tocca per riprendere"
                                    else -> "Pronto • Tocca per avviare"
                                },
                                accentColor = timerColor,
                                isActive = isTimerRunning,
                                isEditMode = isEditMode,
                                isDragging = isDraggedItem,
                                onClick = {
                                    if (!isEditMode) {
                                        if (isCompleted) {
                                            timerRemainingSeconds = timerTotalSeconds
                                            isTimerRunning = true
                                        } else {
                                            isTimerRunning = !isTimerRunning
                                        }
                                    }
                                },
                                onLongClick = { isEditMode = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colors.surfaceSecondary)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Countdown text - tocca per aprire il selettore tempo a scorrimento
                                        Text(
                                            text = timerText,
                                            fontSize = if (timerText.length > 5) 19.sp else 23.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when {
                                                isCompleted -> Color(0xFF10B981)
                                                isTimerRunning -> timerColor
                                                else -> colors.textPrimary
                                            },
                                            modifier = Modifier.clickable {
                                                if (!isEditMode) {
                                                    showTimerPickerDialog = true
                                                }
                                            },
                                            style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                        )

                                        // Controlli timer e tasto menu selettore tempo
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Tasto IMPOSTA TEMPO (menu selettore ore, min, sec a scorrimento)
                                            Surface(
                                                shape = CircleShape,
                                                color = timerColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f),
                                                border = BorderStroke(0.5.dp, timerColor.copy(alpha = 0.45f)),
                                                modifier = Modifier.clickable {
                                                    if (!isEditMode) {
                                                        showTimerPickerDialog = true
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Timer,
                                                        contentDescription = "Imposta tempo",
                                                        tint = timerColor,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "Imposta",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = timerColor
                                                    )
                                                }
                                            }

                                            // Tasto +1m rapido
                                            Surface(
                                                shape = CircleShape,
                                                color = colors.cardBackground,
                                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                                modifier = Modifier.clickable {
                                                    if (!isEditMode) {
                                                        timerRemainingSeconds += 60
                                                        timerTotalSeconds = timerRemainingSeconds
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "+1m",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }

                                            // Tasto Reset (quando diverso dal totale o in corso o finito)
                                            if (timerRemainingSeconds != timerTotalSeconds || isCompleted || isTimerRunning) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFE55353).copy(alpha = 0.18f),
                                                    modifier = Modifier.clickable {
                                                        if (!isEditMode) {
                                                            isTimerRunning = false
                                                            timerRemainingSeconds = timerTotalSeconds
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        text = "Reset",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFE55353),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
                }
            }
        }

        if (showTimerPickerDialog) {
            SamsungTimerDurationPickerDialog(
                initialSeconds = timerTotalSeconds,
                onDismiss = { showTimerPickerDialog = false },
                onConfirm = { newSeconds ->
                    timerTotalSeconds = newSeconds
                    timerRemainingSeconds = newSeconds
                    isTimerRunning = false
                    showTimerPickerDialog = false
                }
            )
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
                onClick = {
                    if (isEditMode) isEditMode = false else onBack()
                },
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
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    accentColor: Color,
    isActive: Boolean,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    displayContent: @Composable BoxScope.() -> Unit
) {
    val colors = LocalOneUIColors.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(
            width = if (isDragging) 2.dp else if (isActive || isEditMode) 1.5.dp else 1.dp,
            color = when {
                isDragging -> colors.accent
                isEditMode -> colors.accent.copy(alpha = 0.6f)
                isActive -> accentColor.copy(alpha = 0.7f)
                else -> colors.cardBorder
            }
        ),
        tonalElevation = 2.dp,
        shadowElevation = if (isDragging) 12.dp else if (isEditMode) 4.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongClick?.invoke() }
                        )
                    }
                } else Modifier
            )
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (isActive) accentColor else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                if (isEditMode) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Trascina per ordinare",
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


// =========================================================================
// SELETTORE TEMPO A SCORRIMENTO (ORE, MINUTI, SECONDI) STILE SAMSUNG CLOCK
// =========================================================================

@Composable
fun SamsungTimerDurationPickerDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val colors = LocalOneUIColors.current
    val timerColor = Color(0xFFF97316)

    val initHours = (initialSeconds / 3600).coerceIn(0, 23)
    val initMins = ((initialSeconds % 3600) / 60).coerceIn(0, 59)
    val initSecs = (initialSeconds % 60).coerceIn(0, 59)

    var selectedHour by remember { mutableIntStateOf(initHours) }
    var selectedMinute by remember { mutableIntStateOf(initMins) }
    var selectedSecond by remember { mutableIntStateOf(initSecs) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.dialogBackground,
            border = BorderStroke(1.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header popup con badge icona e titolo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(timerColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = timerColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Imposta timer",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(colors.surfaceSecondary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.cardBorder)

                // Etichette colonne (Ore, Min, Sec)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ore",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.width(72.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "Min",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.width(72.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "Sec",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.width(72.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                val itemHeight = 50.dp
                val containerHeight = 164.dp

                // Contenitore ruote con capsula centrale One UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(containerHeight),
                    contentAlignment = Alignment.Center
                ) {
                    // Capsula centrale di selezione stile Samsung Clock
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = timerColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.2.dp, timerColor.copy(alpha = 0.40f)),
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(itemHeight)
                    ) {}

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SamsungDurationWheelColumn(
                            maxValue = 24,
                            initialValue = initHours,
                            itemHeight = itemHeight,
                            containerHeight = containerHeight,
                            width = 72.dp,
                            accentColor = timerColor,
                            onValueChange = { selectedHour = it }
                        )

                        Text(
                            text = ":",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = timerColor,
                            modifier = Modifier.offset(y = (-1).dp)
                        )

                        SamsungDurationWheelColumn(
                            maxValue = 60,
                            initialValue = initMins,
                            itemHeight = itemHeight,
                            containerHeight = containerHeight,
                            width = 72.dp,
                            accentColor = timerColor,
                            onValueChange = { selectedMinute = it }
                        )

                        Text(
                            text = ":",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = timerColor,
                            modifier = Modifier.offset(y = (-1).dp)
                        )

                        SamsungDurationWheelColumn(
                            maxValue = 60,
                            initialValue = initSecs,
                            itemHeight = itemHeight,
                            containerHeight = containerHeight,
                            width = 72.dp,
                            accentColor = timerColor,
                            onValueChange = { selectedSecond = it }
                        )
                    }
                }

                // Preset rapidi One UI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "1 min" to 60,
                        "5 min" to 300,
                        "10 min" to 600,
                        "15 min" to 900,
                        "30 min" to 1800
                    ).forEach { (label, secs) ->
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceSecondary,
                            border = BorderStroke(0.5.dp, colors.cardBorder),
                            modifier = Modifier.clickable {
                                onConfirm(secs)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = colors.cardBorder)

                // Pulsanti azione One UI simmetrici
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceSecondary,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Text(
                            text = "Annulla",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val total = selectedHour * 3600 + selectedMinute * 60 + selectedSecond
                            onConfirm(if (total > 0) total else 300)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = timerColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Imposta",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SamsungDurationWheelColumn(
    maxValue: Int,
    initialValue: Int,
    itemHeight: Dp,
    containerHeight: Dp,
    width: Dp = 72.dp,
    accentColor: Color = LocalOneUIColors.current.accent,
    onValueChange: (Int) -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val animatable = remember { Animatable(initialValue.toFloat()) }

    LaunchedEffect(animatable.value) {
        val centerIndex = animatable.value.roundToInt()
        val normalized = ((centerIndex % maxValue) + maxValue) % maxValue
        onValueChange(normalized)
    }

    val currentValue = animatable.value
    val centerIndex = currentValue.roundToInt()

    Box(
        modifier = Modifier
            .width(width)
            .height(containerHeight)
            .clipToBounds()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { deltaPx ->
                    coroutineScope.launch {
                        animatable.snapTo(animatable.value - deltaPx / itemHeightPx)
                    }
                },
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        val momentumOffset = -(velocity / itemHeightPx) * 0.16f
                        val projected = (animatable.value + momentumOffset).roundToInt()
                        animatable.animateTo(
                            targetValue = projected.toFloat(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialVelocity = -velocity / itemHeightPx
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        for (step in -2..2) {
            val itemIndex = centerIndex + step
            val displayValue = ((itemIndex % maxValue) + maxValue) % maxValue
            val diffFromCenter = itemIndex - currentValue
            val yOffsetPx = diffFromCenter * itemHeightPx

            val dist = kotlin.math.abs(diffFromCenter)
            if (dist <= 2.2f) {
                val scale = (1.08f - dist * 0.22f).coerceIn(0.72f, 1.08f)
                val alpha = (1f - dist * 0.62f).coerceIn(0.10f, 1f)
                val isSelected = dist < 0.5f

                val rotationX = (-diffFromCenter * 22f).coerceIn(-50f, 50f)

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(0, yOffsetPx.roundToInt()) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            this.rotationX = rotationX
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (step != 0) {
                                coroutineScope.launch {
                                    animatable.animateTo(
                                        targetValue = itemIndex.toFloat(),
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", displayValue),
                        fontSize = if (isSelected) 30.sp else 20.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentColor else colors.textSecondary
                    )
                }
            }
        }
    }
}
