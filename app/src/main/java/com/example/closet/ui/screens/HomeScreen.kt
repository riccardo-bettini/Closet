@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.closet.ui.updater.StartupUpdateDialog
import com.example.closet.ui.viewmodel.ClosetFolder
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// =========================================================================
// HELPER PER OTTIMIZZAZIONE IMMAGINI (COIL DOWNSAMPLING + GPU HARDWARE BITMAPS)
// =========================================================================

@Composable
fun rememberOptimizedPainter(
    uri: Uri?,
    targetSizeDp: Int = 120
): androidx.compose.ui.graphics.painter.Painter {
    val context = LocalContext.current
    val density = LocalDensity.current
    val px = with(density) { targetSizeDp.dp.roundToPx() }

    return rememberAsyncImagePainter(
        model = remember(uri, px) {
            ImageRequest.Builder(context)
                .data(uri)
                .size(px, px)
                .allowHardware(true)
                .crossfade(150)
                .build()
        }
    )
}

// =========================================================================
// HELPER TORCIA E SENSORI BUSSOLA
// =========================================================================

@Composable
fun rememberTorchState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }
    var torchCameraId by remember { mutableStateOf<String?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    DisposableEffect(cameraManager) {
        if (cameraManager == null) return@DisposableEffect onDispose {}

        val foundId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
        torchCameraId = foundId

        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == foundId) {
                    isTorchOn = enabled
                }
            }
        }

        try {
            cameraManager.registerTorchCallback(callback, null)
        } catch (_: Exception) {}

        onDispose {
            try {
                cameraManager.unregisterTorchCallback(callback)
            } catch (_: Exception) {}
        }
    }

    val toggleTorch = {
        val camId = torchCameraId
        if (cameraManager != null && camId != null) {
            try {
                cameraManager.setTorchMode(camId, !isTorchOn)
            } catch (_: Exception) {
                Toast.makeText(context, "Impossibile attivare la torcia", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Flash non disponibile su questo dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    return isTorchOn to toggleTorch
}

@Immutable
data class CompassReading(
    val azimuthDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f
)

@Composable
fun rememberCompassReading(): CompassReading {
    val context = LocalContext.current
    var reading by remember { mutableStateOf(CompassReading()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) return@DisposableEffect onDispose {}

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false
        var smoothedAzimuth = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        val rawAzimuth = ((Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f)
                        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                        var delta = rawAzimuth - smoothedAzimuth
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        smoothedAzimuth = (smoothedAzimuth + delta * 0.22f + 360f) % 360f

                        reading = CompassReading(
                            azimuthDegrees = smoothedAzimuth,
                            pitchDegrees = pitch,
                            rollDegrees = roll
                        )
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity[0] = gravity[0] * 0.85f + event.values[0] * 0.15f
                        gravity[1] = gravity[1] * 0.85f + event.values[1] * 0.15f
                        gravity[2] = gravity[2] * 0.85f + event.values[2] * 0.15f
                        hasGravity = true
                        if (hasGeomagnetic && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val rawAzimuth = ((Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f)
                            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                            var delta = rawAzimuth - smoothedAzimuth
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            smoothedAzimuth = (smoothedAzimuth + delta * 0.22f + 360f) % 360f

                            reading = CompassReading(
                                azimuthDegrees = smoothedAzimuth,
                                pitchDegrees = pitch,
                                rollDegrees = roll
                            )
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic[0] = geomagnetic[0] * 0.85f + event.values[0] * 0.15f
                        geomagnetic[1] = geomagnetic[1] * 0.85f + event.values[1] * 0.15f
                        geomagnetic[2] = geomagnetic[2] * 0.85f + event.values[2] * 0.15f
                        hasGeomagnetic = true
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelSensor != null) sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            if (magSensor != null) sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return reading
}

fun getCardinalLabel(degrees: Float): Pair<String, String> {
    val norm = ((degrees % 360f) + 360f) % 360f
    return when {
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
// =========================================================================
// ISOLA SUPERIORE AZIONI (ONE UI TOP ACTION ISLAND)
// =========================================================================

@Composable
fun OneUITopActionIsland(
    showSearchButton: Boolean = false,
    isSearchActive: Boolean = false,
    onSearchClick: () -> Unit = {},
    showTodayButton: Boolean = false,
    todayDayNumber: String = "",
    hasActiveReminders: Boolean = false,
    onReminderClick: () -> Unit = {},
    isMoreMenuOpen: Boolean = false,
    onMoreOptionsClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val moreRotation by animateFloatAsState(
        targetValue = if (isMoreMenuOpen) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moreIconRotation"
    )

    Surface(
        shape = CircleShape,
        color = colors.islandBackground,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showTodayButton) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.accentContainer)
                        .border(
                            width = 1.5.dp,
                            color = colors.accent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = todayDayNumber,
                        color = colors.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            if (showSearchButton) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSearchActive) colors.accent else Color.Transparent)
                        .clickable(onClick = onSearchClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cerca",
                        tint = if (isSearchActive) Color.White else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (hasActiveReminders) colors.accent else colors.surfaceSecondary)
                    .clickable(onClick = onReminderClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasActiveReminders) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = "Promemoria",
                    tint = if (hasActiveReminders) Color.White else colors.textSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isMoreMenuOpen) colors.accentContainer else Color.Transparent)
                    .clickable(onClick = onMoreOptionsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opzioni",
                    tint = if (isMoreMenuOpen) colors.accent else colors.textPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = moreRotation }
                )
            }
        }
    }
}

