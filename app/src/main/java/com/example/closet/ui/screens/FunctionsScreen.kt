@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.closet.ui.viewmodel.ClosetViewModel
import com.example.closet.ui.viewmodel.TransferHistoryItem
import com.example.closet.ui.viewmodel.TripEndAction
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class FunctionSubScreen {
    WARDROBE,
    CALENDAR,
    CLOSET_PHOTOS,
    MOVE,
    LAUNDRY,
    TRIP,
    TRANSFERS_ACTIVITY,
    TOOLS
}

@Immutable
data class FunctionItemDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val targetSubScreen: FunctionSubScreen?
)

fun suitcaseDestSafe(closets: List<String>, source: String, candidate: String): String {
    return if (candidate != source && closets.contains(candidate)) candidate
    else closets.firstOrNull { it != source } ?: source
}

fun ClosetViewModel.suitcaseDestSafe(closets: List<String>, source: String, candidate: String): String {
    return if (candidate != source && closets.contains(candidate)) candidate
    else closets.firstOrNull { it != source } ?: source
}

@Composable
fun FunctionsScreen(
    viewModel: ClosetViewModel,
    isFunctionEditMode: Boolean = false,
    initialSubScreen: FunctionSubScreen? = null,
    onSubScreenConsumed: () -> Unit = {},
    onEnterFunctionEditMode: () -> Unit = {},
    onSubScreenActiveChange: (Boolean) -> Unit = {},
    onOpenWardrobeAsFunction: () -> Unit = {}
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    var currentSubScreen by remember { mutableStateOf<FunctionSubScreen?>(null) }
    var isFunctionInfoOpen by remember { mutableStateOf(false) }

    LaunchedEffect(currentSubScreen) {
        isFunctionInfoOpen = false
    }

    BackHandler(enabled = isFunctionInfoOpen) {
        isFunctionInfoOpen = false
    }

    LaunchedEffect(initialSubScreen) {
        if (initialSubScreen != null) {
            if (initialSubScreen == FunctionSubScreen.WARDROBE) {
                onSubScreenConsumed()
                onOpenWardrobeAsFunction()
            } else {
                currentSubScreen = initialSubScreen
                onSubScreenConsumed()
            }
        }
    }

    LaunchedEffect(currentSubScreen) {
        onSubScreenActiveChange(currentSubScreen != null)
    }

    BackHandler(enabled = currentSubScreen != null && !isFunctionInfoOpen) {
        currentSubScreen = null
    }

    val useDistinct = viewModel.useDistinctFunctionColors
    val itemsCount = viewModel.clothingItems.size
    val closetsCount = viewModel.availableClosets.size

    val allFunctions = remember(colors.accent, useDistinct, itemsCount, closetsCount) {
        listOf(
            FunctionItemDef(
                id = "WARDROBE",
                title = "Armadio",
                subtitle = "Organizza, cerca e visualizza tutti i tuoi capi per armadio e cartelle",
                icon = Icons.Outlined.Checkroom,
                accentColor = if (useDistinct) Color(0xFF14B8A6) else colors.accent,
                targetSubScreen = FunctionSubScreen.WARDROBE
            ),
            FunctionItemDef(
                id = "CALENDAR",
                title = "Calendario",
                subtitle = "Pianifica outfit, lavaggi ed eventi nel calendario",
                icon = Icons.Outlined.CalendarMonth,
                accentColor = if (useDistinct) Color(0xFF388AF6) else colors.accent,
                targetSubScreen = FunctionSubScreen.CALENDAR
            ),
            FunctionItemDef(
                id = "CLOSET_PHOTOS",
                title = "Foto armadio",
                subtitle = "Panoramica fotografica dei tuoi armadi",
                icon = Icons.Outlined.CameraAlt,
                accentColor = if (useDistinct) Color(0xFF53B5A2) else colors.accent,
                targetSubScreen = FunctionSubScreen.CLOSET_PHOTOS
            ),
            FunctionItemDef(
                id = "MOVE",
                title = "Spostamento",
                subtitle = "Trasferisci capi da un armadio all'altro mantenendo le cartelle",
                icon = Icons.Outlined.SyncAlt,
                accentColor = if (useDistinct) Color(0xFF10B981) else colors.accent,
                targetSubScreen = FunctionSubScreen.MOVE
            ),
            FunctionItemDef(
                id = "LAUNDRY",
                title = "Panni",
                subtitle = "Gestisci lavaggi, durata e capi in transito",
                icon = Icons.Outlined.LocalLaundryService,
                accentColor = if (useDistinct) Color(0xFFEF4444) else colors.accent,
                targetSubScreen = FunctionSubScreen.LAUNDRY
            ),
            FunctionItemDef(
                id = "TRIP",
                title = "Viaggio",
                subtitle = "Valigia virtuale multi-armadio e gestione rientro",
                icon = Icons.Outlined.Work,
                accentColor = if (useDistinct) Color(0xFF8B5CF6) else colors.accent,
                targetSubScreen = FunctionSubScreen.TRIP
            ),
            FunctionItemDef(
                id = "TRANSFERS_ACTIVITY",
                title = "Trasferimenti",
                subtitle = "Trasferimenti attivi e cronologia",
                icon = Icons.Outlined.History,
                accentColor = if (useDistinct) Color(0xFFF97316) else colors.accent,
                targetSubScreen = FunctionSubScreen.TRANSFERS_ACTIVITY
            ),
            FunctionItemDef(
                id = "TOOLS",
                title = "Strumenti",
                subtitle = "Torcia, bussola, livella e cronometro",
                icon = Icons.Outlined.Construction,
                accentColor = if (useDistinct) Color(0xFFEAB308) else colors.accent,
                targetSubScreen = FunctionSubScreen.TOOLS
            )
        )
    }

    val functionOrderSnapshot = viewModel.functionOrder.toList()
    val activeIslandKeys = viewModel.islandTabs.mapNotNull { it.functionKey }.toSet()

    val orderedFunctions: List<FunctionItemDef> = remember(
        functionOrderSnapshot,
        activeIslandKeys,
        allFunctions
    ) {
        val map = allFunctions.associateBy { it.id }
        val visibleKeys = functionOrderSnapshot.filter { it !in activeIslandKeys }.distinct()
        val result = visibleKeys.mapNotNull { map[it] }.toMutableList()
        val existingIds = result.map { it.id }.toMutableSet()
        allFunctions.forEach { def ->
            if (def.id !in activeIslandKeys && def.id !in existingIds) {
                result.add(def)
                existingIds.add(def.id)
            }
        }
        result
    }

    val currentFullOrder by rememberUpdatedState(functionOrderSnapshot)
    val currentIslandKeys by rememberUpdatedState(activeIslandKeys)
    val currentAllFunctions by rememberUpdatedState(allFunctions)

    val commitVisibleFunctionOrder: (List<String>) -> Unit = { newVisibleIds ->
        val islandSet = currentIslandKeys
        val visibleIter = newVisibleIds.iterator()
        val updatedFullOrder = mutableListOf<String>()

        for (key in currentFullOrder) {
            if (key in islandSet) {
                if (key !in updatedFullOrder) updatedFullOrder.add(key)
            } else if (visibleIter.hasNext()) {
                val nextVis = visibleIter.next()
                if (nextVis !in updatedFullOrder) updatedFullOrder.add(nextVis)
            }
        }
        while (visibleIter.hasNext()) {
            val nextVis = visibleIter.next()
            if (nextVis !in updatedFullOrder) updatedFullOrder.add(nextVis)
        }
        currentAllFunctions.forEach { def ->
            if (def.id !in updatedFullOrder) {
                updatedFullOrder.add(def.id)
            }
        }
        viewModel.updateFunctionOrder(updatedFullOrder)
    }

    val emptySpaceGestureModifier = if (!isFunctionEditMode && currentSubScreen == null) {
        Modifier.pointerInput(isFunctionEditMode, currentSubScreen) {
            detectTapGestures(
                onLongPress = {
                    onEnterFunctionEditMode()
                }
            )
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .then(emptySpaceGestureModifier)
    ) {
        AnimatedContent(
            targetState = currentSubScreen,
            label = "functionsSubScreenTransition"
        ) { subScreen ->
            when (subScreen) {
                null -> {
                    val fadeColor = colors.backgroundBrush.let {
                        if (colors.isDark) Color(0xFF121212) else Color(0xFFF7F8FA)
                    }
                    val topFadeColor = when {
                        viewModel.currentTheme == AppTheme.AMOLED -> Color(0xFF000000)
                        colors.isDark -> Color(0xFF13171E)
                        else -> Color(0xFFF2F4F8)
                    }
                    val scrollState = rememberScrollState()
                    val topFadeProgress = (scrollState.value.toFloat() / with(density) { 24.dp.toPx() }).coerceIn(0f, 1f)

                    Box(modifier = Modifier.fillMaxSize()) {
                        FunctionsInteractiveList(
                            orderedFunctions = orderedFunctions,
                            isEditMode = isFunctionEditMode,
                            scrollState = scrollState,
                            onEnterEditMode = onEnterFunctionEditMode,
                            onUpdateVisibleOrder = commitVisibleFunctionOrder,
                            onFunctionClick = { def ->
                                if (def.targetSubScreen == FunctionSubScreen.WARDROBE) {
                                    onOpenWardrobeAsFunction()
                                } else if (def.targetSubScreen != null) {
                                    currentSubScreen = def.targetSubScreen
                                }
                            }
                        )

                        // Sfumatura superiore morbida in uscita
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(38.dp)
                                .graphicsLayer { alpha = topFadeProgress }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            topFadeColor.copy(alpha = 0.70f),
                                            topFadeColor.copy(alpha = 0.30f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .zIndex(15f)
                        )
                    }
                }
                FunctionSubScreen.CALENDAR -> {
                    CalendarScreen(
                        viewModel = viewModel,
                        onBack = { currentSubScreen = null }
                    )
                }
                FunctionSubScreen.CLOSET_PHOTOS -> {
                    ClosetPhotosScreen(
                        viewModel = viewModel,
                        onBack = { currentSubScreen = null }
                    )
                }
                FunctionSubScreen.MOVE -> {
                    MoveClothingScreen(
                        viewModel = viewModel,
                        isLaundryMode = false,
                        onBack = { currentSubScreen = null }
                    )
                }
                FunctionSubScreen.LAUNDRY -> {
                    MoveClothingScreen(
                        viewModel = viewModel,
                        isLaundryMode = true,
                        onBack = { currentSubScreen = null }
                    )
                }
                FunctionSubScreen.TRIP -> {
                    TripScreen(
                        viewModel = viewModel,
                        onBack = { currentSubScreen = null }
                    )
                }
                FunctionSubScreen.TRANSFERS_ACTIVITY -> {
                    TransfersActivityScreen(
                        viewModel = viewModel,
                        onBack = { currentSubScreen = null },
                        onNavigateToLaundry = { currentSubScreen = FunctionSubScreen.LAUNDRY }
                    )
                }
                FunctionSubScreen.TOOLS -> {
                    ToolsScreen(
                        viewModel = viewModel,
                        onBack = { currentSubScreen = null }
                    )
                }
                FunctionSubScreen.WARDROBE -> {
                    LaunchedEffect(Unit) {
                        currentSubScreen = null
                        onSubScreenActiveChange(false)
                        onOpenWardrobeAsFunction()
                    }
                }
            }
        }

        if (isFunctionInfoOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(28f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isFunctionInfoOpen = false }
            )
        }

        // Tasto Info circolare in alto a destra quando una funzione è aperta
        val activeDef = remember(currentSubScreen, allFunctions) {
            allFunctions.find { it.targetSubScreen == currentSubScreen }
        }

        if (activeDef != null && currentSubScreen != FunctionSubScreen.TOOLS) {
            FunctionInfoButtonWithPopup(
                title = activeDef.title,
                description = activeDef.subtitle,
                accentColor = activeDef.accentColor,
                isOpen = isFunctionInfoOpen,
                onToggleOpen = { isFunctionInfoOpen = !isFunctionInfoOpen },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp, top = 18.dp)
                    .zIndex(30f)
            )
        }
    }
}

