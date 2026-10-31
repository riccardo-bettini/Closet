package com.example.closet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable
data class DraftClosetItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalName: String?,
    var name: String
)

@Stable
data class DraftTagItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalName: String?,
    var name: String
)

// ============================================================================
// 1. POPUP GESTIONE ARMADI (Riscritto da zero con stile One UI unificato)
// ============================================================================
@Composable
fun ManageClosetsDialog(
    viewModel: ClosetViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val dialogScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "closetDialogScale"
    )
    val dialogAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(180),
        label = "closetDialogAlpha"
    )

    val draftClosets = remember { mutableStateListOf<DraftClosetItem>() }
    val initialClosetsList = remember { viewModel.availableClosets.toList() }

    LaunchedEffect(viewModel.availableClosets.toList()) {
        if (draftClosets.isEmpty()) {
            draftClosets.addAll(
                viewModel.availableClosets.map { DraftClosetItem(originalName = it, name = it) }
            )
        }
    }

    val clothingItems = viewModel.clothingItems
    val itemCountsByCloset = remember(clothingItems.toList()) {
        clothingItems.groupingBy { it.closetName }.eachCount()
    }

    var newName by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var closetPendingDelete by remember { mutableStateOf<DraftClosetItem?>(null) }

    // Costanti geometriche per il motore di trascinamento fluido
    val itemHeightDp = 58.dp
    val itemSpacingDp = 8.dp
    val stepDp = itemHeightDp + itemSpacingDp
    val stepPx = with(density) { stepDp.toPx() }
    val itemHeightPx = with(density) { itemHeightDp.toPx() }
    val handleZoneWidthPx = with(density) { 42.dp.toPx() }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var targetHoverIndex by remember { mutableIntStateOf(-1) }
    var dragTouchOffsetY by remember { mutableFloatStateOf(0f) }
    var dragFingerCurrentY by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    // Animazione morbida di atterraggio al rilascio della scheda
    var settlingItemId by remember { mutableStateOf<String?>(null) }
    val releaseOffsetY = remember { Animatable(0f) }

    val baseList = draftClosets.toList()

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

    val currentVirtualList by rememberUpdatedState(virtualList)
    val currentBaseCount by rememberUpdatedState(baseList.size)

    // Auto-scroll continuo a 60fps quando il dito si avvicina al bordo superiore o inferiore
    LaunchedEffect(draggedItemId) {
        if (draggedItemId == null) return@LaunchedEffect
        val edgeScrollZonePx = with(density) { 58.dp.toPx() }
        val maxScrollStepPx = with(density) { 10.dp.toPx() }

        while (draggedItemId != null) {
            if (viewportHeightPx > 0f) {
                val fingerY = dragFingerCurrentY
                val topZone = edgeScrollZonePx
                val bottomZone = viewportHeightPx - edgeScrollZonePx

                val scrollDelta: Float = when {
                    fingerY < topZone && scrollState.value > 0 -> {
                        val factor = ((topZone - fingerY) / topZone).coerceIn(0.2f, 1f)
                        -maxScrollStepPx * factor
                    }
                    fingerY > bottomZone && scrollState.value < scrollState.maxValue -> {
                        val factor = ((fingerY - bottomZone) / edgeScrollZonePx).coerceIn(0.2f, 1f)
                        maxScrollStepPx * factor
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    val targetScroll = (scrollState.value + scrollDelta.roundToInt())
                        .coerceIn(0, scrollState.maxValue)
                    if (targetScroll != scrollState.value) {
                        scrollState.scrollTo(targetScroll)
                        val count = currentBaseCount
                        if (count > 0) {
                            val maxCardTopPx = ((count - 1).coerceAtLeast(0)) * stepPx
                            val currentCardTopPx = (dragFingerCurrentY + scrollState.value - dragTouchOffsetY)
                                .coerceIn(0f, maxCardTopPx)
                            val cardCenterPx = currentCardTopPx + (itemHeightPx / 2f)
                            val rawSlot = cardCenterPx / stepPx
                            val prevTarget = targetHoverIndex.coerceIn(0, count - 1)
                            val newTarget = if (abs(rawSlot - prevTarget) > 0.52f) {
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

    fun checkAndDismiss() {
        val currentNames = draftClosets.map { it.name }
        if (currentNames != initialClosetsList) {
            showDiscardConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { checkAndDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = colors.dialogBackground,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .heightIn(max = 650.dp)
                .graphicsLayer {
                    scaleX = dialogScale
                    scaleY = dialogScale
                    alpha = dialogAlpha
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Stile One UI Unificato
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Gestione armadi",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${draftClosets.size} armadi • Usa ≡ o tieni premuto per riordinare",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = colors.accent.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier
                                .padding(9.dp)
                                .size(19.dp)
                        )
                    }
                }

                // Campo di inserimento nuovo armadio
                val inputBorderColor by animateColorAsState(
                    targetValue = if (newName.isNotBlank()) colors.accent else colors.cardBorder.copy(alpha = 0.75f),
                    animationSpec = tween(180),
                    label = "closetInputBorder"
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceSecondary,
                    border = BorderStroke(1.dp, inputBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 14.dp, end = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreateNewFolder,
                            contentDescription = null,
                            tint = if (newName.isNotBlank()) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (newName.isEmpty()) {
                                Text(
                                    text = "Aggiungi nuovo armadio...",
                                    color = colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                            BasicTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                singleLine = true,
                                textStyle = ComposeTextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                cursorBrush = SolidColor(colors.accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val canAdd = newName.trim().isNotBlank() &&
                                !draftClosets.any { it.name.equals(newName.trim(), ignoreCase = true) }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (canAdd) colors.accent else colors.accent.copy(alpha = 0.35f))
                                .clickable(enabled = canAdd) {
                                    val clean = newName.trim()
                                    if (clean.isNotBlank()) {
                                        draftClosets.add(DraftClosetItem(originalName = null, name = clean))
                                        newName = ""
                                        coroutineScope.launch {
                                            delay(60)
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Aggiungi",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Contenitore lista interattiva con posizionamento assoluto fluido e auto-scroll
                val totalHeightDp = ((stepDp * baseList.size) - itemSpacingDp).coerceAtLeast(itemHeightDp)
                val containerBg = colors.surfaceSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.65f)

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = containerBg,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    viewportHeightPx = size.height.toFloat()
                                }
                                .verticalScroll(scrollState, enabled = draggedItemId == null)
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(totalHeightDp)
                            ) {
                                // Sagoma luminosa dello slot di destinazione durante il trascinamento
                                if (draggedItemId != null && targetHoverIndex in baseList.indices) {
                                    val dropSlotY by animateDpAsState(
                                        targetValue = stepDp * targetHoverIndex,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            dampingRatio = Spring.DampingRatioNoBouncy
                                        ),
                                        label = "closetDropSlotY"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .offset(x = 0.dp, y = dropSlotY)
                                            .fillMaxWidth()
                                            .height(itemHeightDp)
                                            .zIndex(0.5f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(colors.accent.copy(alpha = if (colors.isDark) 0.16f else 0.11f))
                                            .border(
                                                width = 1.5.dp,
                                                color = colors.accent.copy(alpha = 0.65f),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                    )
                                }

                                baseList.forEachIndexed { origIndex, item ->
                                    key(item.id) {
                                        val isDragged = (draggedItemId == item.id)
                                        val isSettling = (settlingItemId == item.id)
                                        val isEditing = (editingId == item.id)

                                        val virtIndex = virtualList.indexOfFirst { it.id == item.id }.let {
                                            if (it != -1) it else origIndex
                                        }
                                        val slotPosYDp = stepDp * virtIndex

                                        val animPosY by animateDpAsState(
                                            targetValue = slotPosYDp,
                                            animationSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            ),
                                            label = "closetCardPosY"
                                        )

                                        val cardScale by animateFloatAsState(
                                            targetValue = if (isDragged) 1.03f else 1f,
                                            animationSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            ),
                                            label = "closetCardScale"
                                        )

                                        val cardElevation by animateFloatAsState(
                                            targetValue = if (isDragged) 20f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                            label = "closetCardElevation"
                                        )

                                        val borderColor by animateColorAsState(
                                            targetValue = when {
                                                isDragged || isEditing -> colors.accent
                                                else -> colors.cardBorder
                                            },
                                            animationSpec = tween(180),
                                            label = "closetBorderColor"
                                        )

                                        val isMoving = !isDragged && abs(animPosY.value - slotPosYDp.value) > 0.5f
                                        val currentOrigIdx by rememberUpdatedState(origIndex)

                                        val maxCardTopPx = ((baseList.size - 1).coerceAtLeast(0)) * stepPx
                                        val currentCardTopPx = if (isDragged) {
                                            (dragFingerCurrentY + scrollState.value - dragTouchOffsetY)
                                                .coerceIn(0f, maxCardTopPx)
                                        } else 0f

                                        val dragVisualTranslationY = when {
                                            isDragged -> currentCardTopPx - (stepPx * dragStartIndex)
                                            isSettling -> releaseOffsetY.value
                                            else -> 0f
                                        }

                                        val itemsCount = itemCountsByCloset[item.originalName ?: item.name] ?: 0

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
                                                        isSettling -> 30f
                                                        isMoving -> 10f
                                                        else -> 1f
                                                    }
                                                )
                                                .pointerInput(item.id, editingId) {
                                                    if (editingId == null) {
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
                                                                settlingItemId = null
                                                                draggedItemId = item.id
                                                                dragStartIndex = startIdx
                                                                targetHoverIndex = startIdx
                                                                dragTouchOffsetY = down.position.y
                                                                dragFingerCurrentY = (stepPx * startIdx) + down.position.y - scrollState.value

                                                                var completed = false
                                                                try {
                                                                    completed = drag(down.id) { change ->
                                                                        change.consume()
                                                                        if (draggedItemId == item.id) {
                                                                            dragFingerCurrentY = (stepPx * startIdx) + change.position.y - scrollState.value
                                                                            val count = currentBaseCount
                                                                            if (count > 0) {
                                                                                val maxTop = ((count - 1).coerceAtLeast(0)) * stepPx
                                                                                val cardTop = (dragFingerCurrentY + scrollState.value - dragTouchOffsetY)
                                                                                    .coerceIn(0f, maxTop)
                                                                                val cardCenterPx = cardTop + (itemHeightPx / 2f)
                                                                                val rawSlot = cardCenterPx / stepPx
                                                                                val prevTarget = targetHoverIndex.coerceIn(0, count - 1)
                                                                                val newTarget = if (abs(rawSlot - prevTarget) > 0.52f) {
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

                                                                val finalTarget = targetHoverIndex.coerceIn(0, (currentBaseCount - 1).coerceAtLeast(0))
                                                                val maxTop = ((currentBaseCount - 1).coerceAtLeast(0)) * stepPx
                                                                val endCardTopPx = (dragFingerCurrentY + scrollState.value - dragTouchOffsetY)
                                                                    .coerceIn(0f, maxTop)
                                                                val residualPx = endCardTopPx - (stepPx * finalTarget)

                                                                if (completed && draggedItemId == item.id) {
                                                                    val finalOrder = currentVirtualList
                                                                    draftClosets.clear()
                                                                    draftClosets.addAll(finalOrder)
                                                                }

                                                                draggedItemId = null
                                                                dragStartIndex = -1
                                                                targetHoverIndex = -1
                                                                dragTouchOffsetY = 0f
                                                                dragFingerCurrentY = 0f

                                                                if (abs(residualPx) > 0.5f) {
                                                                    settlingItemId = item.id
                                                                    coroutineScope.launch {
                                                                        releaseOffsetY.snapTo(residualPx)
                                                                        releaseOffsetY.animateTo(
                                                                            targetValue = 0f,
                                                                            animationSpec = spring(
                                                                                stiffness = Spring.StiffnessMedium,
                                                                                dampingRatio = Spring.DampingRatioNoBouncy
                                                                            )
                                                                        )
                                                                        if (settlingItemId == item.id) {
                                                                            settlingItemId = null
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                .graphicsLayer {
                                                    shape = RoundedCornerShape(18.dp)
                                                    clip = false
                                                    scaleX = cardScale
                                                    scaleY = cardScale
                                                    translationY = dragVisualTranslationY
                                                    shadowElevation = cardElevation
                                                }
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = colors.cardBackground,
                                                border = BorderStroke(
                                                    width = if (isDragged || isEditing) 1.5.dp else 0.5.dp,
                                                    color = borderColor
                                                ),
                                                tonalElevation = if (isDragged) 6.dp else 1.dp,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                AnimatedContent(
                                                    targetState = isEditing,
                                                    transitionSpec = {
                                                        fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                                                    },
                                                    label = "closetRowEditTransition"
                                                ) { editing ->
                                                    if (editing) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 12.dp),
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
                                                                        .size(34.dp)
                                                                        .clip(CircleShape)
                                                                        .background(colors.accent.copy(alpha = 0.18f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Edit,
                                                                        contentDescription = null,
                                                                        tint = colors.accent,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }

                                                                BasicTextField(
                                                                    value = editingText,
                                                                    onValueChange = { editingText = it },
                                                                    singleLine = true,
                                                                    textStyle = ComposeTextStyle(
                                                                        color = colors.textPrimary,
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                                                    ),
                                                                    cursorBrush = SolidColor(colors.accent),
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            }

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                IconButton(
                                                                    onClick = {
                                                                        val clean = editingText.trim()
                                                                        if (clean.isNotBlank()) {
                                                                            val idx = draftClosets.indexOfFirst { it.id == item.id }
                                                                            if (idx != -1) {
                                                                                draftClosets[idx] = draftClosets[idx].copy(name = clean)
                                                                            }
                                                                            editingId = null
                                                                        }
                                                                    },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = "Conferma",
                                                                        tint = colors.accent,
                                                                        modifier = Modifier.size(19.dp)
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = { editingId = null },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Close,
                                                                        contentDescription = "Annulla",
                                                                        tint = colors.textSecondary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(start = 12.dp, end = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(36.dp)
                                                                        .clip(CircleShape)
                                                                        .background(
                                                                            colors.accent.copy(
                                                                                alpha = if (colors.isDark) 0.22f else 0.14f
                                                                            )
                                                                        ),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.MeetingRoom,
                                                                        contentDescription = null,
                                                                        tint = colors.accent,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }

                                                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                                    Text(
                                                                        text = item.name,
                                                                        color = colors.textPrimary,
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        maxLines = 1,
                                                                        modifier = Modifier.basicMarquee()
                                                                    )
                                                                    Text(
                                                                        text = if (item.originalName == null) "Nuovo armadio" else "$itemsCount capi",
                                                                        color = if (item.originalName == null) colors.accent else colors.textSecondary,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                }
                                                            }

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                            ) {
                                                                IconButton(
                                                                    onClick = {
                                                                        editingId = item.id
                                                                        editingText = item.name
                                                                    },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Edit,
                                                                        contentDescription = "Rinomina",
                                                                        tint = colors.textSecondary,
                                                                        modifier = Modifier.size(17.dp)
                                                                    )
                                                                }

                                                                IconButton(
                                                                    onClick = {
                                                                        if (draftClosets.size > 1) {
                                                                            closetPendingDelete = item
                                                                        }
                                                                    },
                                                                    enabled = draftClosets.size > 1,
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Delete,
                                                                        contentDescription = "Elimina",
                                                                        tint = if (draftClosets.size > 1) Color(0xFFE55353) else colors.textMuted.copy(alpha = 0.4f),
                                                                        modifier = Modifier.size(17.dp)
                                                                    )
                                                                }

                                                                Box(
                                                                    modifier = Modifier.size(34.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Menu,
                                                                        contentDescription = "Trascina per riordinare",
                                                                        tint = if (isDragged) colors.accent else colors.textSecondary.copy(alpha = 0.7f),
                                                                        modifier = Modifier.size(19.dp)
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

                        // Leggera sfumatura superiore/inferiore quando la lista scorre
                        if (scrollState.value > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(containerBg, Color.Transparent)
                                        )
                                    )
                                    .zIndex(5f)
                            )
                        }
                        if (scrollState.value < scrollState.maxValue) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, containerBg)
                                        )
                                    )
                                    .zIndex(5f)
                            )
                        }
                    }
                }

                // Pulsanti Azione Inferiori
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceSecondary,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .clickable { checkAndDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Annulla",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val keptOriginalNames = draftClosets.mapNotNull { it.originalName }.toSet()
                            val toDelete = viewModel.availableClosets.filterNot { keptOriginalNames.contains(it) }
                            toDelete.forEach { viewModel.deleteCloset(it) }

                            draftClosets.forEach { draft ->
                                if (draft.originalName != null && draft.originalName != draft.name) {
                                    viewModel.renameCloset(draft.originalName, draft.name)
                                }
                            }

                            draftClosets.forEach { draft ->
                                if (draft.originalName == null) {
                                    viewModel.addCloset(draft.name)
                                }
                            }

                            val finalOrder = draftClosets.map { it.name }
                            viewModel.setClosetsList(finalOrder)

                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Salva",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Annullare le modifiche?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tutte le modifiche non salvate alla gestione armadi andranno perse.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) {
                    Text("Annulla modifiche", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Continua a modificare", color = colors.textPrimary)
                }
            }
        )
    }

    if (closetPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { closetPendingDelete = null },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Eliminare armadio?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sei sicuro di voler eliminare l'armadio \"${closetPendingDelete?.name}\"? La rimozione diventerà effettiva al salvataggio.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftClosets.size > 1) {
                            draftClosets.remove(closetPendingDelete)
                        }
                        closetPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) {
                    Text("Elimina", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { closetPendingDelete = null }) {
                    Text("Annulla", color = colors.textPrimary)
                }
            }
        )
    }
}

// ============================================================================
// 2. POPUP GESTIONE TAG (Riscritto da zero con le stesse direttive tecniche)
// ============================================================================
@Composable
fun ManageTagsDialog(
    viewModel: ClosetViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val dialogScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tagDialogScale"
    )
    val dialogAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(180),
        label = "tagDialogAlpha"
    )

    val draftTags = remember { mutableStateListOf<DraftTagItem>() }
    val initialTagsList = remember { viewModel.availableTags.toList() }

    LaunchedEffect(viewModel.availableTags.toList()) {
        if (draftTags.isEmpty()) {
            draftTags.addAll(
                viewModel.availableTags.map { DraftTagItem(originalName = it, name = it) }
            )
        }
    }

    val clothingItems = viewModel.clothingItems
    val itemCountsByTag = remember(clothingItems.toList()) {
        val counts = mutableMapOf<String, Int>()
        clothingItems.forEach { item ->
            item.tags.forEach { tag ->
                counts[tag] = (counts[tag] ?: 0) + 1
            }
        }
        counts
    }

    var newTag by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var tagPendingDelete by remember { mutableStateOf<DraftTagItem?>(null) }

    // Costanti geometriche identiche per il motore di trascinamento fluido
    val itemHeightDp = 58.dp
    val itemSpacingDp = 8.dp
    val stepDp = itemHeightDp + itemSpacingDp
    val stepPx = with(density) { stepDp.toPx() }
    val itemHeightPx = with(density) { itemHeightDp.toPx() }
    val handleZoneWidthPx = with(density) { 42.dp.toPx() }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var targetHoverIndex by remember { mutableIntStateOf(-1) }
    var dragTouchOffsetY by remember { mutableFloatStateOf(0f) }
    var dragFingerCurrentY by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    // Animazione morbida di atterraggio al rilascio del tag
    var settlingItemId by remember { mutableStateOf<String?>(null) }
    val releaseOffsetY = remember { Animatable(0f) }

    val baseList = draftTags.toList()

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

    val currentVirtualList by rememberUpdatedState(virtualList)
    val currentBaseCount by rememberUpdatedState(baseList.size)

    // Auto-scroll continuo a 60fps quando il dito si avvicina al bordo superiore o inferiore
    LaunchedEffect(draggedItemId) {
        if (draggedItemId == null) return@LaunchedEffect
        val edgeScrollZonePx = with(density) { 58.dp.toPx() }
        val maxScrollStepPx = with(density) { 10.dp.toPx() }

        while (draggedItemId != null) {
            if (viewportHeightPx > 0f) {
                val fingerY = dragFingerCurrentY
                val topZone = edgeScrollZonePx
                val bottomZone = viewportHeightPx - edgeScrollZonePx

                val scrollDelta: Float = when {
                    fingerY < topZone && scrollState.value > 0 -> {
                        val factor = ((topZone - fingerY) / topZone).coerceIn(0.2f, 1f)
                        -maxScrollStepPx * factor
                    }
                    fingerY > bottomZone && scrollState.value < scrollState.maxValue -> {
                        val factor = ((fingerY - bottomZone) / edgeScrollZonePx).coerceIn(0.2f, 1f)
                        maxScrollStepPx * factor
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    val targetScroll = (scrollState.value + scrollDelta.roundToInt())
                        .coerceIn(0, scrollState.maxValue)
                    if (targetScroll != scrollState.value) {
                        scrollState.scrollTo(targetScroll)
                        val count = currentBaseCount
                        if (count > 0) {
                            val maxCardTopPx = ((count - 1).coerceAtLeast(0)) * stepPx
                            val currentCardTopPx = (dragFingerCurrentY + scrollState.value - dragTouchOffsetY)
                                .coerceIn(0f, maxCardTopPx)
                            val cardCenterPx = currentCardTopPx + (itemHeightPx / 2f)
                            val rawSlot = cardCenterPx / stepPx
                            val prevTarget = targetHoverIndex.coerceIn(0, count - 1)
                            val newTarget = if (abs(rawSlot - prevTarget) > 0.52f) {
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

    fun checkAndDismiss() {
        val currentNames = draftTags.map { it.name }
        if (currentNames != initialTagsList) {
            showDiscardConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { checkAndDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = colors.dialogBackground,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .heightIn(max = 650.dp)
                .graphicsLayer {
                    scaleX = dialogScale
                    scaleY = dialogScale
                    alpha = dialogAlpha
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Stile One UI Unificato
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Gestione tag",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${draftTags.size} tag • Usa ≡ o tieni premuto per riordinare",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = colors.accent.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Label,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier
                                .padding(9.dp)
                                .size(19.dp)
                        )
                    }
                }

                // Campo di inserimento nuovo tag
                val inputBorderColor by animateColorAsState(
                    targetValue = if (newTag.isNotBlank()) colors.accent else colors.cardBorder.copy(alpha = 0.75f),
                    animationSpec = tween(180),
                    label = "tagInputBorder"
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceSecondary,
                    border = BorderStroke(1.dp, inputBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 14.dp, end = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NewLabel,
                            contentDescription = null,
                            tint = if (newTag.isNotBlank()) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (newTag.isEmpty()) {
                                Text(
                                    text = "Aggiungi nuovo tag (es. Casual)...",
                                    color = colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                            BasicTextField(
                                value = newTag,
                                onValueChange = { newTag = it },
                                singleLine = true,
                                textStyle = ComposeTextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                cursorBrush = SolidColor(colors.accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val canAdd = newTag.trim().isNotBlank() &&
                                !draftTags.any { it.name.equals(newTag.trim(), ignoreCase = true) }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (canAdd) colors.accent else colors.accent.copy(alpha = 0.35f))
                                .clickable(enabled = canAdd) {
                                    val clean = newTag.trim()
                                    if (clean.isNotBlank()) {
                                        draftTags.add(DraftTagItem(originalName = null, name = clean))
                                        newTag = ""
                                        coroutineScope.launch {
                                            delay(60)
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Aggiungi",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Contenitore lista interattiva con posizionamento assoluto fluido e auto-scroll
                val totalHeightDp = ((stepDp * baseList.size) - itemSpacingDp).coerceAtLeast(itemHeightDp)
                val containerBg = colors.surfaceSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.65f)

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = containerBg,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp)
                    ) {
                        if (baseList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nessun tag presente",
                                    color = colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        viewportHeightPx = size.height.toFloat()
                                    }
                                    .verticalScroll(scrollState, enabled = draggedItemId == null)
                                    .padding(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(totalHeightDp)
                                ) {
                                    // Sagoma luminosa dello slot di destinazione durante il trascinamento
                                    if (draggedItemId != null && targetHoverIndex in baseList.indices) {
                                        val dropSlotY by animateDpAsState(
                                            targetValue = stepDp * targetHoverIndex,
                                            animationSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            ),
                                            label = "tagDropSlotY"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .offset(x = 0.dp, y = dropSlotY)
                                                .fillMaxWidth()
                                                .height(itemHeightDp)
                                                .zIndex(0.5f)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(colors.accent.copy(alpha = if (colors.isDark) 0.16f else 0.11f))
                                                .border(
                                                    width = 1.5.dp,
                                                    color = colors.accent.copy(alpha = 0.65f),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                        )
                                    }

                                    baseList.forEachIndexed { origIndex, item ->
                                        key(item.id) {
                                            val isDragged = (draggedItemId == item.id)
                                            val isSettling = (settlingItemId == item.id)
                                            val isEditing = (editingId == item.id)

                                            val virtIndex = virtualList.indexOfFirst { it.id == item.id }.let {
                                                if (it != -1) it else origIndex
                                            }
                                            val slotPosYDp = stepDp * virtIndex

                                            val animPosY by animateDpAsState(
                                                targetValue = slotPosYDp,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                ),
                                                label = "tagCardPosY"
                                            )

                                            val cardScale by animateFloatAsState(
                                                targetValue = if (isDragged) 1.03f else 1f,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                ),
                                                label = "tagCardScale"
                                            )

                                            val cardElevation by animateFloatAsState(
                                                targetValue = if (isDragged) 20f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                                label = "tagCardElevation"
                                            )

                                            val borderColor by animateColorAsState(
                                                targetValue = when {
                                                    isDragged || isEditing -> colors.accent
                                                    else -> colors.cardBorder
                                                },
                                                animationSpec = tween(180),
                                                label = "tagBorderColor"
                                            )

                                            val isMoving = !isDragged && abs(animPosY.value - slotPosYDp.value) > 0.5f
                                            val currentOrigIdx by rememberUpdatedState(origIndex)

                                            val maxCardTopPx = ((baseList.size - 1).coerceAtLeast(0)) * stepPx
                                            val currentCardTopPx = if (isDragged) {
                                                (dragFingerCurrentY + scrollState.value - dragTouchOffsetY)
                                                    .coerceIn(0f, maxCardTopPx)
                                            } else 0f

                                            val dragVisualTranslationY = when {
                                                isDragged -> currentCardTopPx - (stepPx * dragStartIndex)
                                                isSettling -> releaseOffsetY.value
                                                else -> 0f
                                            }

                                            val itemsCount = itemCountsByTag[item.originalName ?: item.name] ?: 0

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
                                                            isSettling -> 30f
                                                            isMoving -> 10f
                                                            else -> 1f
                                                        }
                                                    )
                                                    .pointerInput(item.id, editingId) {
                                                        if (editingId == null) {
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
                                                                    settlingItemId = null
                                                                    draggedItemId = item.id
                                                                    dragStartIndex = startIdx
                                                                    targetHoverIndex = startIdx
                                                                    dragTouchOffsetY = down.position.y
                                                                    dragFingerCurrentY = (stepPx * startIdx) + down.position.y - scrollState.value

                                                                    var completed = false
                                                                    try {
                                                                        completed = drag(down.id) { change ->
                                                                            change.consume()
                                                                            if (draggedItemId == item.id) {
                                                                                dragFingerCurrentY = (stepPx * startIdx) + change.position.y - scrollState.value
                                                                                val count = currentBaseCount
                                                                                if (count > 0) {
                                                                                    val maxTop = ((count - 1).coerceAtLeast(0)) * stepPx
                                                                                    val cardTop = (dragFingerCurrentY + scrollState.value - dragTouchOffsetY)
                                                                                        .coerceIn(0f, maxTop)
                                                                                    val cardCenterPx = cardTop + (itemHeightPx / 2f)
                                                                                    val rawSlot = cardCenterPx / stepPx
                                                                                    val prevTarget = targetHoverIndex.coerceIn(0, count - 1)
                                                                                    val newTarget = if (abs(rawSlot - prevTarget) > 0.52f) {
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

                                                                    val finalTarget = targetHoverIndex.coerceIn(0, (currentBaseCount - 1).coerceAtLeast(0))
                                                                    val maxTop = ((currentBaseCount - 1).coerceAtLeast(0)) * stepPx
                                                                    val endCardTopPx = (dragFingerCurrentY + scrollState.value - dragTouchOffsetY)
                                                                        .coerceIn(0f, maxTop)
                                                                    val residualPx = endCardTopPx - (stepPx * finalTarget)

                                                                    if (completed && draggedItemId == item.id) {
                                                                        val finalOrder = currentVirtualList
                                                                        draftTags.clear()
                                                                        draftTags.addAll(finalOrder)
                                                                    }

                                                                    draggedItemId = null
                                                                    dragStartIndex = -1
                                                                    targetHoverIndex = -1
                                                                    dragTouchOffsetY = 0f
                                                                    dragFingerCurrentY = 0f

                                                                    if (abs(residualPx) > 0.5f) {
                                                                        settlingItemId = item.id
                                                                        coroutineScope.launch {
                                                                            releaseOffsetY.snapTo(residualPx)
                                                                            releaseOffsetY.animateTo(
                                                                                targetValue = 0f,
                                                                                animationSpec = spring(
                                                                                    stiffness = Spring.StiffnessMedium,
                                                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                                                )
                                                                            )
                                                                            if (settlingItemId == item.id) {
                                                                                settlingItemId = null
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .graphicsLayer {
                                                        shape = RoundedCornerShape(18.dp)
                                                        clip = false
                                                        scaleX = cardScale
                                                        scaleY = cardScale
                                                        translationY = dragVisualTranslationY
                                                        shadowElevation = cardElevation
                                                    }
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(18.dp),
                                                    color = colors.cardBackground,
                                                    border = BorderStroke(
                                                        width = if (isDragged || isEditing) 1.5.dp else 0.5.dp,
                                                        color = borderColor
                                                    ),
                                                    tonalElevation = if (isDragged) 6.dp else 1.dp,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    AnimatedContent(
                                                        targetState = isEditing,
                                                        transitionSpec = {
                                                            fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                                                        },
                                                        label = "tagRowEditTransition"
                                                    ) { editing ->
                                                        if (editing) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .padding(horizontal = 12.dp),
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
                                                                            .size(34.dp)
                                                                            .clip(CircleShape)
                                                                            .background(colors.accent.copy(alpha = 0.18f)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Outlined.Edit,
                                                                            contentDescription = null,
                                                                            tint = colors.accent,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }

                                                                    BasicTextField(
                                                                        value = editingText,
                                                                        onValueChange = { editingText = it },
                                                                        singleLine = true,
                                                                        textStyle = ComposeTextStyle(
                                                                            color = colors.textPrimary,
                                                                            fontSize = 14.sp,
                                                                            fontWeight = FontWeight.SemiBold,
                                                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                                                        ),
                                                                        cursorBrush = SolidColor(colors.accent),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    )
                                                                }

                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    IconButton(
                                                                        onClick = {
                                                                            val clean = editingText.trim()
                                                                            if (clean.isNotBlank()) {
                                                                                val idx = draftTags.indexOfFirst { it.id == item.id }
                                                                                if (idx != -1) {
                                                                                    draftTags[idx] = draftTags[idx].copy(name = clean)
                                                                                }
                                                                                editingId = null
                                                                            }
                                                                        },
                                                                        modifier = Modifier.size(32.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = "Conferma",
                                                                            tint = colors.accent,
                                                                            modifier = Modifier.size(19.dp)
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = { editingId = null },
                                                                        modifier = Modifier.size(32.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Close,
                                                                            contentDescription = "Annulla",
                                                                            tint = colors.textSecondary,
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .padding(start = 12.dp, end = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                                                                    modifier = Modifier.weight(1f)
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(36.dp)
                                                                            .clip(CircleShape)
                                                                            .background(
                                                                                colors.accent.copy(
                                                                                    alpha = if (colors.isDark) 0.22f else 0.14f
                                                                                )
                                                                            ),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Outlined.Label,
                                                                            contentDescription = null,
                                                                            tint = colors.accent,
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                    }

                                                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                                        Text(
                                                                            text = item.name,
                                                                            color = colors.textPrimary,
                                                                            fontSize = 14.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            maxLines = 1,
                                                                            modifier = Modifier.basicMarquee()
                                                                        )
                                                                        Text(
                                                                            text = if (item.originalName == null) "Nuovo tag" else "$itemsCount capi",
                                                                            color = if (item.originalName == null) colors.accent else colors.textSecondary,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Medium
                                                                        )
                                                                    }
                                                                }

                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                                ) {
                                                                    IconButton(
                                                                        onClick = {
                                                                            editingId = item.id
                                                                            editingText = item.name
                                                                        },
                                                                        modifier = Modifier.size(32.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Outlined.Edit,
                                                                            contentDescription = "Modifica tag",
                                                                            tint = colors.textSecondary,
                                                                            modifier = Modifier.size(17.dp)
                                                                        )
                                                                    }

                                                                    IconButton(
                                                                        onClick = {
                                                                            tagPendingDelete = item
                                                                        },
                                                                        modifier = Modifier.size(32.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Outlined.Delete,
                                                                            contentDescription = "Elimina",
                                                                            tint = Color(0xFFE55353),
                                                                            modifier = Modifier.size(17.dp)
                                                                        )
                                                                    }

                                                                    Box(
                                                                        modifier = Modifier.size(34.dp),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Menu,
                                                                            contentDescription = "Trascina per riordinare",
                                                                            tint = if (isDragged) colors.accent else colors.textSecondary.copy(alpha = 0.7f),
                                                                            modifier = Modifier.size(19.dp)
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

                        // Leggera sfumatura superiore/inferiore quando la lista scorre
                        if (scrollState.value > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(containerBg, Color.Transparent)
                                        )
                                    )
                                    .zIndex(5f)
                            )
                        }
                        if (scrollState.value < scrollState.maxValue) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, containerBg)
                                        )
                                    )
                                    .zIndex(5f)
                            )
                        }
                    }
                }

                // Pulsanti Azione Inferiori
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceSecondary,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .clickable { checkAndDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Annulla",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val keptOriginalNames = draftTags.mapNotNull { it.originalName }.toSet()
                            val toDelete = viewModel.availableTags.filterNot { keptOriginalNames.contains(it) }
                            toDelete.forEach { viewModel.deleteTag(it) }

                            draftTags.forEach { draft ->
                                if (draft.originalName != null && draft.originalName != draft.name) {
                                    viewModel.updateTag(draft.originalName, draft.name)
                                }
                            }

                            draftTags.forEach { draft ->
                                if (draft.originalName == null) {
                                    viewModel.addTag(draft.name)
                                }
                            }

                            val finalOrder = draftTags.map { it.name }
                            viewModel.setTagsList(finalOrder)

                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Salva",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Annullare le modifiche?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tutte le modifiche non salvate alla gestione tag andranno perse.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) {
                    Text("Annulla modifiche", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Continua a modificare", color = colors.textPrimary)
                }
            }
        )
    }

    if (tagPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { tagPendingDelete = null },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Eliminare tag?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sei sicuro di voler eliminare il tag \"${tagPendingDelete?.name}\"? La rimozione diventerà effettiva al salvataggio.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        draftTags.remove(tagPendingDelete)
                        tagPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) {
                    Text("Elimina", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagPendingDelete = null }) {
                    Text("Annulla", color = colors.textPrimary)
                }
            }
        )
    }
}