@Composable
fun TopActionMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    titleColor: Color = LocalOneUIColors.current.textPrimary,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.5.dp)
            )
            Text(
                text = title,
                color = titleColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

// =========================================================================
// ISOLA INFERIORE FLOTTANTE DINAMICA (DA 2 A 5 SCHEDE - 120 FPS)
// =========================================================================

@Composable
private fun getIslandTabIcons(tab: IslandTabType): Pair<ImageVector, ImageVector> = when (tab) {
    IslandTabType.HOME -> Icons.Filled.Home to Icons.Outlined.Home
    IslandTabType.WARDROBE -> Icons.Filled.Checkroom to Icons.Outlined.Checkroom
    IslandTabType.FUNCTIONS -> Icons.Default.DashboardCustomize to Icons.Default.DashboardCustomize
    IslandTabType.CALENDAR -> Icons.Filled.CalendarMonth to Icons.Outlined.CalendarMonth
    IslandTabType.CLOSET_PHOTOS -> Icons.Filled.CameraAlt to Icons.Outlined.CameraAlt
    IslandTabType.MOVE -> Icons.Default.SyncAlt to Icons.Outlined.SyncAlt
    IslandTabType.LAUNDRY -> Icons.Filled.LocalLaundryService to Icons.Outlined.LocalLaundryService
    IslandTabType.TRIP -> Icons.Filled.Work to Icons.Outlined.Work
    IslandTabType.TRANSFERS_ACTIVITY -> Icons.Default.History to Icons.Outlined.History
    IslandTabType.TOOLS -> Icons.Filled.Construction to Icons.Outlined.Construction
}

@Composable
fun OneUIFloatingIsland(
    tabs: List<IslandTabType>,
    currentPosition: Float,
    isDraggingCursor: Boolean,
    onTabSelected: (Int, IslandTabType) -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current

    val tabCount = tabs.size.coerceIn(2, MAX_ISLAND_TABS)
    val collapsedWidthF = when {
        tabCount <= 3 -> 42f
        tabCount == 4 -> 38f
        else -> 34f
    }
    val expandedExtraWidthF = when {
        tabCount <= 3 -> 42f
        tabCount == 4 -> 38f
        else -> 36f
    }
    val spacingF = 4f
    val horizontalPadF = 5f
    val slotStepF = collapsedWidthF + spacingF
    val totalIslandWidthDp = (
            horizontalPadF * 2f +
                    tabCount * collapsedWidthF +
                    expandedExtraWidthF +
                    (tabCount - 1) * spacingF
            ).dp

    val slotStepPx = with(density) { slotStepF.dp.toPx() }
    var accumulatedDeltaX by remember { mutableFloatStateOf(0f) }

    val safePos = currentPosition.coerceIn(0f, (tabCount - 1).toFloat())
    val fraction = (safePos - safePos.toInt()).coerceIn(0f, 1f)
    val stretch = (sin(fraction * Math.PI.toFloat()) * 10f).coerceAtLeast(0f)
    val cursorWidth = (collapsedWidthF + expandedExtraWidthF + stretch).dp
    val cursorOffset = ((safePos * slotStepF) - (stretch / 2f)).dp

    val cursorColor = if (colors.isDark) Color(0xFF282D37) else Color(0xFFE5E8EF)

    val cursorScale by animateFloatAsState(
        targetValue = if (isDraggingCursor) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cursorScaleAnim"
    )

    val cursorOffsetPx = with(density) { cursorOffset.toPx() }

    Box(
        modifier = Modifier
            .height(54.dp)
            .width(totalIslandWidthDp)
            .pointerInput(tabCount) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        accumulatedDeltaX = 0f
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDeltaX += dragAmount.x
                        val deltaPages = accumulatedDeltaX / slotStepPx
                        onDragDelta(deltaPages)
                    },
                    onDragEnd = {
                        onDragEnd()
                        accumulatedDeltaX = 0f
                    },
                    onDragCancel = {
                        onDragCancel()
                        accumulatedDeltaX = 0f
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadF.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = cursorOffsetPx
                        scaleX = cursorScale
                        scaleY = cursorScale
                    }
                    .width(cursorWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(cursorColor)
                    .border(
                        width = if (isDraggingCursor) 1.dp else 0.5.dp,
                        color = if (isDraggingCursor) colors.accent else colors.cardBorder,
                        shape = CircleShape
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacingF.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selectionWeight = (1.0f - abs(safePos - index.toFloat())).coerceIn(0f, 1f)
                    val tabWidth = (collapsedWidthF + expandedExtraWidthF * selectionWeight).dp
                    val (selectedIcon, unselectedIcon) = getIslandTabIcons(tab)

                    FloatingIslandItem(
                        label = tab.label,
                        tabWidth = tabWidth,
                        selectionWeight = selectionWeight,
                        selectedIcon = selectedIcon,
                        unselectedIcon = unselectedIcon,
                        onClick = { onTabSelected(index, tab) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesFloatingButton(
    onClick: () -> Unit = {}
) {
    val colors = LocalOneUIColors.current
    Surface(
        shape = CircleShape,
        color = colors.islandBackground,
        border = BorderStroke(0.5.dp, colors.cardBorder),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        modifier = Modifier
            .size(54.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Preferiti",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FloatingIslandItem(
    label: String,
    tabWidth: Dp,
    selectionWeight: Float,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val interactionSource = remember { MutableInteractionSource() }

    val iconColor = androidx.compose.ui.graphics.lerp(
        colors.textSecondary,
        colors.textPrimary,
        selectionWeight
    )

    Box(
        modifier = Modifier
            .width(tabWidth)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selectionWeight > 0.5f) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier
                .size((18.5f + 1.5f * selectionWeight).dp)
                .offset(y = (-6.5f * selectionWeight).dp)
        )

        if (selectionWeight > 0.15f) {
            val textAlpha = ((selectionWeight - 0.15f) / 0.85f).coerceIn(0f, 1f)
            Text(
                text = label,
                color = colors.textPrimary.copy(alpha = textAlpha),
                fontSize = 9.5.sp,
                lineHeight = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .offset(y = 11.dp)
                    .basicMarquee()
            )
        }
    }
}

// =========================================================================
// CARD EXTRA "ARMADIO" QUANDO L'ARMADIO NON È NELL'ISOLA INFERIORE
// =========================================================================

@Composable
private fun WardrobeFunctionBannerCard(
    itemsCount: Int,
    closetsCount: Int,
    useDistinctColors: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val wardrobeTint = if (useDistinctColors) Color(0xFF14B8A6) else colors.accent

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, wardrobeTint.copy(alpha = 0.45f)),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(wardrobeTint.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checkroom,
                        contentDescription = "Armadio",
                        tint = wardrobeTint,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Armadio",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$itemsCount capi in $closetsCount armadi • Tocca per aprire",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = wardrobeTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// =========================================================================
// SCHERMATA PRINCIPALE (HOME SCREEN)
// =========================================================================

@Composable
fun HomeScreen(
    viewModel: ClosetViewModel,
    onAddItemClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val appVersionName = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkForStartupUpdates(appVersionName)
        viewModel.checkReleaseNotes(appVersionName)
    }

    val coroutineScope = rememberCoroutineScope()
    val (isGlobalTorchOn, toggleGlobalTorch) = rememberTorchState()

    val islandTabs = viewModel.islandTabs
    val maxPageIndex = (islandTabs.size - 1).coerceAtLeast(1)
    val homePageIndex = islandTabs.indexOf(IslandTabType.HOME).takeIf { it != -1 } ?: 0

    val pagerState = rememberPagerState(
        initialPage = homePageIndex,
        pageCount = { islandTabs.size.coerceAtLeast(2) }
    )

    val currentTab = islandTabs.getOrNull(pagerState.currentPage) ?: IslandTabType.HOME
    var isShowingSettings by remember { mutableStateOf(false) }
    var isReminderPopupOpen by remember { mutableStateOf(false) }

    // Stato per quando l'Armadio non è nell'isola ma viene aperto come funzione dentro "Funzioni"
    var isWardrobeOpenedAsSubFunction by remember { mutableStateOf(false) }
    var isWardrobeFunctionInfoOpen by remember { mutableStateOf(false) }

    LaunchedEffect(isWardrobeOpenedAsSubFunction) {
        if (!isWardrobeOpenedAsSubFunction) {
            isWardrobeFunctionInfoOpen = false
        }
    }

    val homeScrollState = rememberScrollState()
    var homeScrollToTopTrigger by remember { mutableIntStateOf(0) }
    var wardrobeScrollToTopTrigger by remember { mutableIntStateOf(0) }

    // Se riduci il numero di schede nell'isola, evita che il pager resti fuori range
    LaunchedEffect(islandTabs.size) {
        if (pagerState.currentPage >= islandTabs.size && islandTabs.isNotEmpty()) {
            pagerState.scrollToPage((islandTabs.size - 1).coerceAtLeast(0))
        }
        if (islandTabs.contains(IslandTabType.WARDROBE)) {
            isWardrobeOpenedAsSubFunction = false
        }
    }

    var isDraggingCursor by remember { mutableStateOf(false) }
    var dragStartPosition by remember { mutableFloatStateOf(homePageIndex.toFloat()) }
    var islandDragPosition by remember { mutableFloatStateOf(homePageIndex.toFloat()) }

    LaunchedEffect(islandDragPosition, isDraggingCursor, maxPageIndex) {
        if (isDraggingCursor) {
            val targetPage = islandDragPosition.roundToInt().coerceIn(0, maxPageIndex)
            val targetFraction = (islandDragPosition - targetPage).coerceIn(-0.5f, 0.5f)
            pagerState.scrollToPage(page = targetPage, pageOffsetFraction = targetFraction)
        }
    }

    val currentDisplayPosition by remember {
        derivedStateOf {
            if (isDraggingCursor) {
                islandDragPosition
            } else {
                (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, maxPageIndex.toFloat())
            }
        }
    }

    val pageBackStack = remember { mutableStateListOf(homePageIndex) }
    var isBackScrolling by remember { mutableStateOf(false) }

    var isTopMenuOpen by remember { mutableStateOf(false) }
    var isFavoriteIslandOpen by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, homePageIndex) {
        val current = pagerState.currentPage
        isTopMenuOpen = false
        isFavoriteIslandOpen = false
        if (!isDraggingCursor) {
            islandDragPosition = current.toFloat()
            dragStartPosition = current.toFloat()
        }
        if (isBackScrolling) {
            isBackScrolling = false
        } else {
            if (current == homePageIndex) {
                pageBackStack.clear()
                pageBackStack.add(homePageIndex)
            } else {
                if (pageBackStack.isEmpty() || pageBackStack.last() != current) {
                    pageBackStack.add(current)
                }
            }
        }
    }

    val currentTheme = viewModel.currentTheme
    val systemInDark = isSystemInDarkTheme()
    val isDarkEffective = remember(currentTheme, systemInDark) {
        when (currentTheme) {
            AppTheme.SYSTEM -> systemInDark
            AppTheme.LIGHT -> false
            AppTheme.DARK, AppTheme.AMOLED -> true
        }
    }

    val customAccent = viewModel.customAccentColor
    val effectiveAccent = remember(customAccent, isDarkEffective) {
        customAccent ?: if (isDarkEffective) Color(0xFF53B5A2) else Color(0xFF1B8A74)
    }
    val effectiveAccentContainer = remember(customAccent, isDarkEffective) {
        if (customAccent != null) {
            customAccent.copy(alpha = if (isDarkEffective) 0.22f else 0.16f)
        } else {
            if (isDarkEffective) Color(0xFF1B2E2A) else Color(0xFFD4EFE8)
        }
    }

    val themeColors = remember(currentTheme, isDarkEffective, effectiveAccent, effectiveAccentContainer) {
        when {
            currentTheme == AppTheme.AMOLED -> AmoledOneUIColors.copy(
                accent = effectiveAccent,
                accentContainer = effectiveAccentContainer
            )
            isDarkEffective -> DarkOneUIColors.copy(
                accent = effectiveAccent,
                accentContainer = effectiveAccentContainer
            )
            else -> LightOneUIColors.copy(
                accent = effectiveAccent,
                accentContainer = effectiveAccentContainer
            )
        }
    }

    var isEditMode by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showHomeOptionsDialog by remember { mutableStateOf(false) }
    var currentHomePagerPage by remember { mutableIntStateOf(0) }

    val today = remember { LocalDate.now() }

    var isFunctionEditMode by remember { mutableStateOf(false) }
    var isFunctionOptionsMenuOpen by remember { mutableStateOf(false) }
    var isFunctionsSubScreenActive by remember { mutableStateOf(false) }
    var originalFunctionOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    var targetFunctionSubScreen by remember { mutableStateOf<FunctionSubScreen?>(null) }

    var showAddClothingSheet by remember { mutableStateOf(false) }
    var showManageClosetsDialog by remember { mutableStateOf(false) }
    var showManageTagsDialog by remember { mutableStateOf(false) }
    var showIslandTabsEditorDialog by remember { mutableStateOf(false) }
    var showFavoriteEditorDialog by remember { mutableStateOf(false) }
    var isItemEditMode by remember { mutableStateOf(false) }
    var isItemEditAddMenuOpen by remember { mutableStateOf(false) }
    var showDeleteSelectedConfirmDialog by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }
    var isWardrobeSearchActive by remember { mutableStateOf(false) }
    var wardrobeSearchQuery by remember { mutableStateOf("") }
    var wardrobeFilterTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var targetWardrobeCloset by remember { mutableStateOf<String?>(null) }

    val selectedItemIds = remember { mutableStateListOf<String>() }
    val selectedItemQuantities = remember { mutableStateMapOf<String, Int>() }
    var onRequestCreateFolder by remember { mutableStateOf(false) }
    var onRequestMoveItems by remember { mutableStateOf(false) }

    var originalClothingState by remember { mutableStateOf<List<ClothingItem>>(emptyList()) }
    var originalFolderState by remember { mutableStateOf<List<ClosetFolder>>(emptyList()) }
    var originalWidgetsState by remember { mutableStateOf<List<PlacedWidget>>(emptyList()) }

    // Helper universale per navigare a qualsiasi scheda (se è nell'isola) o aprirla dentro "Funzioni" (se non è nell'isola)
    fun navigateToTabOrFunction(targetTab: IslandTabType) {
        val idxInIsland = islandTabs.indexOf(targetTab)
        if (idxInIsland != -1) {
            isWardrobeOpenedAsSubFunction = false
            targetFunctionSubScreen = null
            coroutineScope.launch { pagerState.animateScrollToPage(idxInIsland) }
        } else {
            val fnIdx = islandTabs.indexOf(IslandTabType.FUNCTIONS).coerceAtLeast(0)
            if (targetTab == IslandTabType.WARDROBE) {
                isWardrobeOpenedAsSubFunction = true
            } else {
                isWardrobeOpenedAsSubFunction = false
                targetFunctionSubScreen = targetTab.toFunctionSubScreenOrNull()
            }
            coroutineScope.launch { pagerState.animateScrollToPage(fnIdx) }
        }
    }

    LaunchedEffect(viewModel.pendingNavigationFunction) {
        val target = viewModel.pendingNavigationFunction ?: return@LaunchedEffect
        viewModel.pendingNavigationFunction = null
        when (target) {
            "SETTINGS" -> isShowingSettings = true
            "WARDROBE" -> navigateToTabOrFunction(IslandTabType.WARDROBE)
            "CALENDAR" -> navigateToTabOrFunction(IslandTabType.CALENDAR)
            "CLOSET_PHOTOS" -> navigateToTabOrFunction(IslandTabType.CLOSET_PHOTOS)
            "MOVE" -> navigateToTabOrFunction(IslandTabType.MOVE)
            "LAUNDRY" -> navigateToTabOrFunction(IslandTabType.LAUNDRY)
            "TRIP" -> navigateToTabOrFunction(IslandTabType.TRIP)
            "TRANSFERS_ACTIVITY" -> navigateToTabOrFunction(IslandTabType.TRANSFERS_ACTIVITY)
            "TOOLS" -> navigateToTabOrFunction(IslandTabType.TOOLS)
            else -> {}
        }
    }

    // Indica se in questo momento l'utente sta visualizzando l'Armadio (o come scheda dell'isola o come sotto-funzione)
    val isCurrentlyViewingWardrobe = (currentTab == IslandTabType.WARDROBE) ||
            (currentTab == IslandTabType.FUNCTIONS && isWardrobeOpenedAsSubFunction)

    LaunchedEffect(isFunctionEditMode) {
        if (isFunctionEditMode) {
            originalFunctionOrder = viewModel.functionOrder.toList()
        } else {
            isFunctionOptionsMenuOpen = false
        }
    }

    LaunchedEffect(isItemEditMode) {
        if (isItemEditMode) {
            originalClothingState = viewModel.clothingItems.toList()
            originalFolderState = viewModel.closetFolders.toList()
        } else {
            isItemEditAddMenuOpen = false
            if (isWardrobeSearchActive) {
                isWardrobeSearchActive = false
                wardrobeSearchQuery = ""
                wardrobeFilterTags = emptySet()
            }
        }
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            originalWidgetsState = viewModel.homeWidgets.toList()
        }
    }

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    val isAnyEditModeActive = isEditMode || isItemEditMode || isFunctionEditMode
    val homeWidgets = viewModel.homeWidgets

    val isInFunctionsGridSubScreen = (currentTab == IslandTabType.FUNCTIONS && isFunctionsSubScreenActive)

    val shouldInterceptBack = !isInFunctionsGridSubScreen && (
            showHomeOptionsDialog ||
            isCameraActive ||
                    showDeleteSelectedConfirmDialog ||
                    isWardrobeFunctionInfoOpen ||
                    isItemEditAddMenuOpen ||
                    isFunctionOptionsMenuOpen ||
                    isFavoriteIslandOpen ||
                    isTopMenuOpen ||
                    isItemEditMode ||
                    isFunctionEditMode ||
                    isEditMode ||
                    isShowingSettings ||
                    isWardrobeSearchActive ||
                    isWardrobeOpenedAsSubFunction ||
                    pagerState.currentPage != homePageIndex ||
                    pageBackStack.size > 1
            )

    BackHandler(enabled = shouldInterceptBack) {
        when {
            showHomeOptionsDialog -> showHomeOptionsDialog = false
            isCameraActive -> isCameraActive = false
            isWardrobeFunctionInfoOpen -> isWardrobeFunctionInfoOpen = false
            showDeleteSelectedConfirmDialog -> showDeleteSelectedConfirmDialog = false
            isItemEditAddMenuOpen -> isItemEditAddMenuOpen = false
            isFunctionOptionsMenuOpen -> isFunctionOptionsMenuOpen = false
            isFavoriteIslandOpen -> isFavoriteIslandOpen = false
            isTopMenuOpen -> isTopMenuOpen = false
            isWardrobeSearchActive -> {
                isWardrobeSearchActive = false
                wardrobeSearchQuery = ""
                wardrobeFilterTags = emptySet()
            }
            isItemEditMode -> {
                val hasChanges = viewModel.clothingItems.toList() != originalClothingState ||
                        viewModel.closetFolders.toList() != originalFolderState
                if (hasChanges) {
                    showDiscardConfirmDialog = true
                } else {
                    selectedItemIds.clear()
                    selectedItemQuantities.clear()
                    isItemEditMode = false
                    isItemEditAddMenuOpen = false
                }
            }
            isFunctionEditMode -> {
                val hasChanges = viewModel.functionOrder.toList() != originalFunctionOrder
                if (hasChanges) {
                    showDiscardConfirmDialog = true
                } else {
                    isFunctionEditMode = false
                    isFunctionOptionsMenuOpen = false
                }
            }
            isEditMode -> {
                val hasChanges = viewModel.homeWidgets.toList() != originalWidgetsState
                if (hasChanges) {
                    showDiscardConfirmDialog = true
                } else {
                    isEditMode = false
                }
            }
            isShowingSettings -> isShowingSettings = false
            isWardrobeOpenedAsSubFunction -> {
                isWardrobeOpenedAsSubFunction = false
            }
            pageBackStack.size > 1 -> {
                pageBackStack.removeAt(pageBackStack.lastIndex)
                val prevPage = pageBackStack.last().coerceIn(0, maxPageIndex)
                isBackScrolling = true
                coroutineScope.launch { pagerState.animateScrollToPage(prevPage) }
            }
            pagerState.currentPage != homePageIndex -> {
                pageBackStack.clear()
                pageBackStack.add(homePageIndex)
                isBackScrolling = true
                coroutineScope.launch { pagerState.animateScrollToPage(homePageIndex) }
            }
        }
    }

    CompositionLocalProvider(LocalOneUIColors provides themeColors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.backgroundBrush)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            if (isItemEditMode && isItemEditAddMenuOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(28f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isItemEditAddMenuOpen = false }
                )
            }

            if (isFunctionEditMode && isFunctionOptionsMenuOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(28f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isFunctionOptionsMenuOpen = false }
                )
            }

            if (isFavoriteIslandOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(28f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isFavoriteIslandOpen = false }
                )
            }

            if (isTopMenuOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(24f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isTopMenuOpen = false }
                )
            }

            if (isShowingSettings) {
                SettingsScreen(
                    currentTheme = currentTheme,
                    onThemeChange = { newTheme -> viewModel.setTheme(newTheme) },
                    currentAccentColor = effectiveAccent,
                    isCustomAccent = (viewModel.customAccentColor != null),
                    onAccentColorChange = { newColor -> viewModel.setCustomAccentColor(newColor) },
                    onResetAccentColor = { viewModel.resetCustomAccentColor() },
                    useDistinctFunctionColors = viewModel.useDistinctFunctionColors,
                    onToggleDistinctFunctionColors = { viewModel.setDistinctFunctionColors(it) },
                    notificationsSoundEnabled = viewModel.notificationsSoundEnabled,
                    onToggleNotificationsSound = { viewModel.setNotificationsSound(it) },
                    notificationsVibrationEnabled = viewModel.notificationsVibrationEnabled,
                    onToggleNotificationsVibration = { viewModel.setNotificationsVibration(it) },
                    autoCheckUpdatesOnStartup = viewModel.autoCheckUpdatesOnStartup,
                    onToggleAutoCheckUpdates = { viewModel.updateAutoCheckUpdatesOnStartup(it) },
                    islandTabs = viewModel.islandTabs,
                    onSaveIslandTabs = { viewModel.updateIslandTabs(it) },
                    favoriteShortcuts = viewModel.favoriteShortcuts,
                    onSaveFavoriteShortcuts = { viewModel.updateFavoriteShortcuts(it) },
                    closetSwipeChangeEnabled = viewModel.isClosetSwipeChangeEnabled,
                    onToggleClosetSwipeChange = { viewModel.updateClosetSwipeChangeEnabled(it) },
                    closetSwipeDirection = viewModel.closetSwipeDirection,
                    onSelectClosetSwipeDirection = { viewModel.updateClosetSwipeDirection(it) },
                    onOpenReleaseNotes = { viewModel.openReleaseNotesManually() },
                    onClearAllData = { viewModel.clearAllData() },
                    onResetSettings = { viewModel.resetSettingsToDefault() },
                    onBack = { isShowingSettings = false }
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isAnyEditModeActive &&
                            !isCameraActive &&
                            !isWardrobeSearchActive &&
                            !isInFunctionsGridSubScreen &&
                            !isWardrobeOpenedAsSubFunction &&
                            !isDraggingCursor
                ) { page ->
                    val tabForPage = islandTabs.getOrNull(page) ?: IslandTabType.HOME

                    when (tabForPage) {
                        IslandTabType.FUNCTIONS -> {
                            val isWardrobeInIsland = islandTabs.contains(IslandTabType.WARDROBE)
                            val showWardrobeAsSub = isWardrobeOpenedAsSubFunction && !isWardrobeInIsland

                            AnimatedContent(
                                targetState = showWardrobeAsSub,
                                transitionSpec = {
                                    if (targetState) {
                                        (fadeIn(animationSpec = tween(220, delayMillis = 30)) + scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                                            .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.96f))
                                    } else {
                                        (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.96f))
                                            .togetherWith(fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.96f))
                                    }
                                },
                                label = "functionsWardrobeTransition",
                                modifier = Modifier.fillMaxSize()
                            ) { isWardrobeActive ->
                                if (isWardrobeActive) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Barra di ritorno alla schermata Funzioni
                                            if (!isItemEditMode && !isCameraActive) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = themeColors.surfaceSecondary,
                                                        border = BorderStroke(0.5.dp, themeColors.cardBorder),
                                                        tonalElevation = 4.dp,
                                                        shadowElevation = 4.dp,
                                                        modifier = Modifier.clickable {
                                                            isWardrobeOpenedAsSubFunction = false
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                                contentDescription = "Torna a Funzioni",
                                                                tint = themeColors.accent,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = "Funzioni",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = themeColors.textPrimary
                                                            )
                                                        }
                                                    }

                                                    FunctionInfoButtonWithPopup(
                                                        title = "Armadio",
                                                        description = "Organizza, cerca e visualizza tutti i tuoi capi per armadio e cartelle",
                                                        accentColor = if (viewModel.useDistinctFunctionColors) Color(0xFF14B8A6) else themeColors.accent,
                                                        isOpen = isWardrobeFunctionInfoOpen,
                                                        onToggleOpen = { isWardrobeFunctionInfoOpen = !isWardrobeFunctionInfoOpen },
                                                        buttonSize = 34.dp
                                                    )
                                                }
                                            }

                                            Box(modifier = Modifier.weight(1f)) {
                                                WardrobeScreen(
                                                    viewModel = viewModel,
                                                    showAddItemSheet = showAddClothingSheet,
                                                    onDismissAddItemSheet = { showAddClothingSheet = false },
                                                    showManageClosetsDialog = showManageClosetsDialog,
                                                    onDismissManageClosets = { showManageClosetsDialog = false },
                                                    onOpenManageClosets = { showManageClosetsDialog = true },
                                                    showManageTagsDialog = showManageTagsDialog,
                                                    onDismissManageTags = { showManageTagsDialog = false },
                                                    isItemEditMode = isItemEditMode,
                                                    onEnterItemEditMode = { isItemEditMode = true },
                                                    onExitItemEditMode = {
                                                        selectedItemIds.clear()
                                                        selectedItemQuantities.clear()
                                                        isItemEditMode = false
                                                    },
                                                    selectedItemIds = selectedItemIds,
                                                    selectedItemQuantities = selectedItemQuantities,
                                                    triggerCreateFolder = onRequestCreateFolder,
                                                    onFolderDialogConsumed = { onRequestCreateFolder = false },
                                                    triggerMoveItems = onRequestMoveItems,
                                                    onMoveDialogConsumed = { onRequestMoveItems = false },
                                                    onCameraActiveChange = { active: Boolean -> isCameraActive = active },
                                                    isSearchActive = isWardrobeSearchActive,
                                                    onSearchActiveChange = { isWardrobeSearchActive = it },
                                                    targetWardrobeCloset = targetWardrobeCloset,
                                                    onTargetWardrobeClosetConsumed = { targetWardrobeCloset = null },
                                                    searchQuery = wardrobeSearchQuery,
                                                    filterTags = wardrobeFilterTags,
                                                    scrollToTopTrigger = wardrobeScrollToTopTrigger
                                                )
                                            }
                                        }

                                        if (isWardrobeFunctionInfoOpen) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .zIndex(28f)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { isWardrobeFunctionInfoOpen = false }
                                            )
                                        }
                                    }
                                } else {
                                    FunctionsScreen(
                                        viewModel = viewModel,
                                        isFunctionEditMode = isFunctionEditMode,
                                        initialSubScreen = targetFunctionSubScreen,
                                        onSubScreenConsumed = { targetFunctionSubScreen = null },
                                        onEnterFunctionEditMode = { isFunctionEditMode = true },
                                        onSubScreenActiveChange = { isFunctionsSubScreenActive = it },
                                        onOpenWardrobeAsFunction = { isWardrobeOpenedAsSubFunction = true }
                                    )
                                }
                            }
                        }

                        IslandTabType.WARDROBE -> {
                            WardrobeScreen(
                                viewModel = viewModel,
                                showAddItemSheet = showAddClothingSheet,
                                onDismissAddItemSheet = { showAddClothingSheet = false },
                                showManageClosetsDialog = showManageClosetsDialog,
                                onDismissManageClosets = { showManageClosetsDialog = false },
                                onOpenManageClosets = { showManageClosetsDialog = true },
                                showManageTagsDialog = showManageTagsDialog,
                                onDismissManageTags = { showManageTagsDialog = false },
                                isItemEditMode = isItemEditMode,
                                onEnterItemEditMode = { isItemEditMode = true },
                                onExitItemEditMode = {
                                    selectedItemIds.clear()
                                    selectedItemQuantities.clear()
                                    isItemEditMode = false
                                },
                                selectedItemIds = selectedItemIds,
                                selectedItemQuantities = selectedItemQuantities,
                                triggerCreateFolder = onRequestCreateFolder,
                                onFolderDialogConsumed = { onRequestCreateFolder = false },
                                triggerMoveItems = onRequestMoveItems,
                                onMoveDialogConsumed = { onRequestMoveItems = false },
                                onCameraActiveChange = { active: Boolean -> isCameraActive = active },
                                isSearchActive = isWardrobeSearchActive,
                                onSearchActiveChange = { isWardrobeSearchActive = it },
                                targetWardrobeCloset = targetWardrobeCloset,
                                onTargetWardrobeClosetConsumed = { targetWardrobeCloset = null },
                                searchQuery = wardrobeSearchQuery,
                                filterTags = wardrobeFilterTags,
                                scrollToTopTrigger = wardrobeScrollToTopTrigger
                            )
                        }

                        IslandTabType.HOME -> {
                            HomeDashboardGrid(
                                viewModel = viewModel,
                                widgets = homeWidgets,
                                useDistinctColors = viewModel.useDistinctFunctionColors,
                                isEditMode = isEditMode,
                                onEnterEditMode = { isEditMode = true },
                                scrollState = homeScrollState,
                                scrollToTopTrigger = homeScrollToTopTrigger,
                                onPageChanged = { page -> currentHomePagerPage = page },
                                onRemoveWidget = { widgetId: String -> viewModel.removeWidget(widgetId) },
                                onMoveWidget = { fromIdx: Int, toIdx: Int -> viewModel.moveWidget(fromIdx, toIdx) },
                                onUpdateWidgetsLayout = { updatedList ->
                                    viewModel.updateWidgetsLayout(updatedList)
                                },
                                onNavigateFunction = { targetSubScreen ->
                                    navigateToTabOrFunction(targetSubScreen.toIslandTabType())
                                },
                                onWidgetClick = { widget ->
                                    if (!isEditMode) {
                                        when (widget.type) {
                                            WidgetType.CALENDAR_TODAY_1X1,
                                            WidgetType.CALENDAR_TODAY_2X1,
                                            WidgetType.CALENDAR_TODAY_2X2,
                                            WidgetType.CALENDAR_MONTH_2X2,
                                            WidgetType.CALENDAR_AGENDA_4X2,
                                            WidgetType.CALENDAR_ADD_EVENT_1X1,
                                            WidgetType.CALENDAR_ADD_EVENT_1X2,
                                            WidgetType.FUNCTION_CALENDAR_1X1 -> {
                                                navigateToTabOrFunction(IslandTabType.CALENDAR)
                                            }
                                            WidgetType.FUNCTION_TOOLS_1X1,
                                            WidgetType.CLOCK_DIGITAL_1X2,
                                            WidgetType.CLOCK_DIGITAL_2X2,
                                            WidgetType.CLOCK_DIGITAL_2X4,
                                            WidgetType.CLOCK_ANALOG_1X1,
                                            WidgetType.CLOCK_ANALOG_2X2,
                                            WidgetType.CLOCK_ANALOG_2X4 -> {
                                                navigateToTabOrFunction(IslandTabType.TOOLS)
                                            }
                                            WidgetType.WARDROBE_COUNT_1X1,
                                            WidgetType.WARDROBE_COUNT_1X2,
                                            WidgetType.WARDROBE_COUNT_2X2,
                                            WidgetType.WARDROBE_COUNT_2X4,
                                            WidgetType.WARDROBE_LIST_1X4,
                                            WidgetType.WARDROBE_LIST_2X2,
                                            WidgetType.WARDROBE_LIST_2X4 -> {
                                                if (widget.targetCloset != null) {
                                                    targetWardrobeCloset = widget.targetCloset
                                                }
                                                navigateToTabOrFunction(IslandTabType.WARDROBE)
                                            }
                                            WidgetType.WARDROBE_SEARCH_1X1,
                                            WidgetType.WARDROBE_SEARCH_1X2 -> {
                                                navigateToTabOrFunction(IslandTabType.WARDROBE)
                                                isWardrobeSearchActive = true
                                            }
                                            WidgetType.WARDROBE_ADD_ITEM_1X1,
                                            WidgetType.WARDROBE_ADD_ITEM_1X2,
                                            WidgetType.WARDROBE_ADD_ITEM_2X2 -> {
                                                navigateToTabOrFunction(IslandTabType.WARDROBE)
                                                showAddClothingSheet = true
                                            }
                                            WidgetType.FUNCTION_QUICK_ACTIONS_1X4,
                                            WidgetType.FUNCTION_QUICK_ACTIONS_2X2 -> {
                                                navigateToTabOrFunction(IslandTabType.FUNCTIONS)
                                            }
                                            WidgetType.FUNCTION_PHOTO_1X1,
                                            WidgetType.FUNCTION_PHOTO_2X2,
                                            WidgetType.FUNCTION_PHOTO_2X4 -> {
                                                navigateToTabOrFunction(IslandTabType.CLOSET_PHOTOS)
                                            }
                                            WidgetType.FUNCTION_MOVE_1X1,
                                            WidgetType.FUNCTION_MOVE_1X2,
                                            WidgetType.FUNCTION_MOVE_2X2 -> {
                                                navigateToTabOrFunction(IslandTabType.MOVE)
                                            }
                                            WidgetType.FUNCTION_LAUNDRY_1X1,
                                            WidgetType.FUNCTION_LAUNDRY_1X2,
                                            WidgetType.FUNCTION_LAUNDRY_2X2,
                                            WidgetType.FUNCTION_LAUNDRY_2X4 -> {
                                                navigateToTabOrFunction(IslandTabType.LAUNDRY)
                                            }
                                            WidgetType.FUNCTION_TRIP_1X1,
                                            WidgetType.FUNCTION_TRIP_1X2,
                                            WidgetType.FUNCTION_TRIP_2X2,
                                            WidgetType.FUNCTION_TRIP_2X4 -> {
                                                navigateToTabOrFunction(IslandTabType.TRIP)
                                            }
                                            WidgetType.FUNCTION_TRANSFERS_1X1,
                                            WidgetType.FUNCTION_TRANSFERS_1X2,
                                            WidgetType.FUNCTION_TRANSFERS_2X2 -> {
                                                navigateToTabOrFunction(IslandTabType.TRANSFERS_ACTIVITY)
                                            }
                                            WidgetType.UTILITY_TORCH_1X1,
                                            WidgetType.UTILITY_TORCH_1X2,
                                            WidgetType.UTILITY_TORCH_2X2,
                                            WidgetType.UTILITY_COMPASS_1X1,
                                            WidgetType.UTILITY_COMPASS_1X2,
                                            WidgetType.UTILITY_COMPASS_2X2,
                                            WidgetType.UTILITY_COMPASS_2X4,
                                            WidgetType.UTILITY_LEVEL_1X1,
                                            WidgetType.UTILITY_LEVEL_1X2,
                                            WidgetType.UTILITY_LEVEL_2X2,
                                            WidgetType.UTILITY_LEVEL_2X4 -> {
                                                // Gestiti direttamente dal widget stesso
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // Qualsiasi altra funzione posizionata direttamente come scheda nell'isola inferiore
                        else -> {
                            when (tabForPage) {
                                IslandTabType.CALENDAR -> CalendarScreen(viewModel = viewModel, onBack = {})
                                IslandTabType.CLOSET_PHOTOS -> ClosetPhotosScreen(viewModel = viewModel, onBack = {})
                                IslandTabType.MOVE -> MoveClothingScreen(viewModel = viewModel, isLaundryMode = false, onBack = {})
                                IslandTabType.LAUNDRY -> MoveClothingScreen(viewModel = viewModel, isLaundryMode = true, onBack = {})
                                IslandTabType.TRIP -> TripScreen(viewModel = viewModel, onBack = {})
                                IslandTabType.TRANSFERS_ACTIVITY -> TransfersActivityScreen(
                                    viewModel = viewModel,
                                    onBack = {},
                                    onNavigateToLaundry = { navigateToTabOrFunction(IslandTabType.LAUNDRY) }
                                )
                                IslandTabType.TOOLS -> ToolsScreen(viewModel = viewModel, onBack = {})
                                else -> {}
                            }
                        }
                    }
                }
            }

            // Top action island in alto a destra
            val showTopActionIsland = !isShowingSettings && !isCameraActive && !isInFunctionsGridSubScreen &&
                    !isItemEditMode && !isFunctionEditMode && !isEditMode &&
                    !(isCurrentlyViewingWardrobe && isWardrobeSearchActive)

            AnimatedVisibility(
                visible = showTopActionIsland,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp, top = 16.dp)
                    .zIndex(25f)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    OneUITopActionIsland(
                        showSearchButton = isCurrentlyViewingWardrobe,
                        isSearchActive = isWardrobeSearchActive,
                        onSearchClick = {
                            isWardrobeSearchActive = !isWardrobeSearchActive
                            if (!isWardrobeSearchActive) {
                                wardrobeSearchQuery = ""
                                wardrobeFilterTags = emptySet()
                            }
                        },
                        showTodayButton = false,
                        todayDayNumber = today.dayOfMonth.toString(),
                        hasActiveReminders = viewModel.reminders.any { it.isEnabled },
                        onReminderClick = { isReminderPopupOpen = true },
                        isMoreMenuOpen = isTopMenuOpen,
                        onMoreOptionsClick = { isTopMenuOpen = !isTopMenuOpen }
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isTopMenuOpen,
                        enter = fadeIn(tween(180)) + scaleIn(
                            initialScale = 0.4f,
                            transformOrigin = TransformOrigin(1f, 0f),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + slideInVertically(
                            initialOffsetY = { -it / 3 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut(tween(130)) + scaleOut(
                            targetScale = 0.4f,
                            transformOrigin = TransformOrigin(1f, 0f)
                        ) + slideOutVertically(targetOffsetY = { -it / 3 }),
                        modifier = Modifier
                            .padding(top = 48.dp)
                            .width(195.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = themeColors.dialogBackground,
                            border = BorderStroke(0.5.dp, themeColors.cardBorder),
                            tonalElevation = 8.dp,
                            shadowElevation = 16.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                when {
                                    isCurrentlyViewingWardrobe -> {
                                        TopActionMenuItem(
                                            icon = Icons.Default.Add,
                                            iconTint = themeColors.accent,
                                            title = "Aggiungi capo",
                                            onClick = {
                                                isTopMenuOpen = false
                                                showAddClothingSheet = true
                                            }
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.Edit,
                                            iconTint = themeColors.textSecondary,
                                            title = "Gestione elementi",
                                            onClick = {
                                                isTopMenuOpen = false
                                                isItemEditMode = true
                                            }
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.MeetingRoom,
                                            iconTint = themeColors.textSecondary,
                                            title = "Gestione armadi",
                                            onClick = {
                                                isTopMenuOpen = false
                                                showManageClosetsDialog = true
                                            }
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.Label,
                                            iconTint = themeColors.accent,
                                            title = "Gestione tag",
                                            onClick = {
                                                isTopMenuOpen = false
                                                showManageTagsDialog = true
                                            }
                                        )
                                        HorizontalDivider(
                                            color = themeColors.cardBorder,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.Settings,
                                            iconTint = themeColors.textSecondary,
                                            title = "Impostazioni",
                                            onClick = {
                                                isTopMenuOpen = false
                                                isShowingSettings = true
                                            }
                                        )
                                    }

                                    currentTab == IslandTabType.FUNCTIONS -> {
                                        TopActionMenuItem(
                                            icon = Icons.Default.DashboardCustomize,
                                            iconTint = themeColors.accent,
                                            title = "Gestione funzioni",
                                            onClick = {
                                                isTopMenuOpen = false
                                                isFunctionEditMode = true
                                            }
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.ViewAgenda,
                                            iconTint = themeColors.accent,
                                            title = "Gestione schede",
                                            onClick = {
                                                isTopMenuOpen = false
                                                showIslandTabsEditorDialog = true
                                            }
                                        )
                                        HorizontalDivider(
                                            color = themeColors.cardBorder,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.Settings,
                                            iconTint = themeColors.textSecondary,
                                            title = "Impostazioni",
                                            onClick = {
                                                isTopMenuOpen = false
                                                isShowingSettings = true
                                            }
                                        )
                                    }

                                    currentTab == IslandTabType.HOME -> {
                                        TopActionMenuItem(
                                            icon = Icons.Outlined.Tune,
                                            iconTint = themeColors.accent,
                                            title = "Opzioni home",
                                            onClick = {
                                                isTopMenuOpen = false
                                                showHomeOptionsDialog = true
                                            }
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.DashboardCustomize,
                                            iconTint = themeColors.accent,
                                            title = if (isEditMode) "Termina modifica" else "Modifica home",
                                            onClick = {
                                                isTopMenuOpen = false
                                                isEditMode = !isEditMode
                                            }
                                        )
                                        HorizontalDivider(
                                            color = themeColors.cardBorder,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.Star,
                                            iconTint = themeColors.accent,
                                            title = "Gestione funzioni preferite",
                                            onClick = {
                                                isTopMenuOpen = false
                                                showFavoriteEditorDialog = true
                                            }
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.ViewAgenda,
                                            iconTint = themeColors.accent,
                                            title = "Gestione schede",
                                            onClick = {
                                                isTopMenuOpen = false
                                                showIslandTabsEditorDialog = true
                                            }
                                        )
                                        HorizontalDivider(
                                            color = themeColors.cardBorder,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        TopActionMenuItem(
                                            icon = Icons.Default.Settings,
                                            iconTint = themeColors.textSecondary,
                                            title = "Impostazioni",
                                            onClick = {
                                                isTopMenuOpen = false
                                                isShowingSettings = true
                                            }
                                        )
                                    }

                                    else -> {
                                        TopActionMenuItem(
                                            icon = Icons.Default.Settings,
                                            iconTint = themeColors.textSecondary,
                                            title = "Impostazioni",
                                            onClick = {
                                                isTopMenuOpen = false
                                                isShowingSettings = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Isola di Navigazione inferiore flottante
            if (!isShowingSettings && !isCameraActive && !isInFunctionsGridSubScreen) {
                val showBottomIsland = !(isCurrentlyViewingWardrobe && isWardrobeSearchActive)
                val islandMode = when {
                    isItemEditMode -> BottomIslandMode.ITEM_EDIT
                    isFunctionEditMode -> BottomIslandMode.FUNCTION_EDIT
                    isEditMode -> BottomIslandMode.WIDGET_EDIT
                    else -> BottomIslandMode.NORMAL
                }

                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val islandBottomPadding = when {
                    navBarBottom == 0.dp -> 14.dp
                    navBarBottom <= 24.dp -> 6.dp
                    else -> 8.dp
                }
                val popupBottomPadding = islandBottomPadding + 54.dp + 6.dp

                AnimatedVisibility(
                    visible = showBottomIsland,
                    enter = fadeIn(animationSpec = tween(280)) + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut(animationSpec = tween(280)) + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = islandBottomPadding)
                        .zIndex(30f)
                ) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = themeColors.islandBackground,
                                border = BorderStroke(0.5.dp, themeColors.cardBorder),
                                tonalElevation = 8.dp,
                                shadowElevation = 16.dp,
                                modifier = Modifier
                                    .height(54.dp)
                                    .wrapContentWidth()
                            ) {
                                AnimatedContent(
                                    targetState = islandMode,
                                    contentAlignment = Alignment.Center,
                                    transitionSpec = {
                                        (fadeIn(animationSpec = tween(160, delayMillis = 40)) + scaleIn(initialScale = 0.94f))
                                            .togetherWith(fadeOut(animationSpec = tween(110)) + scaleOut(targetScale = 0.94f))
                                            .using(
                                                SizeTransform(
                                                    clip = false,
                                                    sizeAnimationSpec = { _, _ ->
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    }
                                                )
                                            )
                                    },
                                    label = "islandPillMorph"
                                ) { mode ->
                                    when (mode) {
                                        BottomIslandMode.ITEM_EDIT -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color.Transparent,
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clickable {
                                                            val hasChanges = viewModel.clothingItems.toList() != originalClothingState ||
                                                                    viewModel.closetFolders.toList() != originalFolderState
                                                            if (hasChanges) {
                                                                showDiscardConfirmDialog = true
                                                            } else {
                                                                selectedItemIds.clear()
                                                                selectedItemQuantities.clear()
                                                                isItemEditMode = false
                                                                isItemEditAddMenuOpen = false
                                                            }
                                                        }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = if (selectedItemIds.isEmpty()) 10.dp else 6.dp)
                                                    ) {
                                                        AnimatedContent(
                                                            targetState = selectedItemIds.isNotEmpty(),
                                                            transitionSpec = {
                                                                (fadeIn(tween(200)) + scaleIn(initialScale = 0.8f)) togetherWith
                                                                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.8f))
                                                            },
                                                            label = "annullaMorph"
                                                        ) { hasSelection ->
                                                            if (hasSelection) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Annulla",
                                                                    tint = Color(0xFFE55353),
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = "Annulla",
                                                                    color = Color(0xFFE55353),
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = selectedItemIds.isNotEmpty(),
                                                    enter = fadeIn(tween(200)) + expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)),
                                                    exit = fadeOut(tween(150)) + shrinkHorizontally(spring(stiffness = Spring.StiffnessMediumLow))
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Tasto Sposta (Solo icona)
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = themeColors.accentContainer,
                                                            border = BorderStroke(0.5.dp, themeColors.accent.copy(alpha = 0.4f)),
                                                            modifier = Modifier
                                                                .size(38.dp)
                                                                .clickable { onRequestMoveItems = true }
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                                                    contentDescription = "Sposta",
                                                                    tint = themeColors.accent,
                                                                    modifier = Modifier.size(19.dp)
                                                                )
                                                            }
                                                        }

                                                        // Tasto Elimina (Solo icona)
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color(0xFFE55353).copy(alpha = 0.15f),
                                                            border = BorderStroke(0.5.dp, Color(0xFFE55353).copy(alpha = 0.4f)),
                                                            modifier = Modifier
                                                                .size(38.dp)
                                                                .clickable { showDeleteSelectedConfirmDialog = true }
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Delete,
                                                                    contentDescription = "Elimina selezionati",
                                                                    tint = Color(0xFFE55353),
                                                                    modifier = Modifier.size(19.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Tasto Cerca evidenziato solo icona (a destra del tasto Elimina quando ci sono elementi selezionati)
                                                Surface(
                                                    shape = CircleShape,
                                                    color = themeColors.accent.copy(alpha = 0.14f),
                                                    border = BorderStroke(0.5.dp, themeColors.accent.copy(alpha = 0.35f)),
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clickable {
                                                            isItemEditAddMenuOpen = false
                                                            isWardrobeSearchActive = true
                                                        }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Search,
                                                            contentDescription = "Cerca",
                                                            tint = themeColors.accent,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = themeColors.accent,
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clickable {
                                                            selectedItemIds.clear()
                                                            isItemEditMode = false
                                                            isItemEditAddMenuOpen = false
                                                        }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = if (selectedItemIds.isEmpty()) 14.dp else 10.dp)
                                                    ) {
                                                        AnimatedContent(
                                                            targetState = selectedItemIds.isNotEmpty(),
                                                            transitionSpec = {
                                                                (fadeIn(tween(200)) + scaleIn(initialScale = 0.8f)) togetherWith
                                                                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.8f))
                                                            },
                                                            label = "fineMorph"
                                                        ) { hasSelection ->
                                                            if (hasSelection) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Fine",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = "Fine",
                                                                    color = Color.White,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        BottomIslandMode.FUNCTION_EDIT -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color.Transparent,
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clickable {
                                                            isFunctionOptionsMenuOpen = false
                                                            val hasChanges = viewModel.functionOrder.toList() != originalFunctionOrder
                                                            if (hasChanges) {
                                                                showDiscardConfirmDialog = true
                                                            } else {
                                                                isFunctionEditMode = false
                                                            }
                                                        }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = 14.dp)
                                                    ) {
                                                        Text(
                                                            text = "Annulla",
                                                            color = Color(0xFFE55353),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = themeColors.accent,
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clickable {
                                                            isFunctionOptionsMenuOpen = false
                                                            isFunctionEditMode = false
                                                        }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = 18.dp)
                                                    ) {
                                                        Text(
                                                            text = "Fine",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        BottomIslandMode.WIDGET_EDIT -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color.Transparent,
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clickable {
                                                            val hasChanges = viewModel.homeWidgets.toList() != originalWidgetsState
                                                            if (hasChanges) {
                                                                showDiscardConfirmDialog = true
                                                            } else {
                                                                isEditMode = false
                                                            }
                                                        }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = 14.dp)
                                                    ) {
                                                        Text(
                                                            text = "Annulla",
                                                            color = Color(0xFFE55353),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = themeColors.surfaceSecondary,
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clickable { showHomeOptionsDialog = true }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Tune,
                                                            contentDescription = "Opzioni home",
                                                            tint = themeColors.textPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = themeColors.accent,
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .clickable { isEditMode = false }
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = 18.dp)
                                                    ) {
                                                        Text(
                                                            text = "Fine",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        BottomIslandMode.NORMAL -> {
                                            OneUIFloatingIsland(
                                                tabs = islandTabs,
                                                currentPosition = currentDisplayPosition,
                                                isDraggingCursor = isDraggingCursor,
                                                onTabSelected = { targetIndex, targetTab ->
                                                    if (targetIndex != pagerState.currentPage) {
                                                        islandDragPosition = targetIndex.toFloat()
                                                        dragStartPosition = targetIndex.toFloat()
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(targetIndex)
                                                        }
                                                    } else {
                                                        when (targetTab) {
                                                            IslandTabType.HOME -> {
                                                                homeScrollToTopTrigger++
                                                                if (homeScrollState.value > 0) {
                                                                    coroutineScope.launch {
                                                                        homeScrollState.animateScrollTo(0)
                                                                    }
                                                                }
                                                            }
                                                            IslandTabType.WARDROBE -> {
                                                                wardrobeScrollToTopTrigger++
                                                            }
                                                            IslandTabType.FUNCTIONS -> {
                                                                if (isWardrobeOpenedAsSubFunction) {
                                                                    isWardrobeOpenedAsSubFunction = false
                                                                }
                                                            }
                                                            else -> {}
                                                        }
                                                    }
                                                },
                                                onDragStart = {
                                                    isDraggingCursor = true
                                                    dragStartPosition = currentDisplayPosition
                                                    islandDragPosition = dragStartPosition
                                                },
                                                onDragDelta = { deltaPages ->
                                                    islandDragPosition = (dragStartPosition + deltaPages)
                                                        .coerceIn(0f, maxPageIndex.toFloat())
                                                },
                                                onDragEnd = {
                                                    if (isDraggingCursor) {
                                                        val targetPage = islandDragPosition.roundToInt().coerceIn(0, maxPageIndex)
                                                        isDraggingCursor = false
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(
                                                                page = targetPage,
                                                                animationSpec = spring(
                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                    stiffness = Spring.StiffnessMediumLow
                                                                )
                                                            )
                                                        }
                                                    }
                                                },
                                                onDragCancel = {
                                                    if (isDraggingCursor) {
                                                        val targetPage = islandDragPosition.roundToInt().coerceIn(0, maxPageIndex)
                                                        isDraggingCursor = false
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(
                                                                page = targetPage,
                                                                animationSpec = spring(
                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                    stiffness = Spring.StiffnessMediumLow
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Modulo Destro circolare 54.dp (Stella Preferiti, +, Palette o + per i widget)
                            val isRightButtonActive = (isItemEditMode && isItemEditAddMenuOpen) ||
                                    (isFunctionEditMode && isFunctionOptionsMenuOpen) ||
                                    (islandMode == BottomIslandMode.NORMAL && isFavoriteIslandOpen)

                            val plusRotation by animateFloatAsState(
                                targetValue = if (isItemEditMode && isItemEditAddMenuOpen) 45f else 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "plusMorphRotation"
                            )

                            Surface(
                                shape = CircleShape,
                                color = if (isRightButtonActive) themeColors.accentContainer else themeColors.islandBackground,
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isRightButtonActive) themeColors.accent else themeColors.cardBorder
                                ),
                                tonalElevation = 8.dp,
                                shadowElevation = 16.dp,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable {
                                        when (islandMode) {
                                            BottomIslandMode.ITEM_EDIT -> {
                                                isItemEditAddMenuOpen = !isItemEditAddMenuOpen
                                            }
                                            BottomIslandMode.FUNCTION_EDIT -> {
                                                isFunctionOptionsMenuOpen = !isFunctionOptionsMenuOpen
                                            }
                                            BottomIslandMode.WIDGET_EDIT -> {
                                                showWidgetPicker = true
                                            }
                                            BottomIslandMode.NORMAL -> {
                                                isFavoriteIslandOpen = !isFavoriteIslandOpen
                                            }
                                        }
                                    }
                            ) {
                                AnimatedContent(
                                    targetState = islandMode,
                                    contentAlignment = Alignment.Center,
                                    transitionSpec = {
                                        (fadeIn(tween(160, delayMillis = 40)) + scaleIn(initialScale = 0.75f)) togetherWith
                                                (fadeOut(tween(110)) + scaleOut(targetScale = 0.75f))
                                    },
                                    label = "rightButtonMorph"
                                ) { mode ->
                                    when (mode) {
                                        BottomIslandMode.ITEM_EDIT -> {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Menu creazione",
                                                    tint = if (isItemEditAddMenuOpen) themeColors.accent else themeColors.textPrimary,
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .graphicsLayer { rotationZ = plusRotation }
                                                )
                                            }
                                        }
                                        BottomIslandMode.FUNCTION_EDIT -> {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Palette,
                                                    contentDescription = "Opzioni modalità",
                                                    tint = if (isFunctionOptionsMenuOpen) themeColors.accent else themeColors.textPrimary,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                            }
                                        }
                                        BottomIslandMode.WIDGET_EDIT -> {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Aggiungi widget",
                                                    tint = themeColors.accent,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        BottomIslandMode.NORMAL -> {
                                            val starRotation by animateFloatAsState(
                                                targetValue = if (isFavoriteIslandOpen) 72f else 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                ),
                                                label = "starRotation"
                                            )
                                            val starScale by animateFloatAsState(
                                                targetValue = if (isFavoriteIslandOpen) 1.15f else 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                ),
                                                label = "starScale"
                                            )
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Funzioni preferite",
                                                    tint = if (isFavoriteIslandOpen) themeColors.accent else themeColors.textPrimary,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .graphicsLayer {
                                                            rotationZ = starRotation
                                                            scaleX = starScale
                                                            scaleY = starScale
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ISOLA POPUP FUNZIONI PREFERITE (APERTA DAL TASTO STELLA)
                        val favoritePivotX = remember(viewModel.favoriteShortcuts) {
                            val compacted = compactFavoriteRows(viewModel.favoriteShortcuts)
                            if (compacted.isEmpty()) 0.88f
                            else {
                                val span = (compacted.maxOf { it.col } - compacted.minOf { it.col } + 1).coerceAtLeast(1)
                                (span.toFloat() - 0.5f) / span.toFloat()
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = islandMode == BottomIslandMode.NORMAL && isFavoriteIslandOpen,
                            enter = fadeIn(tween(180)) + scaleIn(
                                initialScale = 0.2f,
                                transformOrigin = TransformOrigin(favoritePivotX, 1f),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            exit = fadeOut(tween(130)) + scaleOut(
                                targetScale = 0.2f,
                                transformOrigin = TransformOrigin(favoritePivotX, 1f),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = popupBottomPadding)
                        ) {
                            FavoriteShortcutsIslandPopup(
                                shortcuts = viewModel.favoriteShortcuts,
                                useDistinctColors = viewModel.useDistinctFunctionColors,
                                isTorchOn = isGlobalTorchOn,
                                onShortcutClick = { action ->
                                    when (action) {
                                        FavoriteActionType.TORCH -> {
                                            toggleGlobalTorch()
                                        }
                                        FavoriteActionType.ADD_CLOTHING -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.WARDROBE)
                                            showAddClothingSheet = true
                                        }
                                        FavoriteActionType.SEARCH_WARDROBE -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.WARDROBE)
                                            isWardrobeSearchActive = true
                                        }
                                        FavoriteActionType.CALENDAR,
                                        FavoriteActionType.ADD_CALENDAR_EVENT -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.CALENDAR)
                                        }
                                        FavoriteActionType.MOVE -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.MOVE)
                                        }
                                        FavoriteActionType.LAUNDRY -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.LAUNDRY)
                                        }
                                        FavoriteActionType.TRIP -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.TRIP)
                                        }
                                        FavoriteActionType.TRANSFERS -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.TRANSFERS_ACTIVITY)
                                        }
                                        FavoriteActionType.CLOSET_PHOTOS -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.CLOSET_PHOTOS)
                                        }
                                        FavoriteActionType.TOOLS -> {
                                            isFavoriteIslandOpen = false
                                            navigateToTabOrFunction(IslandTabType.TOOLS)
                                        }
                                        FavoriteActionType.SETTINGS -> {
                                            isFavoriteIslandOpen = false
                                            isShowingSettings = true
                                        }
                                    }
                                },
                                onOpenEditorClick = {
                                    isFavoriteIslandOpen = false
                                    showFavoriteEditorDialog = true
                                }
                            )
                        }

                        // Menu opzioni funzioni (toggle Colori distinti) ancorato sopra al tasto circolare
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isFunctionEditMode && isFunctionOptionsMenuOpen,
                            enter = fadeIn(tween(180)) + scaleIn(
                                initialScale = 0.4f,
                                transformOrigin = TransformOrigin(1f, 1f),
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                            ) + slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ),
                            exit = fadeOut(tween(130)) + scaleOut(
                                targetScale = 0.4f,
                                transformOrigin = TransformOrigin(1f, 1f)
                            ) + slideOutVertically(targetOffsetY = { it / 3 }),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = popupBottomPadding)
                                .width(220.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = themeColors.islandBackground,
                                border = BorderStroke(0.5.dp, themeColors.cardBorder),
                                tonalElevation = 8.dp,
                                shadowElevation = 14.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.setDistinctFunctionColors(!viewModel.useDistinctFunctionColors)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(themeColors.accentContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Palette,
                                                    contentDescription = null,
                                                    tint = themeColors.accent,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            Text(
                                                text = "Colori distinti",
                                                color = themeColors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Switch(
                                            checked = viewModel.useDistinctFunctionColors,
                                            onCheckedChange = { viewModel.setDistinctFunctionColors(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = themeColors.accent,
                                                uncheckedThumbColor = themeColors.textSecondary,
                                                uncheckedTrackColor = themeColors.surfaceSecondary
                                            ),
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Menu aggiungi capo / cartella (armadio) ancorato sopra al tasto '+'
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isItemEditMode && isItemEditAddMenuOpen,
                            enter = fadeIn(tween(180)) + scaleIn(
                                initialScale = 0.4f,
                                transformOrigin = TransformOrigin(1f, 1f),
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                            ) + slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ),
                            exit = fadeOut(tween(130)) + scaleOut(
                                targetScale = 0.4f,
                                transformOrigin = TransformOrigin(1f, 1f)
                            ) + slideOutVertically(targetOffsetY = { it / 3 }),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = popupBottomPadding)
                                .width(165.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = themeColors.islandBackground,
                                border = BorderStroke(0.5.dp, themeColors.cardBorder),
                                tonalElevation = 8.dp,
                                shadowElevation = 14.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clickable {
                                                isItemEditAddMenuOpen = false
                                                showAddClothingSheet = true
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = themeColors.accent,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = "Aggiungi capo",
                                                color = themeColors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clickable {
                                                isItemEditAddMenuOpen = false
                                                onRequestCreateFolder = true
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CreateNewFolder,
                                                contentDescription = null,
                                                tint = themeColors.accent,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = "Crea cartella",
                                                color = themeColors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Ricerca Armadio
                AnimatedVisibility(
                    visible = isCurrentlyViewingWardrobe && isWardrobeSearchActive,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = fadeOut(animationSpec = tween(240)) + slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                        .zIndex(35f)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            viewModel.availableTags.forEach { tag ->
                                val isSel = wardrobeFilterTags.contains(tag)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSel) themeColors.accent else themeColors.islandBackground,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSel) themeColors.accent else themeColors.cardBorder
                                    ),
                                    tonalElevation = 6.dp,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.clickable {
                                        wardrobeFilterTags = if (isSel) {
                                            wardrobeFilterTags - tag
                                        } else {
                                            wardrobeFilterTags + tag
                                        }
                                    }
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSel) Color.White else themeColors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = themeColors.islandBackground,
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                            tonalElevation = 8.dp,
                            shadowElevation = 16.dp,
                            modifier = Modifier
                                .height(54.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cerca",
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    if (wardrobeSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Cerca nell'armadio...",
                                            color = themeColors.textSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = wardrobeSearchQuery,
                                        onValueChange = { wardrobeSearchQuery = it },
                                        singleLine = true,
                                        textStyle = ComposeTextStyle(
                                            color = themeColors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(themeColors.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (wardrobeSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { wardrobeSearchQuery = "" },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Cancella",
                                            tint = themeColors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        isWardrobeSearchActive = false
                                        wardrobeSearchQuery = ""
                                        wardrobeFilterTags = emptySet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "Fine",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteSelectedConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteSelectedConfirmDialog = false },
                containerColor = themeColors.dialogBackground,
                shape = RoundedCornerShape(26.dp),
                title = {
                    Text(
                        text = if (selectedItemIds.size == 1) "Eliminare il capo?" else "Eliminare i capi selezionati?",
                        color = themeColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (selectedItemIds.size == 1)
                            "Il capo selezionato verrà eliminato definitivamente dall'armadio."
                        else
                            "I ${selectedItemIds.size} capi selezionati verranno eliminati definitivamente dall'armadio.",
                        color = themeColors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedItemIds.toList().forEach { id ->
                                val item = viewModel.clothingItems.firstOrNull { it.id == id }
                                if (item != null) {
                                    viewModel.deleteClothingItem(item)
                                }
                            }
                            selectedItemIds.clear()
                            selectedItemQuantities.clear()
                            showDeleteSelectedConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                        shape = CircleShape
                    ) {
                        Text("Elimina", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSelectedConfirmDialog = false }) {
                        Text("Annulla", color = themeColors.textPrimary)
                    }
                }
            )
        }

        if (showDiscardConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardConfirmDialog = false },
                containerColor = themeColors.dialogBackground,
                shape = RoundedCornerShape(26.dp),
                title = { Text("Annullare le modifiche?", color = themeColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = { Text("Tutte le modifiche non salvate andranno perse.", color = themeColors.textSecondary, fontSize = 14.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isEditMode) {
                                viewModel.updateWidgetsLayout(originalWidgetsState)
                                isEditMode = false
                            }
                            if (isItemEditMode) {
                                viewModel.clothingItems.clear()
                                viewModel.clothingItems.addAll(originalClothingState)
                                viewModel.closetFolders.clear()
                                viewModel.closetFolders.addAll(originalFolderState)
                                selectedItemIds.clear()
                                selectedItemQuantities.clear()
                                isItemEditMode = false
                                isItemEditAddMenuOpen = false
                            }
                            if (isFunctionEditMode) {
                                viewModel.functionOrder.clear()
                                viewModel.functionOrder.addAll(originalFunctionOrder)
                                isFunctionEditMode = false
                                isFunctionOptionsMenuOpen = false
                            }
                            showDiscardConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                        shape = CircleShape
                    ) { Text("Annulla modifiche", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardConfirmDialog = false }) { Text("Continua a modificare", color = themeColors.textPrimary) }
                }
            )
        }

        if (showWidgetPicker) {
            WidgetPickerDialog(
                viewModel = viewModel,
                availableClosets = viewModel.availableClosets,
                onDismiss = { showWidgetPicker = false },
                onSelectWidget = { widgetType: WidgetType, targetCloset: String? ->
                    viewModel.addWidget(widgetType, targetCloset, preferredPage = currentHomePagerPage)
                    if (isEditMode) {
                        originalWidgetsState = viewModel.homeWidgets.toList()
                    }
                    showWidgetPicker = false
                }
            )
        }

        if (showHomeOptionsDialog) {
            HomeOptionsDialog(
                viewModel = viewModel,
                onDismiss = { showHomeOptionsDialog = false }
            )
        }

        if (showIslandTabsEditorDialog) {
            IslandTabsEditorDialog(
                initialTabs = viewModel.islandTabs,
                useDistinctColors = viewModel.useDistinctFunctionColors,
                onDismiss = { showIslandTabsEditorDialog = false },
                onSave = { updatedTabs ->
                    viewModel.updateIslandTabs(updatedTabs)
                    showIslandTabsEditorDialog = false
                }
            )
        }

        if (showFavoriteEditorDialog) {
            FavoriteShortcutsEditorDialog(
                initialShortcuts = viewModel.favoriteShortcuts,
                useDistinctColors = viewModel.useDistinctFunctionColors,
                onDismiss = { showFavoriteEditorDialog = false },
                onSave = { updatedList ->
                    viewModel.updateFavoriteShortcuts(updatedList)
                    showFavoriteEditorDialog = false
                }
            )
        }

        viewModel.startupUpdateRelease?.let { release ->
            StartupUpdateDialog(
                release = release,
                onDismiss = { viewModel.dismissStartupUpdateDialog() }
            )
        }

        if (viewModel.showReleaseNotesDialog) {
            ReleaseNotesDialog(
                versionName = appVersionName,
                onDismiss = { viewModel.dismissReleaseNotes(appVersionName) }
            )
        }

        if (isReminderPopupOpen) {
            ReminderListDialog(
                viewModel = viewModel,
                onDismiss = { isReminderPopupOpen = false }
            )
        }
    }
}

// =========================================================================
// DASHBOARD GRID E FLOW LAYOUT (SAMSUNG ONE UI 9.0 MATRIX ENGINE)
// =========================================================================

@Composable
fun HomeDashboardGrid(
    viewModel: ClosetViewModel,
    widgets: List<PlacedWidget>,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors,
    isEditMode: Boolean,
    onEnterEditMode: () -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
    scrollToTopTrigger: Int = 0,
    onPageChanged: (Int) -> Unit = {},
    onRemoveWidget: (String) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onUpdateWidgetsLayout: (List<PlacedWidget>) -> Unit = {},
    onNavigateFunction: (FunctionSubScreen) -> Unit = {},
    onWidgetClick: (PlacedWidget) -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val horizontalPadding = 16.dp
    val spacing = 12.dp
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val emptySpaceGestureModifier = if (!isEditMode) {
        Modifier.pointerInput(isEditMode) {
            detectTapGestures(
                onLongPress = {
                    onEnterEditMode()
                }
            )
        }
    } else Modifier

    if (viewModel.homeLayoutMode == HomeLayoutMode.PAGED) {
        val maxRows = viewModel.homeMaxRowsPerPage
        val highestOccupiedRow = if (widgets.isEmpty()) 0 else widgets.maxOf { it.gridRow.coerceAtLeast(0) + it.type.rowSpan }
        val occupiedPages = if (highestOccupiedRow <= 0) 1 else ((highestOccupiedRow - 1) / maxRows + 1)
        val pageCount = if (isEditMode) occupiedPages + 1 else occupiedPages
        val verticalPagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

        var pagedDraggedWidget by remember { mutableStateOf<PlacedWidget?>(null) }
        var pagedDragStartPage by remember { mutableIntStateOf(0) }
        var pagedDragTouchOffset by remember { mutableStateOf(Offset.Zero) }
        var pagedDragFingerPos by remember { mutableStateOf(Offset.Zero) }
        var pagedHoverRow by remember { mutableIntStateOf(-1) }
        var pagedHoverCol by remember { mutableIntStateOf(-1) }
        var lastEdgeHoverTime by remember { mutableLongStateOf(0L) }
        var edgeHoverDirection by remember { mutableIntStateOf(0) }
        var gridRootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        var isDraggingPageIndicator by remember { mutableStateOf(false) }
        var pageIndicatorDragStartPosition by remember { mutableFloatStateOf(0f) }
        var pageIndicatorDragPosition by remember { mutableFloatStateOf(0f) }
        var pageIndicatorAccumulatedDeltaY by remember { mutableFloatStateOf(0f) }

        val currentIndicatorDisplayPosition by remember {
            derivedStateOf {
                if (isDraggingPageIndicator) {
                    pageIndicatorDragPosition
                } else {
                    (verticalPagerState.currentPage + verticalPagerState.currentPageOffsetFraction).coerceIn(0f, (pageCount - 1).toFloat())
                }
            }
        }

        val indicatorActivePage by remember {
            derivedStateOf {
                currentIndicatorDisplayPosition.roundToInt().coerceIn(0, pageCount - 1)
            }
        }

        LaunchedEffect(pageIndicatorDragPosition, isDraggingPageIndicator, pageCount) {
            if (isDraggingPageIndicator && pageCount > 1) {
                val targetPage = pageIndicatorDragPosition.roundToInt().coerceIn(0, pageCount - 1)
                val targetFraction = (pageIndicatorDragPosition - targetPage).coerceIn(-0.5f, 0.5f)
                verticalPagerState.scrollToPage(page = targetPage, pageOffsetFraction = targetFraction)
            }
        }

        LaunchedEffect(isEditMode) {
            if (!isEditMode) {
                pagedDraggedWidget = null
                pagedHoverRow = -1
                pagedHoverCol = -1
                edgeHoverDirection = 0
                lastEdgeHoverTime = 0L
            }
        }

        // Timer di cambio pagina quando si trascina un widget verso l'alto o verso il basso ai bordi
        LaunchedEffect(pagedDraggedWidget) {
            if (pagedDraggedWidget == null) {
                edgeHoverDirection = 0
                lastEdgeHoverTime = 0L
                return@LaunchedEffect
            }

            val topZonePx = with(density) { 140.dp.toPx() }
            val bottomZonePx = with(density) { 150.dp.toPx() }

            while (pagedDraggedWidget != null) {
                val currentFingerY = pagedDragFingerPos.y
                val currentMaxHeight = if (viewportHeightPx > 0f) viewportHeightPx else 2000f
                val bottomEdge = currentMaxHeight - bottomZonePx

                val dir = when {
                    currentFingerY < topZonePx && verticalPagerState.currentPage > 0 -> -1
                    currentFingerY > bottomEdge && verticalPagerState.currentPage < pageCount - 1 -> 1
                    else -> 0
                }

                if (dir != 0 && !verticalPagerState.isScrollInProgress) {
                    val now = System.currentTimeMillis()
                    if (dir != edgeHoverDirection) {
                        edgeHoverDirection = dir
                        lastEdgeHoverTime = now
                    } else if (now - lastEdgeHoverTime >= 350L) {
                        val targetPage = (verticalPagerState.currentPage + dir).coerceIn(0, pageCount - 1)
                        if (targetPage != verticalPagerState.currentPage) {
                            edgeHoverDirection = 0
                            lastEdgeHoverTime = 0L
                            verticalPagerState.animateScrollToPage(targetPage)
                            delay(450)
                        }
                    }
                } else {
                    edgeHoverDirection = 0
                    lastEdgeHoverTime = 0L
                }
                delay(30)
            }
        }

        LaunchedEffect(verticalPagerState.currentPage) {
            onPageChanged(verticalPagerState.currentPage)
        }

        LaunchedEffect(scrollToTopTrigger) {
            if (scrollToTopTrigger > 0 && verticalPagerState.currentPage > 0) {
                verticalPagerState.animateScrollToPage(0)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size -> viewportHeightPx = size.height.toFloat() }
                .then(emptySpaceGestureModifier)
        ) {
            // Header superiore: Titolo Home / Modifica home
            AnimatedContent(
                targetState = isEditMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 40)) + slideInVertically { it / 3 })
                        .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutVertically { -it / 3 })
                },
                label = "homePagedTitleAnim",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = horizontalPadding, top = 18.dp, end = 70.dp)
                    .zIndex(20f)
            ) { editMode ->
                Text(
                    text = if (editMode) "Modifica home" else "Home",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { })
                    }
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = horizontalPadding, end = horizontalPadding)
                    .onGloballyPositioned { gridRootCoordinates = it }
                    .then(emptySpaceGestureModifier)
            ) {
                val availableWidth = maxWidth - (spacing * 3)
                val unitCellSize: Dp = availableWidth / 4

                val pageContentHeight = (unitCellSize * maxRows) + (spacing * (maxRows - 1))
                val topHeaderHeight = 64.dp
                val topHeaderHeightPx = with(density) { topHeaderHeight.toPx() }
                val cellStepPx = with(density) { (unitCellSize + spacing).toPx() }
                val minPeek = unitCellSize + spacing
                val bottomPeekHeight = maxOf(maxHeight - topHeaderHeight - pageContentHeight, minPeek)

                val customPageSize = remember(pageContentHeight) {
                    object : PageSize {
                        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
                            return pageContentHeight.roundToPx()
                        }
                    }
                }

                VerticalPager(
                    state = verticalPagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            val topFadePx = 70.dp.toPx()
                            val bottomFadePx = 130.dp.toPx()
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
                    pageSize = customPageSize,
                    pageSpacing = spacing,
                    contentPadding = PaddingValues(top = topHeaderHeight, bottom = bottomPeekHeight),
                    userScrollEnabled = (pagedDraggedWidget == null),
                    beyondViewportPageCount = 2
                ) { pageIndex ->
                    val pageStartRow = pageIndex * maxRows
                    val pageEndRow = pageStartRow + maxRows
                    val isCurrentActivePage = (pagedDraggedWidget != null && verticalPagerState.currentPage == pageIndex)

                    val pageWidgets = remember(widgets, pagedDraggedWidget, verticalPagerState.currentPage, pageIndex, pagedHoverRow, pagedHoverCol) {
                        if (pagedDraggedWidget != null) {
                            val draggedId = pagedDraggedWidget!!.instanceId
                            if (isCurrentActivePage) {
                                val others = widgets.filter { it.instanceId != draggedId && it.gridRow in pageStartRow until pageEndRow }
                                    .map { it.copy(gridRow = it.gridRow - pageStartRow) }
                                val draggedCandidate = pagedDraggedWidget!!.copy(
                                    gridRow = pagedHoverRow.coerceAtLeast(0),
                                    gridCol = pagedHoverCol.coerceAtLeast(0)
                                )
                                others + listOf(draggedCandidate)
                            } else {
                                widgets.filter { it.instanceId != draggedId && it.gridRow in pageStartRow until pageEndRow }
                                    .map { it.copy(gridRow = it.gridRow - pageStartRow) }
                            }
                        } else {
                            widgets.filter { w ->
                                val r = w.gridRow.coerceAtLeast(0)
                                r in pageStartRow until pageEndRow
                            }.map { w ->
                                w.copy(gridRow = w.gridRow - pageStartRow)
                            }
                        }
                    }

                    val dummyScrollState = rememberScrollState(0)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(emptySpaceGestureModifier),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        FlowWidgetLayout(
                            viewModel = viewModel,
                            widgets = pageWidgets,
                            useDistinctColors = useDistinctColors,
                            unitCellSize = unitCellSize,
                            spacing = spacing,
                            isEditMode = isEditMode,
                            onEnterEditMode = onEnterEditMode,
                            scrollState = dummyScrollState,
                            viewportHeightPx = viewportHeightPx,
                            fixedRows = maxRows,
                            pageIndex = pageIndex,
                            totalPageCount = pageCount,
                            externalDraggedWidgetId = if (isCurrentActivePage) pagedDraggedWidget?.instanceId else null,
                            externalHoverRow = if (isCurrentActivePage) pagedHoverRow else -1,
                            externalHoverCol = if (isCurrentActivePage) pagedHoverCol else -1,
                            onStartWidgetDrag = { widget, touchOffset, widgetCoords ->
                                pagedDraggedWidget = widget
                                pagedDragStartPage = pageIndex
                                pagedDragTouchOffset = touchOffset
                                val gridCoords = gridRootCoordinates
                                val posInGrid = if (gridCoords != null && gridCoords.isAttached && widgetCoords.isAttached) {
                                    gridCoords.localPositionOf(widgetCoords, touchOffset)
                                } else {
                                    val root = widgetCoords.localToRoot(touchOffset)
                                    val gridRoot = gridCoords?.localToRoot(Offset.Zero) ?: Offset.Zero
                                    root - gridRoot
                                }
                                pagedDragFingerPos = posInGrid
                                pagedHoverRow = widget.gridRow
                                pagedHoverCol = widget.gridCol
                            },
                            onWidgetDrag = { changePos, widgetCoords ->
                                val gridCoords = gridRootCoordinates
                                val posInGrid = if (gridCoords != null && gridCoords.isAttached && widgetCoords.isAttached) {
                                    gridCoords.localPositionOf(widgetCoords, changePos)
                                } else {
                                    val root = widgetCoords.localToRoot(changePos)
                                    val gridRoot = gridCoords?.localToRoot(Offset.Zero) ?: Offset.Zero
                                    root - gridRoot
                                }
                                pagedDragFingerPos = posInGrid

                                val cardLeft = posInGrid.x - pagedDragTouchOffset.x
                                val cardTop = posInGrid.y - pagedDragTouchOffset.y - topHeaderHeightPx
                                val rawCol = cardLeft / cellStepPx
                                val rawRow = cardTop / cellStepPx
                                val activeW = pagedDraggedWidget
                                if (activeW != null) {
                                    val prevCol = pagedHoverCol.coerceAtLeast(0)
                                    val prevRow = pagedHoverRow.coerceAtLeast(0)
                                    val maxCol = 4 - activeW.type.colSpan
                                    val maxRow = maxRows - activeW.type.rowSpan

                                    val newCol = if (kotlin.math.abs(rawCol - prevCol) > 0.55f) {
                                        rawCol.roundToInt().coerceIn(0, maxCol)
                                    } else {
                                        prevCol.coerceIn(0, maxCol)
                                    }
                                    val newRow = if (kotlin.math.abs(rawRow - prevRow) > 0.55f) {
                                        rawRow.roundToInt().coerceIn(0, maxRow)
                                    } else {
                                        prevRow.coerceIn(0, maxRow)
                                    }
                                    if (newCol != pagedHoverCol || newRow != pagedHoverRow) {
                                        pagedHoverCol = newCol
                                        pagedHoverRow = newRow
                                    }
                                }
                            },
                            onEndWidgetDrag = { completed ->
                                val activeW = pagedDraggedWidget
                                if (completed && activeW != null) {
                                    val finalPage = verticalPagerState.currentPage
                                    val targetRow = pagedHoverRow.coerceIn(0, (maxRows - activeW.type.rowSpan).coerceAtLeast(0))
                                    val targetCol = pagedHoverCol.coerceIn(0, (4 - activeW.type.colSpan).coerceAtLeast(0))
                                    val startPage = pagedDragStartPage
                                    val movedId = activeW.instanceId

                                    if (startPage == finalPage) {
                                        val thisPageWidgets = widgets.filter { it.gridRow in startPage * maxRows until (startPage + 1) * maxRows }
                                            .map { it.copy(gridRow = it.gridRow - startPage * maxRows) }
                                        val normalizedBase = normalizeGridPlacements(thisPageWidgets, maxRows)
                                        val resolved = resolveMatrixDragLayout(normalizedBase, movedId, targetRow, targetCol, maxRows)
                                        val pageUpdated = resolved.map { it.widget.copy(gridRow = startPage * maxRows + it.row, gridCol = it.col) }
                                        val otherWidgets = widgets.filter { it.gridRow !in startPage * maxRows until (startPage + 1) * maxRows }
                                        val allNew = (otherWidgets + pageUpdated).sortedWith(compareBy<PlacedWidget> { it.gridRow }.thenBy { it.gridCol })
                                        onUpdateWidgetsLayout(allNew)
                                    } else {
                                        // 1. Start page remaining widgets (preserva esattamente le loro posizioni senza alterarle)
                                        val startPageRemaining = widgets.filter { it.instanceId != movedId && it.gridRow in startPage * maxRows until (startPage + 1) * maxRows }

                                        // 2. Target page existing + moved widget
                                        val targetPageExisting = widgets.filter { it.instanceId != movedId && it.gridRow in finalPage * maxRows until (finalPage + 1) * maxRows }
                                            .map { it.copy(gridRow = it.gridRow - finalPage * maxRows) }
                                        val targetCandidate = activeW.copy(gridRow = targetRow, gridCol = targetCol)
                                        val targetBase = targetPageExisting + listOf(targetCandidate)
                                        val targetNormalized = normalizeGridPlacements(targetBase, maxRows)
                                        val targetResolved = resolveMatrixDragLayout(targetNormalized, movedId, targetRow, targetCol, maxRows)
                                        val targetUpdated = targetResolved.map { it.widget.copy(gridRow = finalPage * maxRows + it.row, gridCol = it.col) }

                                        // 3. Other pages untouched
                                        val untouched = widgets.filter { it.instanceId != movedId && it.gridRow !in startPage * maxRows until (startPage + 1) * maxRows && it.gridRow !in finalPage * maxRows until (finalPage + 1) * maxRows }

                                        val combined = (untouched + startPageRemaining + targetUpdated).sortedWith(compareBy<PlacedWidget> { it.gridRow }.thenBy { it.gridCol })
                                        onUpdateWidgetsLayout(combined)
                                    }
                                }
                                pagedDraggedWidget = null
                                pagedHoverRow = -1
                                pagedHoverCol = -1
                            },
                            onRemoveWidget = onRemoveWidget,
                            onUpdateWidgetsLayout = { updatedPageWidgets, _, _ ->
                                val otherWidgets = widgets.filter { w ->
                                    val r = w.gridRow.coerceAtLeast(0)
                                    r !in pageStartRow until pageEndRow
                                }
                                val mappedBack = updatedPageWidgets.map { w ->
                                    w.copy(gridRow = pageStartRow + w.gridRow)
                                }
                                onUpdateWidgetsLayout(otherWidgets + mappedBack)
                            },
                            onNavigateFunction = onNavigateFunction,
                            onWidgetClick = onWidgetClick
                        )
                    }
                }

                // Overlay widget fluttuante durante il trascinamento inter-pagina
                if (pagedDraggedWidget != null) {
                    val activeW = pagedDraggedWidget!!
                    val dragWidthDp = (unitCellSize * activeW.type.colSpan) + (spacing * (activeW.type.colSpan - 1))
                    val dragHeightDp = (unitCellSize * activeW.type.rowSpan) + (spacing * (activeW.type.rowSpan - 1))
                    val is1x1 = activeW.type.rowSpan == 1 && activeW.type.colSpan == 1
                    val dragCornerRadius = if (is1x1) unitCellSize / 2 else unitCellSize / 2

                    val cardLeftPx = pagedDragFingerPos.x - pagedDragTouchOffset.x
                    val cardTopPx = pagedDragFingerPos.y - pagedDragTouchOffset.y

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(cardLeftPx.roundToInt(), cardTopPx.roundToInt())
                            }
                            .size(width = dragWidthDp, height = dragHeightDp)
                            .zIndex(100f)
                            .scale(1.04f)
                            .graphicsLayer {
                                shadowElevation = 32f
                            }
                    ) {
                        WidgetCardContainer(
                            viewModel = viewModel,
                            widget = activeW,
                            useDistinctColors = useDistinctColors,
                            isEditMode = true,
                            isBeingDragged = true,
                            isBeingResized = false,
                            canResize = false,
                            cornerRadius = dragCornerRadius,
                            onRemove = {},
                            onClick = {},
                            onNavigateFunction = onNavigateFunction
                        )
                    }
                }
            }

            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val islandBottomPadding = when {
                navBarBottom == 0.dp -> 14.dp
                navBarBottom <= 24.dp -> 6.dp
                else -> 8.dp
            }
            val indicatorBottomOffset = islandBottomPadding + 54.dp + 10.dp

            HomePageIndicator(
                pageCount = pageCount,
                currentPage = indicatorActivePage,
                visibility = viewModel.homePageIndicatorVisibility,
                position = viewModel.homePageIndicatorPosition,
                isScrollInProgress = verticalPagerState.isScrollInProgress,
                isEditMode = isEditMode,
                isDragging = isDraggingPageIndicator,
                bottomOffset = indicatorBottomOffset,
                onDragStart = {
                    isDraggingPageIndicator = true
                    pageIndicatorDragStartPosition = currentIndicatorDisplayPosition
                    pageIndicatorDragPosition = pageIndicatorDragStartPosition
                    pageIndicatorAccumulatedDeltaY = 0f
                },
                onDragDelta = { dragAmount ->
                    pageIndicatorAccumulatedDeltaY += dragAmount
                    val pageStepPx = with(density) { 32.dp.toPx() }
                    val deltaPages = pageIndicatorAccumulatedDeltaY / pageStepPx
                    pageIndicatorDragPosition = (pageIndicatorDragStartPosition + deltaPages)
                        .coerceIn(0f, (pageCount - 1).toFloat())
                },
                onDragEnd = {
                    if (isDraggingPageIndicator) {
                        val targetPage = pageIndicatorDragPosition.roundToInt().coerceIn(0, pageCount - 1)
                        isDraggingPageIndicator = false
                        coroutineScope.launch {
                            verticalPagerState.animateScrollToPage(
                                page = targetPage,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                },
                onDragCancel = {
                    if (isDraggingPageIndicator) {
                        val targetPage = pageIndicatorDragPosition.roundToInt().coerceIn(0, pageCount - 1)
                        isDraggingPageIndicator = false
                        coroutineScope.launch {
                            verticalPagerState.animateScrollToPage(
                                page = targetPage,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                },
                onPageClick = { targetP ->
                    coroutineScope.launch {
                        verticalPagerState.animateScrollToPage(
                            page = targetP,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            )
        }
    } else {
        // SCORRIMENTO CONTINUO
        LaunchedEffect(scrollToTopTrigger) {
            if (scrollToTopTrigger > 0 && scrollState.value > 0) {
                scrollState.animateScrollTo(0)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    viewportHeightPx = size.height.toFloat()
                }
                .then(emptySpaceGestureModifier)
        ) {
            // Header superiore: Titolo Home / Modifica home
            AnimatedContent(
                targetState = isEditMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 40)) + slideInVertically { it / 3 })
                        .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutVertically { -it / 3 })
                },
                label = "homeTitleAnim",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = horizontalPadding, top = 18.dp, end = 70.dp)
                    .zIndex(20f)
            ) { editMode ->
                Text(
                    text = if (editMode) "Modifica home" else "Home",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { })
                    }
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = horizontalPadding, end = horizontalPadding)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        val topFadePx = 70.dp.toPx()
                        val bottomFadePx = 110.dp.toPx()
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
                    }
                    .verticalScroll(scrollState)
                    .then(emptySpaceGestureModifier)
                    .padding(top = 64.dp, bottom = 80.dp)
            ) {
                val availableWidth = maxWidth - (spacing * 3)
                val unitCellSize: Dp = availableWidth / 4

                FlowWidgetLayout(
                    viewModel = viewModel,
                    widgets = widgets,
                    useDistinctColors = useDistinctColors,
                    unitCellSize = unitCellSize,
                    spacing = spacing,
                    isEditMode = isEditMode,
                    onEnterEditMode = onEnterEditMode,
                    scrollState = scrollState,
                    viewportHeightPx = viewportHeightPx,
                    fixedRows = 0,
                    onRemoveWidget = onRemoveWidget,
                    onUpdateWidgetsLayout = { updatedWidgets, _, _ ->
                        onUpdateWidgetsLayout(updatedWidgets)
                    },
                    onNavigateFunction = onNavigateFunction,
                    onWidgetClick = onWidgetClick
                )
            }
        }
    }
}