@Composable
fun FunctionInfoButtonWithPopup(
    title: String,
    description: String,
    accentColor: Color,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 40.dp
) {
    val colors = LocalOneUIColors.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceSecondary,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.size(buttonSize)
        ) {
            IconButton(
                onClick = onToggleOpen,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Informazioni $title",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = 0.35f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + slideInVertically(
                initialOffsetY = { -it / 4 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(130)) + scaleOut(
                targetScale = 0.35f,
                transformOrigin = TransformOrigin(1f, 0f)
            ) + slideOutVertically(targetOffsetY = { -it / 4 }),
            modifier = Modifier
                .padding(top = buttonSize + 8.dp)
                .widthIn(min = 220.dp, max = 290.dp)
                .zIndex(50f)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.dialogBackground,
                border = BorderStroke(0.5.dp, colors.cardBorder),
                tonalElevation = 8.dp,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = title,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Text(
                        text = description,
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FunctionsInteractiveList(
    orderedFunctions: List<FunctionItemDef>,
    isEditMode: Boolean,
    scrollState: ScrollState,
    onEnterEditMode: () -> Unit,
    onUpdateVisibleOrder: (List<String>) -> Unit,
    onFunctionClick: (FunctionItemDef) -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val itemHeightDp = 72.dp
    val itemSpacingDp = 12.dp
    val stepDp = itemHeightDp + itemSpacingDp
    val stepPx = with(density) { stepDp.toPx() }
    val itemHeightPx = with(density) { itemHeightDp.toPx() }
    val handleZoneWidthPx = with(density) { 68.dp.toPx() }

    val workingList = remember {
        mutableStateListOf<FunctionItemDef>().apply { addAll(orderedFunctions) }
    }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var targetHoverIndex by remember { mutableIntStateOf(-1) }
    var dragTouchOffsetY by remember { mutableFloatStateOf(0f) }
    var dragFingerCurrentY by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    var titleSectionHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(orderedFunctions, draggedItemId) {
        if (draggedItemId == null) {
            val currentIds = workingList.map { it.id }
            val incomingIds = orderedFunctions.map { it.id }
            if (currentIds != incomingIds) {
                workingList.clear()
                workingList.addAll(orderedFunctions)
            } else {
                for (i in orderedFunctions.indices) {
                    if (workingList[i] != orderedFunctions[i]) {
                        workingList[i] = orderedFunctions[i]
                    }
                }
            }
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            draggedItemId = null
            dragStartIndex = -1
            targetHoverIndex = -1
            dragTouchOffsetY = 0f
            dragFingerCurrentY = 0f
        }
    }

    val baseList = if (workingList.isNotEmpty()) workingList.toList() else orderedFunctions

    val virtualList = remember(baseList, draggedItemId, targetHoverIndex) {
        val activeId = draggedItemId
        if (activeId != null && targetHoverIndex in baseList.indices) {
            val fromIdx = baseList.indexOfFirst { it.id == activeId }
            if (fromIdx != -1 && fromIdx != targetHoverIndex) {
                val mutable = baseList.toMutableList()
                val moved = mutable.removeAt(fromIdx)
                mutable.add(targetHoverIndex.coerceIn(0, mutable.size), moved)
                mutable
            } else {
                baseList
            }
        } else {
            baseList
        }
    }

    val currentOnEnterEditMode by rememberUpdatedState(onEnterEditMode)
    val currentOnUpdateVisibleOrder by rememberUpdatedState(onUpdateVisibleOrder)
    val currentOnFunctionClick by rememberUpdatedState(onFunctionClick)
    val currentVirtualList by rememberUpdatedState(virtualList)
    val currentBaseCount by rememberUpdatedState(baseList.size)

    // Auto-scroll loop continuo quando il dito raggiunge la zona superiore o inferiore
    LaunchedEffect(draggedItemId) {
        if (draggedItemId == null) return@LaunchedEffect
        val topScrollZonePx = with(density) { 100.dp.toPx() }
        val bottomScrollZonePx = with(density) { 140.dp.toPx() }
        val maxScrollStepPx = with(density) { 12.dp.toPx() }

        while (draggedItemId != null) {
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
                        val currentCardTopPx = dragFingerCurrentY - titleSectionHeightPx + scrollState.value - dragTouchOffsetY
                        val cardCenterPx = currentCardTopPx + (itemHeightPx / 2f)
                        val count = currentBaseCount
                        if (count > 0) {
                            val rawSlot = cardCenterPx / stepPx
                            val prevTarget = targetHoverIndex.coerceIn(0, count - 1)
                            val newTarget = if (abs(rawSlot - prevTarget) > 0.55f) {
                                rawSlot.roundToInt().coerceIn(0, count - 1)
                            } else {
                                prevTarget
                            }
                            if (newTarget != targetHoverIndex) {
                                targetHoverIndex = newTarget
                            }
                        }
                    }
                }
            }
            delay(16)
        }
    }

    val totalHeightDp = (stepDp * baseList.size).coerceAtLeast(0.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewportHeightPx = size.height.toFloat()
            }
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> titleSectionHeightPx = size.height.toFloat() }
        ) {
            AnimatedContent(
                targetState = isEditMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 40)) + slideInVertically { it / 3 })
                        .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutVertically { -it / 3 })
                },
                label = "functionTitleAnim"
            ) { editMode ->
                Text(
                    text = if (editMode) "Modifica funzioni" else "Funzioni",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { })
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightDp)
        ) {
            if (isEditMode && draggedItemId != null && targetHoverIndex in baseList.indices) {
                val dropSlotY by animateDpAsState(
                    targetValue = stepDp * targetHoverIndex,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    ),
                    label = "funcDropSlotY"
                )

                Box(
                    modifier = Modifier
                        .offset(x = 0.dp, y = dropSlotY)
                        .fillMaxWidth()
                        .height(itemHeightDp)
                        .zIndex(0.5f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.accent.copy(alpha = if (colors.isDark) 0.14f else 0.10f))
                        .border(
                            width = 1.5.dp,
                            color = colors.accent.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(22.dp)
                        )
                )
            }

            baseList.forEachIndexed { origIndex, def ->
                key(def.id) {
                    val isDragged = isEditMode && (draggedItemId == def.id)
                    val virtIndex = virtualList.indexOfFirst { it.id == def.id }.let {
                        if (it != -1) it else origIndex
                    }

                    val slotPosYDp = stepDp * virtIndex

                    val animPosY by animateDpAsState(
                        targetValue = slotPosYDp,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        ),
                        label = "funcCardPosY"
                    )

                    val cardScale by animateFloatAsState(
                        targetValue = when {
                            isDragged -> 1.035f
                            isEditMode -> 0.99f
                            else -> 1f
                        },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        ),
                        label = "funcCardScale"
                    )

                    val cardElevation by animateFloatAsState(
                        targetValue = if (isDragged) 24f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "funcCardElevation"
                    )

                    val isSettling = !isDragged && abs(animPosY.value - slotPosYDp.value) > 0.5f
                    val currentOrigIdx by rememberUpdatedState(origIndex)
                    val currentDef by rememberUpdatedState(def)

                    val currentCardTopPx = if (isDragged) {
                        dragFingerCurrentY - titleSectionHeightPx + scrollState.value - dragTouchOffsetY
                    } else 0f
                    val dragVisualTranslationY = if (isDragged) {
                        currentCardTopPx - (stepPx * dragStartIndex)
                    } else 0f

                    Box(
                        modifier = Modifier
                            .offset(
                                x = 0.dp,
                                y = if (isDragged) (stepDp * dragStartIndex) else animPosY
                            )
                            .fillMaxWidth()
                            .height(itemHeightDp)
                            .zIndex(
                                when {
                                    isDragged -> 50f
                                    isSettling -> 10f
                                    else -> 1f
                                }
                            )
                            .pointerInput(def.id, isEditMode) {
                                if (!isEditMode) {
                                    detectTapGestures(
                                        onTap = { currentOnFunctionClick(currentDef) },
                                        onLongPress = { currentOnEnterEditMode() }
                                    )
                                } else {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val isOnHandle = down.position.x >= (size.width - handleZoneWidthPx)
                                        val trigger = if (isOnHandle) {
                                            down.consume()
                                            down
                                        } else {
                                            awaitLongPressOrCancellation(down.id)
                                        }

                                        if (trigger != null && draggedItemId == null) {
                                            val startIdx = currentOrigIdx
                                            draggedItemId = def.id
                                            dragStartIndex = startIdx
                                            targetHoverIndex = startIdx
                                            dragTouchOffsetY = down.position.y
                                            dragFingerCurrentY = titleSectionHeightPx + (stepPx * startIdx) + down.position.y - scrollState.value

                                            var completed = false
                                            try {
                                                completed = drag(down.id) { change ->
                                                    change.consume()
                                                    if (draggedItemId == def.id) {
                                                        dragFingerCurrentY = titleSectionHeightPx + (stepPx * startIdx) + change.position.y - scrollState.value
                                                        val cardTop = dragFingerCurrentY - titleSectionHeightPx + scrollState.value - dragTouchOffsetY
                                                        val cardCenterPx = cardTop + (itemHeightPx / 2f)
                                                        val count = currentBaseCount
                                                        if (count > 0) {
                                                            val rawSlot = cardCenterPx / stepPx
                                                            val prevTarget = targetHoverIndex.coerceIn(0, count - 1)
                                                            val newTarget = if (abs(rawSlot - prevTarget) > 0.55f) {
                                                                rawSlot.roundToInt().coerceIn(0, count - 1)
                                                            } else {
                                                                prevTarget
                                                            }
                                                            if (newTarget != targetHoverIndex) {
                                                                targetHoverIndex = newTarget
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                completed = false
                                            }

                                            if (completed && draggedItemId == def.id) {
                                                val finalOrder = currentVirtualList
                                                workingList.clear()
                                                workingList.addAll(finalOrder)
                                                currentOnUpdateVisibleOrder(finalOrder.map { it.id })
                                            }

                                            draggedItemId = null
                                            dragStartIndex = -1
                                            targetHoverIndex = -1
                                            dragTouchOffsetY = 0f
                                            dragFingerCurrentY = 0f
                                        }
                                    }
                                }
                            }
                            .graphicsLayer {
                                shape = RoundedCornerShape(22.dp)
                                clip = false
                                scaleX = cardScale
                                scaleY = cardScale
                                translationY = dragVisualTranslationY
                                shadowElevation = cardElevation
                            }
                    ) {
                        FunctionInteractiveCard(
                            item = def,
                            isEditMode = isEditMode,
                            isDragged = isDragged
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun FunctionInteractiveCard(
    item: FunctionItemDef,
    isEditMode: Boolean,
    isDragged: Boolean
) {
    val colors = LocalOneUIColors.current

    val borderColor by animateColorAsState(
        targetValue = when {
            isDragged -> item.accentColor
            isEditMode -> item.accentColor.copy(alpha = 0.65f)
            else -> colors.cardBorder
        },
        animationSpec = tween(200),
        label = "funcBorderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = when {
            isDragged -> 1.5.dp
            isEditMode -> 1.dp
            else -> 0.5.dp
        },
        animationSpec = tween(200),
        label = "funcBorderWidth"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isDragged) 6.dp else 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                        .background(item.accentColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.accentColor,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isEditMode,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.75f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(targetScale = 0.75f))
                    },
                    label = "funcRightIconMorph"
                ) { edit ->
                    if (edit) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Trascina per riordinare",
                            tint = item.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