@Composable
fun FlowWidgetLayout(
    viewModel: ClosetViewModel,
    widgets: List<PlacedWidget>,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors,
    unitCellSize: Dp,
    spacing: Dp,
    isEditMode: Boolean,
    onEnterEditMode: () -> Unit = {},
    scrollState: ScrollState,
    viewportHeightPx: Float = 0f,
    fixedRows: Int = 0,
    pageIndex: Int = 0,
    totalPageCount: Int = 1,
    externalDraggedWidgetId: String? = null,
    externalHoverRow: Int = -1,
    externalHoverCol: Int = -1,
    onStartWidgetDrag: ((widget: PlacedWidget, touchOffset: Offset, widgetCoords: LayoutCoordinates) -> Unit)? = null,
    onWidgetDrag: ((changePos: Offset, widgetCoords: LayoutCoordinates) -> Unit)? = null,
    onEndWidgetDrag: ((completed: Boolean) -> Unit)? = null,
    onRequestPageChange: (Int) -> Unit = {},
    onRemoveWidget: (String) -> Unit,
    onUpdateWidgetsLayout: (List<PlacedWidget>, destinationPageIndex: Int, movedWidgetId: String?) -> Unit,
    onNavigateFunction: (FunctionSubScreen) -> Unit = {},
    onWidgetClick: (PlacedWidget) -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val unitCellSizePx = with(density) { unitCellSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val cellStepPx = unitCellSizePx + spacingPx
    val cellStepDp = unitCellSize + spacing
    val headerOffsetPx = if (fixedRows > 0) 0f else with(density) { 72.dp.toPx() }
    val removeZonePx = with(density) { 36.dp.toPx() }
    val resizeZonePx = with(density) { 44.dp.toPx() }
    val tapSlopPx = with(density) { 10.dp.toPx() }

    val sharedCornerRadius = unitCellSize / 2

    // Lista di lavoro locale sincronizzata (come in FunctionsScreen) per evitare qualsiasi scatto al rilascio
    val workingWidgets = remember {
        mutableStateListOf<PlacedWidget>().apply { addAll(widgets) }
    }

    // Stato trascinamento (Spostamento widget stile One UI 9)
    var draggedWidgetId by remember { mutableStateOf<String?>(null) }
    var dragStartRow by remember { mutableIntStateOf(0) }
    var dragStartCol by remember { mutableIntStateOf(0) }
    var targetHoverRow by remember { mutableIntStateOf(-1) }
    var targetHoverCol by remember { mutableIntStateOf(-1) }
    var dragTouchOffsetX by remember { mutableFloatStateOf(0f) }
    var dragTouchOffsetY by remember { mutableFloatStateOf(0f) }
    var dragFingerCurrentX by remember { mutableFloatStateOf(0f) }
    var dragFingerCurrentY by remember { mutableFloatStateOf(0f) }
    var currentTargetPage by remember(pageIndex) { mutableIntStateOf(pageIndex) }
    var lastEdgeHoverTime by remember { mutableLongStateOf(0L) }
    var edgeHoverDirection by remember { mutableIntStateOf(0) }

    // Stato ridimensionamento (Cursore angolo in basso a destra stile One UI 9)
    var resizingWidgetId by remember { mutableStateOf<String?>(null) }
    var resizePreviewType by remember { mutableStateOf<WidgetType?>(null) }

    LaunchedEffect(widgets.toList(), draggedWidgetId, resizingWidgetId) {
        if (draggedWidgetId == null && resizingWidgetId == null) {
            val currentIds = workingWidgets.map { "${it.instanceId}_${it.type.name}_${it.gridRow}_${it.gridCol}" }
            val incomingIds = widgets.map { "${it.instanceId}_${it.type.name}_${it.gridRow}_${it.gridCol}" }
            if (currentIds != incomingIds) {
                workingWidgets.clear()
                workingWidgets.addAll(widgets)
            } else {
                for (i in widgets.indices) {
                    if (workingWidgets[i] != widgets[i]) {
                        workingWidgets[i] = widgets[i]
                    }
                }
            }
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            draggedWidgetId = null
            targetHoverRow = -1
            targetHoverCol = -1
            dragTouchOffsetX = 0f
            dragTouchOffsetY = 0f
            dragFingerCurrentX = 0f
            dragFingerCurrentY = 0f
            currentTargetPage = pageIndex
            lastEdgeHoverTime = 0L
            edgeHoverDirection = 0
            resizingWidgetId = null
            resizePreviewType = null
        }
    }

    val baseWidgets = if ((draggedWidgetId != null || resizingWidgetId != null) && workingWidgets.isNotEmpty()) {
        workingWidgets.toList()
    } else {
        widgets.toList()
    }

    // Applichiamo eventuale anteprima di ridimensionamento in tempo reale
    val effectiveWidgets = remember(baseWidgets, resizingWidgetId, resizePreviewType) {
        val activeResizeId = resizingWidgetId
        val activePreviewType = resizePreviewType
        if (activeResizeId != null && activePreviewType != null) {
            baseWidgets.map { w ->
                if (w.instanceId == activeResizeId) {
                    val clampedCol = w.gridCol.coerceAtLeast(0).coerceIn(0, 4 - activePreviewType.colSpan)
                    w.copy(type = activePreviewType, gridCol = clampedCol)
                } else {
                    w
                }
            }
        } else {
            baseWidgets
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val originalPlacements = remember(effectiveWidgets, unitCellSizePx, spacingPx, fixedRows) {
        normalizeGridPlacements(effectiveWidgets, maxRowsPerPage = fixedRows)
    }

    val effectiveDragId = externalDraggedWidgetId ?: draggedWidgetId
    val effectiveHoverRow = if (externalDraggedWidgetId != null) externalHoverRow else targetHoverRow
    val effectiveHoverCol = if (externalDraggedWidgetId != null) externalHoverCol else targetHoverCol

    val virtualPlacements = remember(originalPlacements, effectiveDragId, effectiveHoverRow, effectiveHoverCol, fixedRows) {
        val activeDragId = effectiveDragId
        if (activeDragId != null && effectiveHoverRow >= 0 && effectiveHoverCol >= 0) {
            resolveMatrixDragLayout(
                basePlacements = originalPlacements,
                draggedId = activeDragId,
                targetRow = effectiveHoverRow,
                targetCol = effectiveHoverCol,
                maxRowsPerPage = fixedRows
            )
        } else {
            originalPlacements
        }
    }

    val currentOnEnterEditMode by rememberUpdatedState(onEnterEditMode)
    val currentOnUpdateLayout by rememberUpdatedState(onUpdateWidgetsLayout)
    val currentOnRemoveWidget by rememberUpdatedState(onRemoveWidget)
    val currentOnWidgetClick by rememberUpdatedState(onWidgetClick)
    val currentVirtualPlacements by rememberUpdatedState(virtualPlacements)
    val currentOriginalPlacements by rememberUpdatedState(originalPlacements)

    // Auto-scroll continuo in modalità scorrimento continuo
    LaunchedEffect(draggedWidgetId) {
        if (draggedWidgetId == null) return@LaunchedEffect
        if (fixedRows <= 0) {
            val topScrollZonePx = with(density) { 110.dp.toPx() }
            val bottomScrollZonePx = with(density) { 150.dp.toPx() }
            val maxScrollStepPx = with(density) { 14.dp.toPx() }

            while (draggedWidgetId != null) {
                if (viewportHeightPx > 0f) {
                    val fingerY = dragFingerCurrentY
                    val topZone = topScrollZonePx
                    val bottomZone = viewportHeightPx - bottomScrollZonePx

                    val scrollDelta: Float = when {
                        fingerY < topZone && scrollState.value > 0 -> {
                            val factor = ((topZone - fingerY) / topZone).coerceIn(0.2f, 1f)
                            -maxScrollStepPx * factor
                        }
                        fingerY > bottomZone && scrollState.value < scrollState.maxValue -> {
                            val factor = ((fingerY - bottomZone) / bottomScrollZonePx).coerceIn(0.2f, 1f)
                            maxScrollStepPx * factor
                        }
                        else -> 0f
                    }

                    if (scrollDelta != 0f) {
                        val targetScroll = (scrollState.value + scrollDelta.roundToInt()).coerceIn(0, scrollState.maxValue)
                        if (targetScroll != scrollState.value) {
                            scrollState.scrollTo(targetScroll)
                            val currentCardTopPx = dragFingerCurrentY + scrollState.value - headerOffsetPx - dragTouchOffsetY
                            val rawRow = currentCardTopPx / cellStepPx
                            val prevRow = targetHoverRow.coerceAtLeast(0)
                            val maxAllowedRow = maxOf(
                                9,
                                (currentOriginalPlacements.maxOfOrNull { it.row + it.rowSpan } ?: 0) + 1
                            )
                            val newRow = if (abs(rawRow - prevRow) > 0.55f) {
                                rawRow.roundToInt().coerceIn(0, maxAllowedRow)
                            } else {
                                prevRow.coerceIn(0, maxAllowedRow)
                            }
                            if (newRow != targetHoverRow) {
                                targetHoverRow = newRow
                            }
                        }
                    }
                }
                delay(16)
            }
        }
    }

    val maxRow = if (fixedRows > 0) fixedRows else maxOf(
        virtualPlacements.maxOfOrNull { it.row + it.rowSpan } ?: 0,
        originalPlacements.maxOfOrNull { it.row + it.rowSpan } ?: 0,
        if (isEditMode) 10 else 0
    )
    val containerHeightDp = if (maxRow > 0) {
        (unitCellSize * maxRow) + (spacing * (maxRow - 1))
    } else 0.dp

    val editModeSpringScale by animateFloatAsState(
        targetValue = if (isEditMode) 0.935f else 0.97f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "widgetEditModeScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeightDp)
    ) {
        val draggedPlacementPreview = if (effectiveDragId != null) {
            virtualPlacements.find { it.widget.instanceId == effectiveDragId }
        } else null

        if (isEditMode && draggedPlacementPreview != null) {
            val dropTargetX by animateDpAsState(
                targetValue = cellStepDp * draggedPlacementPreview.col,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "dropZoneX"
            )
            val dropTargetY by animateDpAsState(
                targetValue = cellStepDp * draggedPlacementPreview.row,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "dropZoneY"
            )
            val dropWidthDp = (unitCellSize * draggedPlacementPreview.colSpan) + (spacing * (draggedPlacementPreview.colSpan - 1))
            val dropHeightDp = (unitCellSize * draggedPlacementPreview.rowSpan) + (spacing * (draggedPlacementPreview.rowSpan - 1))

            Box(
                modifier = Modifier
                    .offset(x = dropTargetX, y = dropTargetY)
                    .size(width = dropWidthDp, height = dropHeightDp)
                    .zIndex(0.5f)
                    .scale(editModeSpringScale)
                    .clip(RoundedCornerShape(sharedCornerRadius))
                    .background(colors.accent.copy(alpha = if (colors.isDark) 0.16f else 0.12f))
                    .border(
                        width = 2.dp,
                        color = colors.accent.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(sharedCornerRadius)
                    )
            )
        }

        effectiveWidgets.forEach { widget ->
            key(widget.instanceId) {
                val isDragged = isEditMode && (draggedWidgetId == widget.instanceId)
                val isResizing = isEditMode && (resizingWidgetId == widget.instanceId)
                val pOrig = originalPlacements.find { it.widget.instanceId == widget.instanceId }
                val pVirt = virtualPlacements.find { it.widget.instanceId == widget.instanceId }

                if (pOrig != null && pVirt != null) {
                    val slotPosXDp = cellStepDp * pVirt.col
                    val slotPosYDp = cellStepDp * pVirt.row
                    val targetWidthDp = (unitCellSize * pVirt.colSpan) + (spacing * (pVirt.colSpan - 1))
                    val targetHeightDp = (unitCellSize * pVirt.rowSpan) + (spacing * (pVirt.rowSpan - 1))
                    val is1x1 = pVirt.rowSpan == 1 && pVirt.colSpan == 1
                    val targetCornerRadius = if (is1x1) unitCellSize / 2 else sharedCornerRadius

                    val posXDp: Dp
                    val posYDp: Dp
                    val widthDp: Dp
                    val heightDp: Dp
                    val cornerRadiusDp: Dp

                    if (isEditMode) {
                        val animPosX by animateDpAsState(
                            targetValue = slotPosXDp,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            ),
                            label = "widgetPosX"
                        )
                        val animPosY by animateDpAsState(
                            targetValue = slotPosYDp,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            ),
                            label = "widgetPosY"
                        )
                        val animWidthDp by animateDpAsState(
                            targetValue = targetWidthDp,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            ),
                            label = "widgetWidth"
                        )
                        val animHeightDp by animateDpAsState(
                            targetValue = targetHeightDp,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            ),
                            label = "widgetHeight"
                        )
                        val animCornerRadius by animateDpAsState(
                            targetValue = targetCornerRadius,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            ),
                            label = "widgetCornerRadius"
                        )
                        posXDp = animPosX
                        posYDp = animPosY
                        widthDp = animWidthDp
                        heightDp = animHeightDp
                        cornerRadiusDp = animCornerRadius
                    } else {
                        posXDp = slotPosXDp
                        posYDp = slotPosYDp
                        widthDp = targetWidthDp
                        heightDp = targetHeightDp
                        cornerRadiusDp = targetCornerRadius
                    }

                    val cardScale by animateFloatAsState(
                        targetValue = when {
                            isDragged -> 1.02f
                            isResizing -> 0.98f
                            isEditMode -> 0.935f
                            else -> 0.97f
                        },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        ),
                        label = "widgetScale"
                    )

                    val cardElevation by animateFloatAsState(
                        targetValue = if (isDragged || isResizing) 28f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "widgetElevation"
                    )

                    val isSettling = isEditMode && !isDragged && (
                        abs(posXDp.value - slotPosXDp.value) > 0.5f ||
                            abs(posYDp.value - slotPosYDp.value) > 0.5f
                        )

                    val currentOrigPlacement by rememberUpdatedState(pOrig)
                    val currentWidget by rememberUpdatedState(widget)
                    val canResize = remember(widget.type) {
                        widget.type.getAvailableVariants().size > 1
                    }

                    // Calcolo traslazione 1:1 invariante allo scroll (identico a FunctionsScreen)
                    val currentCardLeftPx = if (isDragged) {
                        dragFingerCurrentX - dragTouchOffsetX
                    } else 0f
                    val currentCardTopPx = if (isDragged) {
                        dragFingerCurrentY + scrollState.value - headerOffsetPx - dragTouchOffsetY
                    } else 0f

                    val dragVisualTranslationX = if (isDragged) {
                        currentCardLeftPx - (cellStepPx * dragStartCol)
                    } else 0f
                    val dragVisualTranslationY = if (isDragged) {
                        currentCardTopPx - (cellStepPx * dragStartRow)
                    } else 0f

                    val isExternallyDragged = (externalDraggedWidgetId == widget.instanceId)
                    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

                    Box(
                        modifier = Modifier
                            .offset(
                                x = if (isDragged) (cellStepDp * dragStartCol) else posXDp,
                                y = if (isDragged) (cellStepDp * dragStartRow) else posYDp
                            )
                            .size(width = widthDp, height = heightDp)
                            .zIndex(
                                when {
                                    isDragged || isResizing -> 60f
                                    isSettling -> 10f
                                    else -> 2f
                                }
                            )
                            .onGloballyPositioned { itemCoordinates = it }
                            // pointerInput PRIMA di graphicsLayer per evitare oscillazioni/tremolii
                            .pointerInput(widget.instanceId, isEditMode, canResize) {
                                if (!isEditMode) {
                                    detectTapGestures(
                                        onTap = { currentOnWidgetClick(currentWidget) },
                                        onLongPress = { currentOnEnterEditMode() }
                                    )
                                } else {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)

                                        val is1x1 = currentOrigPlacement.rowSpan == 1 && currentOrigPlacement.colSpan == 1
                                        val effectiveResizeZonePx = with(density) { if (is1x1) 32.dp.toPx() else 42.dp.toPx() }
                                        val effectiveRemoveZonePx = with(density) { if (is1x1) 30.dp.toPx() else 38.dp.toPx() }

                                        // 1. Tasto Rimuovi (-) in alto a sinistra (TopStart)
                                        val isOnRemoveButton = down.position.x <= effectiveRemoveZonePx &&
                                            down.position.y <= effectiveRemoveZonePx

                                        if (isOnRemoveButton) {
                                            down.consume()
                                            val up = waitForUpOrCancellation()
                                            if (up != null) {
                                                up.consume()
                                                currentOnRemoveWidget(widget.instanceId)
                                            }
                                            return@awaitEachGesture
                                        }

                                        // 2. Cursore di ridimensionamento in basso a destra (One UI touch target ottimizzato)
                                        val isOnResizeHandle = canResize &&
                                            down.position.x >= (size.width - effectiveResizeZonePx) &&
                                            down.position.y >= (size.height - effectiveResizeZonePx)

                                        if (isOnResizeHandle && draggedWidgetId == null && resizingWidgetId == null) {
                                            down.consume()
                                            val startWidget = currentWidget
                                            val startType = startWidget.type
                                            val baseWidthPx = (unitCellSizePx * startType.colSpan) + (spacingPx * (startType.colSpan - 1))
                                            val baseHeightPx = (unitCellSizePx * startType.rowSpan) + (spacingPx * (startType.rowSpan - 1))

                                            resizingWidgetId = startWidget.instanceId
                                            resizePreviewType = startType

                                            var totalDragX = 0f
                                            var totalDragY = 0f
                                            var lastPointerPos = down.position
                                            var completed = false

                                            try {
                                                completed = drag(down.id) { change ->
                                                    val delta = change.position - lastPointerPos
                                                    lastPointerPos = change.position
                                                    change.consume()

                                                    totalDragX += delta.x
                                                    totalDragY += delta.y

                                                    val targetWidthPx = (baseWidthPx + totalDragX).coerceAtLeast(unitCellSizePx * 0.6f)
                                                    val targetHeightPx = (baseHeightPx + totalDragY).coerceAtLeast(unitCellSizePx * 0.6f)

                                                    val desiredCols = ((targetWidthPx + spacingPx) / cellStepPx).roundToInt().coerceIn(1, 4)
                                                    val maxAllowedRows = if (fixedRows > 0) fixedRows else 4
                                                    val desiredRows = ((targetHeightPx + spacingPx) / cellStepPx).roundToInt().coerceIn(1, maxAllowedRows)

                                                    val candidateVariant = startType.findBestVariantForSize(desiredCols, desiredRows)
                                                    if (candidateVariant != resizePreviewType) {
                                                        resizePreviewType = candidateVariant
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                completed = false
                                            }

                                            // Se l'utente ha fatto un tocco rapido sul cursore senza trascinare, passa alla dimensione successiva
                                            val draggedDistance = kotlin.math.hypot(totalDragX, totalDragY)
                                            val finalVariant = if (draggedDistance < tapSlopPx) {
                                                startType.getNextSizeVariant()
                                            } else {
                                                resizePreviewType ?: startType
                                            }

                                            if (completed || draggedDistance < tapSlopPx) {
                                                val updatedBase = baseWidgets.map { w ->
                                                    if (w.instanceId == startWidget.instanceId) {
                                                        val clampedCol = w.gridCol.coerceAtLeast(0).coerceIn(0, 4 - finalVariant.colSpan)
                                                        w.copy(type = finalVariant, gridCol = clampedCol)
                                                    } else {
                                                        w
                                                    }
                                                }
                                                val normalized = normalizeGridPlacements(updatedBase, maxRowsPerPage = fixedRows)
                                                    .sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col })
                                                    .map { it.widget.copy(gridRow = it.row, gridCol = it.col) }

                                                workingWidgets.clear()
                                                workingWidgets.addAll(normalized)
                                                currentOnUpdateLayout(normalized, pageIndex, null)
                                            }

                                            resizingWidgetId = null
                                            resizePreviewType = null
                                            return@awaitEachGesture
                                        }

                                        // 3. Spostamento widget immediato al tocco e trascinamento (superamento touch slop senza attese)
                                        var isDragStarted = false
                                        var pan = Offset.Zero
                                        val dragTrigger = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                                            change.consume()
                                            pan = overSlop
                                            isDragStarted = true
                                        }

                                        if (isDragStarted && draggedWidgetId == null && resizingWidgetId == null) {
                                            if (onStartWidgetDrag != null) {
                                                val savedCoords: LayoutCoordinates = itemCoordinates ?: return@awaitEachGesture
                                                onStartWidgetDrag(widget, down.position, savedCoords)
                                                var completed = false
                                                try {
                                                    completed = drag(down.id) { change ->
                                                        change.consume()
                                                        val curCoords: LayoutCoordinates = itemCoordinates ?: savedCoords
                                                        onWidgetDrag?.invoke(change.position, curCoords)
                                                    }
                                                } catch (_: Exception) {
                                                    completed = false
                                                }
                                                onEndWidgetDrag?.invoke(completed)
                                            } else {
                                                val startP = currentOrigPlacement
                                                val startR = startP.row
                                                val startC = startP.col

                                                draggedWidgetId = widget.instanceId
                                                dragStartRow = startR
                                                dragStartCol = startC
                                                targetHoverRow = startR
                                                targetHoverCol = startC

                                                dragTouchOffsetX = down.position.x
                                                dragTouchOffsetY = down.position.y
                                                dragFingerCurrentX = (cellStepPx * startC) + down.position.x + pan.x
                                                dragFingerCurrentY = headerOffsetPx + (cellStepPx * startR) + down.position.y + pan.y - scrollState.value

                                                var completed = false
                                                try {
                                                    completed = drag(down.id) { change ->
                                                        change.consume()
                                                        if (draggedWidgetId == widget.instanceId) {
                                                            dragFingerCurrentX = (cellStepPx * startC) + change.position.x
                                                            dragFingerCurrentY = headerOffsetPx + (cellStepPx * startR) + change.position.y - scrollState.value

                                                            val cardLeftPx = dragFingerCurrentX - dragTouchOffsetX
                                                            val cardTopPx = dragFingerCurrentY + scrollState.value - headerOffsetPx - dragTouchOffsetY

                                                            val rawCol = cardLeftPx / cellStepPx
                                                            val rawRow = cardTopPx / cellStepPx

                                                            val prevCol = targetHoverCol.coerceAtLeast(0)
                                                            val prevRow = targetHoverRow.coerceAtLeast(0)

                                                            val newCol = if (abs(rawCol - prevCol) > 0.55f) {
                                                                rawCol.roundToInt().coerceIn(0, 4 - startP.colSpan)
                                                            } else {
                                                                prevCol.coerceIn(0, 4 - startP.colSpan)
                                                            }

                                                            val maxAllowedRow = if (fixedRows > 0) {
                                                                (fixedRows - startP.rowSpan).coerceAtLeast(0)
                                                            } else {
                                                                maxOf(
                                                                    9,
                                                                    (currentOriginalPlacements.maxOfOrNull { it.row + it.rowSpan } ?: 0) + 1
                                                                )
                                                            }
                                                            val newRow = if (abs(rawRow - prevRow) > 0.55f) {
                                                                rawRow.roundToInt().coerceIn(0, maxAllowedRow)
                                                            } else {
                                                                prevRow.coerceIn(0, maxAllowedRow)
                                                            }

                                                            if (newRow != targetHoverRow || newCol != targetHoverCol) {
                                                                targetHoverRow = newRow
                                                                targetHoverCol = newCol
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                    completed = false
                                                }

                                                if (completed && draggedWidgetId == widget.instanceId) {
                                                    val finalPlacements = currentVirtualPlacements
                                                    val updatedWidgets = finalPlacements
                                                        .sortedWith(compareBy<WidgetPlacement> { it.row }.thenBy { it.col })
                                                        .map { it.widget.copy(gridRow = it.row, gridCol = it.col) }

                                                    workingWidgets.clear()
                                                    workingWidgets.addAll(updatedWidgets)
                                                    currentOnUpdateLayout(updatedWidgets, currentTargetPage, widget.instanceId)
                                                }

                                                draggedWidgetId = null
                                                targetHoverRow = -1
                                                targetHoverCol = -1
                                                dragTouchOffsetX = 0f
                                                dragTouchOffsetY = 0f
                                                dragFingerCurrentX = 0f
                                                dragFingerCurrentY = 0f
                                            }
                                        }
                                    }
                                }
                            }
                            .graphicsLayer {
                                clip = false
                                alpha = if (isExternallyDragged) 0f else 1f
                                scaleX = cardScale
                                scaleY = cardScale
                                translationX = dragVisualTranslationX
                                translationY = dragVisualTranslationY
                                shadowElevation = if (isExternallyDragged) 0f else cardElevation
                            }
                    ) {
                        WidgetCardContainer(
                            viewModel = viewModel,
                            widget = widget,
                            useDistinctColors = useDistinctColors,
                            isEditMode = isEditMode,
                            isBeingDragged = isDragged,
                            isBeingResized = isResizing,
                            canResize = canResize,
                            cornerRadius = cornerRadiusDp,
                            onRemove = { onRemoveWidget(widget.instanceId) },
                            onClick = { onWidgetClick(widget) },
                            onNavigateFunction = onNavigateFunction
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// CONTENITORE SINGOLO WIDGET E CATALOGO WIDGET
// =========================================================================

@Composable
fun WidgetCardContainer(
    viewModel: ClosetViewModel,
    widget: PlacedWidget,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors,
    isEditMode: Boolean,
    isBeingDragged: Boolean = false,
    isBeingResized: Boolean = false,
    canResize: Boolean = false,
    cornerRadius: Dp = 28.dp,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    onNavigateFunction: (FunctionSubScreen) -> Unit = {}
) {
    val colors = LocalOneUIColors.current
    val is1x1 = widget.type.rowSpan == 1 && widget.type.colSpan == 1
    val widgetShape: Shape = RoundedCornerShape(cornerRadius)
    val isActiveInteraction = isBeingDragged || isBeingResized

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = widgetShape,
            color = colors.cardBackground,
            tonalElevation = if (isActiveInteraction) 8.dp else 2.dp,
            shadowElevation = if (isActiveInteraction) 16.dp else if (colors.isDark) 0.dp else 4.dp,
            border = BorderStroke(
                width = if (isActiveInteraction) 2.dp else if (isEditMode) 1.4.dp else 0.5.dp,
                color = when {
                    isActiveInteraction -> colors.accent
                    isEditMode -> colors.accent.copy(alpha = 0.65f)
                    else -> colors.cardBorder
                }
            ),
            modifier = Modifier
                .fillMaxSize()
                .clip(widgetShape)
        ) {
            AnimatedContent(
                targetState = Pair(widget.type, useDistinctColors),
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "widgetContentResizeAnim"
            ) { (currentType, distinctColors) ->
                when (currentType) {
                    WidgetType.CALENDAR_TODAY_1X1 -> CalendarTodayWidget1x1(viewModel)
                    WidgetType.CALENDAR_TODAY_2X1 -> CalendarTodayWidget2x1(viewModel)
                    WidgetType.CALENDAR_TODAY_2X2 -> CalendarTodayWidget2x2(viewModel)
                    WidgetType.CALENDAR_MONTH_2X2 -> CalendarMonthWidget2x2(viewModel)
                    WidgetType.CALENDAR_AGENDA_4X2 -> CalendarAgendaWidget4x2(viewModel)
                    WidgetType.CALENDAR_ADD_EVENT_1X1 -> CalendarAddEventWidget1x1()
                    WidgetType.CALENDAR_ADD_EVENT_1X2 -> CalendarAddEventWidget1x2()

                    WidgetType.WARDROBE_COUNT_1X1 -> WardrobeCountWidget1x1(viewModel, widget.targetCloset)
                    WidgetType.WARDROBE_COUNT_1X2 -> WardrobeCountWidget1x2(viewModel, widget.targetCloset)
                    WidgetType.WARDROBE_COUNT_2X2 -> WardrobeCountWidget2x2(viewModel, widget.targetCloset)
                    WidgetType.WARDROBE_COUNT_2X4 -> WardrobeCountWidget2x4(viewModel, widget.targetCloset)

                    WidgetType.WARDROBE_LIST_1X4 -> WardrobeListWidget1x4(viewModel, widget.targetCloset)
                    WidgetType.WARDROBE_LIST_2X2 -> WardrobeListWidget2x2(viewModel, widget.targetCloset)
                    WidgetType.WARDROBE_LIST_2X4 -> WardrobeListWidget2x4(viewModel, widget.targetCloset)

                    WidgetType.WARDROBE_SEARCH_1X1 -> WardrobeSearchWidget1x1()
                    WidgetType.WARDROBE_SEARCH_1X2 -> WardrobeSearchWidget1x2()
                    WidgetType.WARDROBE_ADD_ITEM_1X1 -> WardrobeAddItemWidget1x1()
                    WidgetType.WARDROBE_ADD_ITEM_1X2 -> WardrobeAddItemWidget1x2()
                    WidgetType.WARDROBE_ADD_ITEM_2X2 -> WardrobeAddItemWidget2x2(viewModel)

                    WidgetType.FUNCTION_CALENDAR_1X1 -> FunctionShortcutWidget1x1(
                        icon = Icons.Outlined.CalendarMonth,
                        tint = if (distinctColors) Color(0xFF388AF6) else colors.accent,
                        label = "Calendario"
                    )
                    WidgetType.FUNCTION_TOOLS_1X1 -> FunctionShortcutWidget1x1(
                        icon = Icons.Outlined.Construction,
                        tint = if (distinctColors) Color(0xFFEAB308) else colors.accent,
                        label = "Strumenti"
                    )
                    WidgetType.FUNCTION_QUICK_ACTIONS_1X4 -> FunctionQuickActionsWidget1x4(
                        useDistinctColors = distinctColors,
                        isEditMode = isEditMode,
                        onSelectFunction = onNavigateFunction
                    )
                    WidgetType.FUNCTION_QUICK_ACTIONS_2X2 -> FunctionQuickActionsWidget2x2(
                        useDistinctColors = distinctColors,
                        isEditMode = isEditMode,
                        onSelectFunction = onNavigateFunction
                    )
                    WidgetType.FUNCTION_PHOTO_1X1 -> FunctionShortcutWidget1x1(
                        icon = Icons.Outlined.CameraAlt,
                        tint = if (distinctColors) Color(0xFF53B5A2) else colors.accent,
                        label = "Foto"
                    )
                    WidgetType.FUNCTION_PHOTO_2X2 -> FunctionPhotoWidget2x2(viewModel, widget.targetCloset, useDistinctColors = distinctColors)
                    WidgetType.FUNCTION_PHOTO_2X4 -> FunctionPhotoWidget2x4(viewModel, widget.targetCloset, useDistinctColors = distinctColors)

                    WidgetType.FUNCTION_MOVE_1X1 -> FunctionShortcutWidget1x1(
                        icon = Icons.Outlined.SyncAlt,
                        tint = if (distinctColors) Color(0xFF10B981) else colors.accent,
                        label = "Sposta"
                    )
                    WidgetType.FUNCTION_MOVE_1X2 -> FunctionMoveWidget1x2(viewModel, useDistinctColors = distinctColors)
                    WidgetType.FUNCTION_MOVE_2X2 -> FunctionMoveWidget2x2(viewModel, useDistinctColors = distinctColors)

                    WidgetType.FUNCTION_LAUNDRY_1X1 -> FunctionLaundryWidget1x1(viewModel, useDistinctColors = distinctColors)
                    WidgetType.FUNCTION_LAUNDRY_1X2 -> FunctionLaundryWidget1x2(viewModel, useDistinctColors = distinctColors)
                    WidgetType.FUNCTION_LAUNDRY_2X2 -> FunctionLaundryWidget2x2(viewModel, useDistinctColors = distinctColors)
                    WidgetType.FUNCTION_LAUNDRY_2X4 -> FunctionLaundryWidget2x4(viewModel, useDistinctColors = distinctColors)

                    WidgetType.FUNCTION_TRIP_1X1 -> FunctionShortcutWidget1x1(
                        icon = Icons.Outlined.Work,
                        tint = if (distinctColors) Color(0xFF8B5CF6) else colors.accent,
                        label = "Viaggio"
                    )
                    WidgetType.FUNCTION_TRIP_1X2 -> FunctionTripWidget1x2(viewModel, useDistinctColors = distinctColors)
                    WidgetType.FUNCTION_TRIP_2X2 -> FunctionTripWidget2x2(viewModel, useDistinctColors = distinctColors)
                    WidgetType.FUNCTION_TRIP_2X4 -> FunctionTripWidget2x4(viewModel, useDistinctColors = distinctColors)

                    WidgetType.FUNCTION_TRANSFERS_1X1 -> FunctionShortcutWidget1x1(
                        icon = Icons.Outlined.History,
                        tint = if (distinctColors) Color(0xFFF97316) else colors.accent,
                        label = "Registro"
                    )
                    WidgetType.FUNCTION_TRANSFERS_1X2 -> FunctionTransfersWidget1x2(viewModel, useDistinctColors = distinctColors)
                    WidgetType.FUNCTION_TRANSFERS_2X2 -> FunctionTransfersWidget2x2(viewModel, useDistinctColors = distinctColors)

                    WidgetType.UTILITY_TORCH_1X1 -> UtilityTorchWidget1x1(isEditMode = isEditMode)
                    WidgetType.UTILITY_TORCH_1X2 -> UtilityTorchWidget1x2(isEditMode = isEditMode)
                    WidgetType.UTILITY_TORCH_2X2 -> UtilityTorchWidget2x2(isEditMode = isEditMode)

                    WidgetType.UTILITY_COMPASS_1X1 -> UtilityCompassWidget1x1()
                    WidgetType.UTILITY_COMPASS_1X2 -> UtilityCompassWidget1x2()
                    WidgetType.UTILITY_COMPASS_2X2 -> UtilityCompassWidget2x2()
                    WidgetType.UTILITY_COMPASS_2X4 -> UtilityCompassWidget2x4()

                    WidgetType.UTILITY_LEVEL_1X1 -> UtilityLevelWidget1x1()
                    WidgetType.UTILITY_LEVEL_1X2 -> UtilityLevelWidget1x2()
                    WidgetType.UTILITY_LEVEL_2X2 -> UtilityLevelWidget2x2()
                    WidgetType.UTILITY_LEVEL_2X4 -> UtilityLevelWidget2x4()

                    WidgetType.CLOCK_DIGITAL_1X2 -> ClockDigitalWidget1x2()
                    WidgetType.CLOCK_DIGITAL_2X2 -> ClockDigitalWidget2x2()
                    WidgetType.CLOCK_DIGITAL_2X4 -> ClockDigitalWidget2x4()
                    WidgetType.CLOCK_ANALOG_1X1 -> ClockAnalogWidget1x1()
                    WidgetType.CLOCK_ANALOG_2X2 -> ClockAnalogWidget2x2()
                    WidgetType.CLOCK_ANALOG_2X4 -> ClockAnalogWidget2x4(viewModel)
                }
            }
        }

        // 1. Pulsante Rimuovi (-) in alto a sinistra (Rosso stile One UI)
        AnimatedVisibility(
            visible = isEditMode,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = if (is1x1) 0.dp else (-4).dp, y = if (is1x1) 0.dp else (-4).dp)
                .zIndex(20f)
        ) {
            val removeButtonBg = Color(0xFFE55353)
            val removeIconTint = Color.White

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(removeButtonBg)
                    .border(1.5.dp, colors.cardBackground, CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Rimuovi widget",
                    tint = removeIconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // 2. Badge dimensione corrente durante il ridimensionamento (es. "2 × 2") in alto a destra
        AnimatedVisibility(
            visible = isEditMode && isBeingResized,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = if (is1x1) 0.dp else 4.dp, y = if (is1x1) 0.dp else (-4).dp)
                .zIndex(19f)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.accent,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "${widget.type.colSpan} \u00D7 ${widget.type.rowSpan}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        // 3. Cursore di ridimensionamento in basso a destra (maniglia/pillola curva d'angolo stile iOS)
        AnimatedVisibility(
            visible = isEditMode && canResize,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = if (is1x1) (-2).dp else (-3).dp, y = if (is1x1) (-2).dp else (-3).dp)
                .zIndex(18f)
        ) {
            val handleBg = if (isBeingResized) {
                colors.accent
            } else {
                if (colors.isDark) Color(0xEBFFFFFF) else Color(0xF2FFFFFF)
            }
            val handleGripColor = if (isBeingResized) Color.White else Color(0xFF8E8E93)

            Box(
                modifier = Modifier
                    .graphicsLayer { rotationZ = -45f }
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .size(width = if (isBeingResized) 30.dp else 26.dp, height = 9.dp)
                    .clip(CircleShape)
                    .background(handleBg)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(handleGripColor.copy(alpha = 0.6f))
                )
            }
        }
    }
}


