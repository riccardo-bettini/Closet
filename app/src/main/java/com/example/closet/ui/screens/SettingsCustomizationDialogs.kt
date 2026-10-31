package com.example.closet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun IslandTabsEditorDialog(
    initialTabs: List<IslandTabType>,
    useDistinctColors: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<IslandTabType>) -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val catalogScrollState = rememberScrollState()

    val workingTabs = remember {
        mutableStateListOf<IslandTabType>().apply {
            addAll(normalizeIslandTabs(initialTabs))
        }
    }

    val cellSizeDp: Dp = 52.dp
    val cellSpacingDp: Dp = 8.dp
    val cellSizePx = with(density) { cellSizeDp.toPx() }
    val cellSpacingPx = with(density) { cellSpacingDp.toPx() }
    val cellStepPx = cellSizePx + cellSpacingPx

    var draggedTab by remember { mutableStateOf<IslandTabType?>(null) }
    var dragStartCol by remember { mutableIntStateOf(0) }
    var targetHoverCol by remember { mutableIntStateOf(-1) }
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    var draggedCatalogTab by remember { mutableStateOf<IslandTabType?>(null) }
    var catalogFingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var dialogRootPos by remember { mutableStateOf(Offset.Zero) }
    var previewRowRootPos by remember { mutableStateOf(Offset.Zero) }

    val baseList = workingTabs.toList()
    val currentSlotsCount = baseList.size.coerceIn(2, MAX_ISLAND_TABS)

    val virtualTabs = remember(baseList, draggedTab, targetHoverCol) {
        if (draggedTab != null && targetHoverCol >= 0) {
            resolveSingleRowDragLayout(
                baseTabs = baseList,
                draggedTab = draggedTab!!,
                targetCol = targetHoverCol
            )
        } else {
            baseList
        }
    }

    // Slot evidenziato durante il trascinamento dal catalogo verso la riga dell'isola
    val catalogHoveredSlot: Int? = remember(
        draggedCatalogTab,
        catalogFingerRootPos,
        previewRowRootPos,
        currentSlotsCount
    ) {
        if (draggedCatalogTab == null) null
        else {
            val relX = catalogFingerRootPos.x - previewRowRootPos.x
            val relY = catalogFingerRootPos.y - previewRowRootPos.y
            val rowWidthPx = (cellSizePx * currentSlotsCount) + (cellSpacingPx * (currentSlotsCount - 1))

            if (relX in -cellSizePx * 0.5f..(rowWidthPx + cellSizePx * 0.5f) &&
                relY in -cellSizePx * 0.65f..(cellSizePx * 1.65f)
            ) {
                val maxIndex = if (workingTabs.contains(draggedCatalogTab) || currentSlotsCount >= MAX_ISLAND_TABS) {
                    currentSlotsCount - 1
                } else {
                    currentSlotsCount
                }
                (relX / cellStepPx).roundToInt().coerceIn(0, maxIndex)
            } else null
        }
    }

    fun placeCatalogTabAt(tab: IslandTabType, targetIndex: Int) {
        val list = workingTabs.toMutableList()
        val existingIdx = list.indexOf(tab)

        if (existingIdx != -1) {
            list.removeAt(existingIdx)
            val safeIdx = targetIndex.coerceIn(0, list.size)
            list.add(safeIdx, tab)
        } else {
            if (list.size < MAX_ISLAND_TABS) {
                val safeIdx = targetIndex.coerceIn(0, list.size)
                list.add(safeIdx, tab)
            } else {
                val safeIdx = targetIndex.coerceIn(0, list.size - 1)
                if (!list[safeIdx].isMandatory) {
                    list[safeIdx] = tab
                } else {
                    val replaceableIdx = list.indexOfLast { !it.isMandatory }
                    if (replaceableIdx != -1) {
                        list[replaceableIdx] = tab
                    }
                }
            }
        }

        val normalized = normalizeIslandTabs(list)
        workingTabs.clear()
        workingTabs.addAll(normalized)
    }

    fun addCatalogTabAtEnd(tab: IslandTabType) {
        if (workingTabs.contains(tab)) return
        if (workingTabs.size < MAX_ISLAND_TABS) {
            val fnIdx = workingTabs.indexOf(IslandTabType.FUNCTIONS)
            if (fnIdx != -1) {
                workingTabs.add(fnIdx, tab)
            } else {
                workingTabs.add(tab)
            }
            val normalized = normalizeIslandTabs(workingTabs)
            workingTabs.clear()
            workingTabs.addAll(normalized)
        }
    }

    val rowTotalWidthDp = (cellSizeDp * currentSlotsCount) + (cellSpacingDp * (currentSlotsCount - 1))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    dialogRootPos = coords.localToRoot(Offset.Zero)
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = colors.dialogBackground,
                border = BorderStroke(0.5.dp, colors.cardBorder),
                tonalElevation = 8.dp,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 680.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gestione delle schede",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )

                        Surface(
                            shape = CircleShape,
                            color = colors.accent.copy(alpha = 0.16f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ViewCarousel,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(18.dp)
                            )
                        }
                    }

                    // Box Anteprima Riga Singola Isola
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surfaceSecondary.copy(alpha = if (colors.isDark) 0.55f else 0.7f),
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ISOLA PRINCIPALE (RIGA SINGOLA)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accent,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${baseList.size}/$MAX_ISLAND_TABS schede",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(27.dp),
                                color = colors.islandBackground,
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f)),
                                tonalElevation = 6.dp,
                                shadowElevation = 10.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(width = rowTotalWidthDp, height = cellSizeDp)
                                        .onGloballyPositioned { coords ->
                                            previewRowRootPos = coords.localToRoot(Offset.Zero)
                                        }
                                ) {
                                    // Slot di sfondo
                                    for (c in 0 until currentSlotsCount) {
                                        val slotX = (cellSizeDp + cellSpacingDp) * c
                                        val isHoveredByCatalog = catalogHoveredSlot == c

                                        Box(
                                            modifier = Modifier
                                                .offset(x = slotX, y = 0.dp)
                                                .size(cellSizeDp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isHoveredByCatalog) colors.accent.copy(alpha = 0.28f)
                                                    else colors.surfaceSecondary.copy(alpha = 0.55f)
                                                )
                                                .border(
                                                    width = if (isHoveredByCatalog) 2.dp else 1.dp,
                                                    color = if (isHoveredByCatalog) colors.accent else colors.cardBorder,
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    // Sagoma slot di atterraggio durante drag interno
                                    val draggedDestIndex = if (draggedTab != null) {
                                        virtualTabs.indexOf(draggedTab)
                                    } else -1

                                    if (draggedDestIndex != -1) {
                                        val dropX by animateDpAsState(
                                            targetValue = (cellSizeDp + cellSpacingDp) * draggedDestIndex,
                                            animationSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            ),
                                            label = "islandTabDropX"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .offset(x = dropX, y = 0.dp)
                                                .size(cellSizeDp)
                                                .zIndex(1f)
                                                .clip(CircleShape)
                                                .background(colors.accent.copy(alpha = 0.22f))
                                                .border(2.dp, colors.accent, CircleShape)
                                        )
                                    }

                                    // Nodi persistenti delle schede nell'isola
                                    baseList.forEachIndexed { origIndex, tabItem ->
                                        key(tabItem.id) {
                                            val isDragged = (draggedTab == tabItem)
                                            val virtIndex = virtualTabs.indexOf(tabItem).coerceAtLeast(0)

                                            val targetPosX = (cellSizeDp + cellSpacingDp) * virtIndex
                                            val animPosX by animateDpAsState(
                                                targetValue = targetPosX,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioLowBouncy
                                                ),
                                                label = "islandTabPosX"
                                            )

                                            val dragLiftScale by animateFloatAsState(
                                                targetValue = if (isDragged) 1.14f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                                label = "islandTabDragScale"
                                            )

                                            val visual = getIslandTabVisual(tabItem, useDistinctColors)
                                            val currentOrigIdx by rememberUpdatedState(origIndex)
                                            val currentVirtualList by rememberUpdatedState(virtualTabs)
                                            val currentCount by rememberUpdatedState(currentSlotsCount)

                                            Box(
                                                modifier = Modifier
                                                    .offset(
                                                        x = if (isDragged) (cellSizeDp + cellSpacingDp) * dragStartCol else animPosX,
                                                        y = 0.dp
                                                    )
                                                    .size(cellSizeDp)
                                                    .zIndex(if (isDragged) 50f else 5f)
                                                    .graphicsLayer {
                                                        if (isDragged) {
                                                            translationX = dragDeltaX
                                                            translationY = dragDeltaY
                                                            shadowElevation = 24f
                                                        } else {
                                                            translationX = 0f
                                                            translationY = 0f
                                                            shadowElevation = 0f
                                                        }
                                                        scaleX = dragLiftScale
                                                        scaleY = dragLiftScale
                                                    }
                                                    .pointerInput(tabItem.id) {
                                                        detectTapGestures(
                                                            onTap = { tapOffset ->
                                                                if (!tabItem.isMandatory &&
                                                                    tapOffset.x >= size.width * 0.55f &&
                                                                    tapOffset.y <= size.height * 0.45f
                                                                ) {
                                                                    workingTabs.remove(tabItem)
                                                                }
                                                            }
                                                        )
                                                    }
                                                    .pointerInput(tabItem.id) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                draggedTab = tabItem
                                                                dragStartCol = currentOrigIdx
                                                                targetHoverCol = currentOrigIdx
                                                                dragDeltaX = 0f
                                                                dragDeltaY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragDeltaX += dragAmount.x
                                                                dragDeltaY += dragAmount.y

                                                                val currentLeftPx = (dragStartCol * cellStepPx) + dragDeltaX
                                                                val rawCol = (currentLeftPx / cellStepPx).roundToInt()

                                                                val draggedOutsideVertically =
                                                                    dragDeltaY < -cellSizePx * 1.15f || dragDeltaY > cellSizePx * 1.15f

                                                                if (!tabItem.isMandatory &&
                                                                    (rawCol < -1 || rawCol > currentCount || draggedOutsideVertically)
                                                                ) {
                                                                    targetHoverCol = -1
                                                                } else {
                                                                    targetHoverCol = rawCol.coerceIn(0, currentCount - 1)
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                val activeDragged = draggedTab
                                                                if (activeDragged != null) {
                                                                    if (targetHoverCol == -1 && !activeDragged.isMandatory) {
                                                                        workingTabs.remove(activeDragged)
                                                                    } else {
                                                                        val finalTabs = normalizeIslandTabs(currentVirtualList)
                                                                        workingTabs.clear()
                                                                        workingTabs.addAll(finalTabs)
                                                                    }
                                                                }
                                                                draggedTab = null
                                                                targetHoverCol = -1
                                                                dragDeltaX = 0f
                                                                dragDeltaY = 0f
                                                            },
                                                            onDragCancel = {
                                                                draggedTab = null
                                                                targetHoverCol = -1
                                                                dragDeltaX = 0f
                                                                dragDeltaY = 0f
                                                            }
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            visual.distinctColor.copy(
                                                                alpha = if (isDragged) 0.90f else if (colors.isDark) 0.24f else 0.16f
                                                            )
                                                        )
                                                        .border(
                                                            width = if (isDragged) 2.dp else 1.dp,
                                                            color = if (isDragged) Color.White else visual.distinctColor.copy(alpha = 0.5f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = visual.icon,
                                                        contentDescription = tabItem.label,
                                                        tint = if (isDragged) Color.White else visual.distinctColor,
                                                        modifier = Modifier.size(21.dp)
                                                    )
                                                }

                                                // Badge: rimozione (-) per le schede opzionali, lucchetto per Home/Funzioni
                                                if (!isDragged) {
                                                    if (!tabItem.isMandatory) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .offset(x = 2.dp, y = (-2).dp)
                                                                .size(18.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFFE55353))
                                                                .border(1.5.dp, colors.islandBackground, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "Rimuovi scheda",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .offset(x = 1.dp, y = 1.dp)
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(colors.surfaceSecondary)
                                                                .border(1.dp, colors.cardBorder, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Lock,
                                                                contentDescription = "Scheda obbligatoria",
                                                                tint = colors.accent,
                                                                modifier = Modifier.size(9.dp)
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

                    // Catalogo di tutte le schede/funzioni disponibili
                    val allTabOptions = remember { IslandTabType.entries.toList() }
                    val chunkedTabs = remember(allTabOptions) { allTabOptions.chunked(4) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(
                                state = catalogScrollState,
                                enabled = draggedCatalogTab == null
                            ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunkedTabs.forEach { rowTabs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in 0..3) {
                                    val tabOption = rowTabs.getOrNull(i)
                                    if (tabOption != null) {
                                        val isAlreadyInIsland = workingTabs.contains(tabOption)
                                        val visual = getIslandTabVisual(tabOption, useDistinctColors)
                                        var catalogItemRootPos by remember { mutableStateOf(Offset.Zero) }
                                        val currentHoveredSlot by rememberUpdatedState(catalogHoveredSlot)

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .onGloballyPositioned { coords ->
                                                    catalogItemRootPos = coords.localToRoot(Offset.Zero)
                                                }
                                                .clip(RoundedCornerShape(14.dp))
                                                .pointerInput(tabOption) {
                                                    detectTapGestures(
                                                        onTap = {
                                                            if (!workingTabs.contains(tabOption)) {
                                                                addCatalogTabAtEnd(tabOption)
                                                            }
                                                        }
                                                    )
                                                }
                                                .pointerInput(tabOption) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { startOffset ->
                                                            draggedCatalogTab = tabOption
                                                            catalogFingerRootPos = catalogItemRootPos + startOffset
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            catalogFingerRootPos += dragAmount
                                                        },
                                                        onDragEnd = {
                                                            val targetIdx = currentHoveredSlot
                                                            val draggedOpt = draggedCatalogTab
                                                            if (draggedOpt != null && targetIdx != null) {
                                                                placeCatalogTabAt(draggedOpt, targetIdx)
                                                            } else if (draggedOpt != null && !workingTabs.contains(draggedOpt)) {
                                                                addCatalogTabAtEnd(draggedOpt)
                                                            }
                                                            draggedCatalogTab = null
                                                        },
                                                        onDragCancel = {
                                                            draggedCatalogTab = null
                                                        }
                                                    )
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(46.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(
                                                            visual.distinctColor.copy(
                                                                alpha = if (isAlreadyInIsland) 0.08f else if (colors.isDark) 0.22f else 0.14f
                                                            )
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isAlreadyInIsland) colors.cardBorder else visual.distinctColor.copy(alpha = 0.45f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = visual.icon,
                                                        contentDescription = tabOption.label,
                                                        tint = if (isAlreadyInIsland) colors.textMuted else visual.distinctColor,
                                                        modifier = Modifier.size(21.dp)
                                                    )
                                                }

                                                if (isAlreadyInIsland) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .offset(x = 1.dp, y = 1.dp)
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(colors.accent)
                                                            .border(1.5.dp, colors.dialogBackground, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (tabOption.isMandatory) Icons.Default.Lock else Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(9.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = tabOption.label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isAlreadyInIsland) colors.textMuted else colors.textPrimary,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder)

                    // Bottoni Annulla e Salva
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Annulla",
                                    color = Color(0xFFE55353),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { onSave(normalizeIslandTabs(workingTabs.toList())) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
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

            // Icona flottante trascinata dal catalogo
            if (draggedCatalogTab != null) {
                val visual = getIslandTabVisual(draggedCatalogTab!!, useDistinctColors)
                val localOffset = catalogFingerRootPos - dialogRootPos
                val halfSizePx = with(density) { 26.dp.toPx() }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer {
                            translationX = localOffset.x - halfSizePx
                            translationY = localOffset.y - halfSizePx
                            scaleX = 1.15f
                            scaleY = 1.15f
                            shadowElevation = 24f
                        }
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(visual.distinctColor)
                        .border(2.dp, Color.White, CircleShape)
                        .zIndex(100f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// DIALOG MODIFICA FUNZIONI PREFERITE (TASTO STELLA ★)
// =========================================================================

@Composable
fun FavoriteShortcutsEditorDialog(
    initialShortcuts: List<PlacedFavoriteShortcut>,
    useDistinctColors: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<PlacedFavoriteShortcut>) -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val catalogScrollState = rememberScrollState()

    val workingShortcuts = remember {
        mutableStateListOf<PlacedFavoriteShortcut>().apply {
            addAll(compactFavoriteRows(initialShortcuts))
        }
    }

    val cellSizeDp: Dp = 52.dp
    val cellSpacingDp: Dp = 10.dp
    val cellSizePx = with(density) { cellSizeDp.toPx() }
    val cellSpacingPx = with(density) { cellSpacingDp.toPx() }
    val cellStepPx = cellSizePx + cellSpacingPx

    val maxOccupiedRow = workingShortcuts.maxOfOrNull { it.row } ?: -1
    val previewRowsCount = maxOf(2, maxOccupiedRow + 1).coerceAtMost(4)

    var draggedPlacedId by remember { mutableStateOf<String?>(null) }
    var dragStartRow by remember { mutableIntStateOf(0) }
    var dragStartCol by remember { mutableIntStateOf(0) }
    var targetHoverRow by remember { mutableIntStateOf(-1) }
    var targetHoverCol by remember { mutableIntStateOf(-1) }
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    var draggedCatalogAction by remember { mutableStateOf<FavoriteActionType?>(null) }
    var catalogFingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var dialogRootPos by remember { mutableStateOf(Offset.Zero) }
    var previewGridRootPos by remember { mutableStateOf(Offset.Zero) }

    val baseList = workingShortcuts.toList()

    val virtualPlacements = remember(baseList, draggedPlacedId, targetHoverRow, targetHoverCol) {
        if (draggedPlacedId != null && targetHoverRow >= 0 && targetHoverCol >= 0) {
            resolveFavoriteGridDragLayout(
                baseItems = baseList,
                draggedId = draggedPlacedId!!,
                targetRow = targetHoverRow,
                targetCol = targetHoverCol
            )
        } else {
            baseList
        }
    }

    val catalogHoveredCell: Pair<Int, Int>? = remember(
        draggedCatalogAction,
        catalogFingerRootPos,
        previewGridRootPos,
        previewRowsCount
    ) {
        if (draggedCatalogAction == null) null
        else {
            val relX = catalogFingerRootPos.x - previewGridRootPos.x
            val relY = catalogFingerRootPos.y - previewGridRootPos.y
            val gridWidthPx = (cellSizePx * 4f) + (cellSpacingPx * 3f)
            val gridHeightPx = (cellSizePx * previewRowsCount) + (cellSpacingPx * (previewRowsCount - 1))

            if (relX in -cellSizePx * 0.45f..(gridWidthPx + cellSizePx * 0.45f) &&
                relY in -cellSizePx * 0.45f..(gridHeightPx + cellSizePx * 0.45f)
            ) {
                val c = (relX / cellStepPx).toInt().coerceIn(0, 3)
                val r = (relY / cellStepPx).toInt().coerceIn(0, previewRowsCount - 1)
                r to c
            } else null
        }
    }

    fun placeCatalogShortcutAt(action: FavoriteActionType, targetR: Int, targetC: Int) {
        val currentList = workingShortcuts.toMutableList()
        val occupant = currentList.find { it.row == targetR && it.col == targetC }
        val existingSameActionIdx = currentList.indexOfFirst { it.action == action }

        if (existingSameActionIdx != -1) {
            val oldRow = currentList[existingSameActionIdx].row
            val oldCol = currentList[existingSameActionIdx].col
            currentList[existingSameActionIdx] = currentList[existingSameActionIdx].copy(row = targetR, col = targetC)
            if (occupant != null && occupant.action != action) {
                val occIdx = currentList.indexOfFirst { it.id == occupant.id }
                if (occIdx != -1) {
                    currentList[occIdx] = currentList[occIdx].copy(row = oldRow, col = oldCol)
                }
            }
        } else {
            if (occupant != null) {
                currentList.removeAll { it.id == occupant.id }
            }
            currentList.add(
                PlacedFavoriteShortcut(
                    action = action,
                    row = targetR,
                    col = targetC
                )
            )
        }

        val compacted = compactFavoriteRows(currentList)
        workingShortcuts.clear()
        workingShortcuts.addAll(compacted)
    }

    fun addShortcutToFirstFreeSlot(action: FavoriteActionType) {
        if (workingShortcuts.any { it.action == action }) return
        val occupiedRows = workingShortcuts.map { it.row }.distinct().sorted()
        for (r in occupiedRows) {
            for (c in 0..3) {
                if (workingShortcuts.none { it.row == r && it.col == c }) {
                    workingShortcuts.add(
                        PlacedFavoriteShortcut(
                            action = action,
                            row = r,
                            col = c
                        )
                    )
                    val compacted = compactFavoriteRows(workingShortcuts)
                    workingShortcuts.clear()
                    workingShortcuts.addAll(compacted)
                    return
                }
            }
        }
        for (r in 0..3) {
            for (c in 0..3) {
                if (workingShortcuts.none { it.row == r && it.col == c }) {
                    workingShortcuts.add(
                        PlacedFavoriteShortcut(
                            action = action,
                            row = r,
                            col = c
                        )
                    )
                    val compacted = compactFavoriteRows(workingShortcuts)
                    workingShortcuts.clear()
                    workingShortcuts.addAll(compacted)
                    return
                }
            }
        }
    }

    val effectiveDimensionLabel = remember(baseList) {
        if (baseList.isEmpty()) "Vuoto (0x0)"
        else {
            val rSpan = (baseList.maxOf { it.row } - baseList.minOf { it.row }) + 1
            val cSpan = (baseList.maxOf { it.col } - baseList.minOf { it.col }) + 1
            "Isola ${rSpan}x${cSpan} • ${baseList.size} pulsanti"
        }
    }

    val gridTotalWidthDp = (cellSizeDp * 4) + (cellSpacingDp * 3)
    val gridTotalHeightDp = (cellSizeDp * previewRowsCount) + (cellSpacingDp * (previewRowsCount - 1))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    dialogRootPos = coords.localToRoot(Offset.Zero)
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = colors.dialogBackground,
                border = BorderStroke(0.5.dp, colors.cardBorder),
                tonalElevation = 8.dp,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 680.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Funzioni Preferite",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEAB308).copy(alpha = 0.18f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surfaceSecondary.copy(alpha = if (colors.isDark) 0.55f else 0.7f),
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ANTEPRIMA ISOLA POPUP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accent,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = effectiveDimensionLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(27.dp),
                                color = colors.islandBackground,
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f)),
                                tonalElevation = 6.dp,
                                shadowElevation = 10.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .size(width = gridTotalWidthDp, height = gridTotalHeightDp)
                                        .onGloballyPositioned { coords ->
                                            previewGridRootPos = coords.localToRoot(Offset.Zero)
                                        }
                                ) {
                                    for (r in 0 until previewRowsCount) {
                                        for (c in 0..3) {
                                            val slotX = (cellSizeDp + cellSpacingDp) * c
                                            val slotY = (cellSizeDp + cellSpacingDp) * r
                                            val isHoveredByCatalog = catalogHoveredCell == (r to c)

                                            Box(
                                                modifier = Modifier
                                                    .offset(x = slotX, y = slotY)
                                                    .size(cellSizeDp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isHoveredByCatalog) colors.accent.copy(alpha = 0.28f)
                                                        else colors.surfaceSecondary.copy(alpha = 0.55f)
                                                    )
                                                    .border(
                                                        width = if (isHoveredByCatalog) 2.dp else 1.dp,
                                                        color = if (isHoveredByCatalog) colors.accent else colors.cardBorder,
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }

                                    val draggedPreview = if (draggedPlacedId != null) {
                                        virtualPlacements.find { it.id == draggedPlacedId }
                                    } else null

                                    if (draggedPreview != null) {
                                        val dropX by animateDpAsState(
                                            targetValue = (cellSizeDp + cellSpacingDp) * draggedPreview.col,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                            label = "favDropX"
                                        )
                                        val dropY by animateDpAsState(
                                            targetValue = (cellSizeDp + cellSpacingDp) * draggedPreview.row,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                                            label = "favDropY"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .offset(x = dropX, y = dropY)
                                                .size(cellSizeDp)
                                                .zIndex(1f)
                                                .clip(CircleShape)
                                                .background(colors.accent.copy(alpha = 0.22f))
                                                .border(2.dp, colors.accent, CircleShape)
                                        )
                                    }

                                    baseList.forEach { item ->
                                        key(item.id) {
                                            val isDragged = (draggedPlacedId == item.id)
                                            val pVirt = virtualPlacements.find { it.id == item.id } ?: item

                                            val targetPosX = (cellSizeDp + cellSpacingDp) * pVirt.col
                                            val targetPosY = (cellSizeDp + cellSpacingDp) * pVirt.row

                                            val animPosX by animateDpAsState(
                                                targetValue = targetPosX,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioLowBouncy
                                                ),
                                                label = "favPosX"
                                            )
                                            val animPosY by animateDpAsState(
                                                targetValue = targetPosY,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioLowBouncy
                                                ),
                                                label = "favPosY"
                                            )

                                            val dragLiftScale by animateFloatAsState(
                                                targetValue = if (isDragged) 1.14f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                                label = "favDragScale"
                                            )

                                            val visual = getFavoriteActionVisual(item.action, useDistinctColors)
                                            val currentOrigItem by rememberUpdatedState(item)
                                            val currentVirtualList by rememberUpdatedState(virtualPlacements)
                                            val currentRowsCount by rememberUpdatedState(previewRowsCount)

                                            Box(
                                                modifier = Modifier
                                                    .offset(
                                                        x = if (isDragged) (cellSizeDp + cellSpacingDp) * dragStartCol else animPosX,
                                                        y = if (isDragged) (cellSizeDp + cellSpacingDp) * dragStartRow else animPosY
                                                    )
                                                    .size(cellSizeDp)
                                                    .zIndex(if (isDragged) 50f else 5f)
                                                    .graphicsLayer {
                                                        if (isDragged) {
                                                            translationX = dragDeltaX
                                                            translationY = dragDeltaY
                                                            shadowElevation = 24f
                                                        } else {
                                                            translationX = 0f
                                                            translationY = 0f
                                                            shadowElevation = 0f
                                                        }
                                                        scaleX = dragLiftScale
                                                        scaleY = dragLiftScale
                                                    }
                                                    .pointerInput(item.id) {
                                                        detectTapGestures(
                                                            onTap = { tapOffset ->
                                                                if (tapOffset.x >= size.width * 0.55f && tapOffset.y <= size.height * 0.45f) {
                                                                    workingShortcuts.removeAll { it.id == item.id }
                                                                    val comp = compactFavoriteRows(workingShortcuts)
                                                                    workingShortcuts.clear()
                                                                    workingShortcuts.addAll(comp)
                                                                }
                                                            }
                                                        )
                                                    }
                                                    .pointerInput(item.id) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                val orig = currentOrigItem
                                                                draggedPlacedId = orig.id
                                                                dragStartRow = orig.row
                                                                dragStartCol = orig.col
                                                                targetHoverRow = orig.row
                                                                targetHoverCol = orig.col
                                                                dragDeltaX = 0f
                                                                dragDeltaY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragDeltaX += dragAmount.x
                                                                dragDeltaY += dragAmount.y

                                                                val currentLeftPx = (dragStartCol * cellStepPx) + dragDeltaX
                                                                val currentTopPx = (dragStartRow * cellStepPx) + dragDeltaY

                                                                val rawCol = (currentLeftPx / cellStepPx).roundToInt()
                                                                val rawRow = (currentTopPx / cellStepPx).roundToInt()

                                                                if (rawCol < -1 || rawCol > 4 || rawRow < -1 || rawRow > currentRowsCount) {
                                                                    targetHoverRow = -1
                                                                    targetHoverCol = -1
                                                                } else {
                                                                    targetHoverCol = rawCol.coerceIn(0, 3)
                                                                    targetHoverRow = rawRow.coerceIn(0, currentRowsCount - 1)
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                val draggedId = draggedPlacedId
                                                                if (draggedId != null) {
                                                                    if (targetHoverRow == -1 || targetHoverCol == -1) {
                                                                        workingShortcuts.removeAll { it.id == draggedId }
                                                                        val comp = compactFavoriteRows(workingShortcuts)
                                                                        workingShortcuts.clear()
                                                                        workingShortcuts.addAll(comp)
                                                                    } else {
                                                                        val finalItems = compactFavoriteRows(currentVirtualList)
                                                                        workingShortcuts.clear()
                                                                        workingShortcuts.addAll(finalItems)
                                                                    }
                                                                }
                                                                draggedPlacedId = null
                                                                targetHoverRow = -1
                                                                targetHoverCol = -1
                                                                dragDeltaX = 0f
                                                                dragDeltaY = 0f
                                                            },
                                                            onDragCancel = {
                                                                draggedPlacedId = null
                                                                targetHoverRow = -1
                                                                targetHoverCol = -1
                                                                dragDeltaX = 0f
                                                                dragDeltaY = 0f
                                                            }
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            visual.distinctColor.copy(
                                                                alpha = if (isDragged) 0.90f else if (colors.isDark) 0.24f else 0.16f
                                                            )
                                                        )
                                                        .border(
                                                            width = if (isDragged) 2.dp else 1.dp,
                                                            color = if (isDragged) Color.White else visual.distinctColor.copy(alpha = 0.5f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = visual.icon,
                                                        contentDescription = item.action.label,
                                                        tint = if (isDragged) Color.White else visual.distinctColor,
                                                        modifier = Modifier.size(21.dp)
                                                    )
                                                }

                                                if (!isDragged) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 2.dp, y = (-2).dp)
                                                            .size(18.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFE55353))
                                                            .border(1.5.dp, colors.islandBackground, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Remove,
                                                            contentDescription = "Rimuovi",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(11.dp)
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

                    val allActions = remember { FavoriteActionType.entries.toList() }
                    val chunkedActions = remember(allActions) { allActions.chunked(4) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(
                                state = catalogScrollState,
                                enabled = draggedCatalogAction == null
                            ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunkedActions.forEach { rowActions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in 0..3) {
                                    val action = rowActions.getOrNull(i)
                                    if (action != null) {
                                        val isAlreadyAdded = workingShortcuts.any { it.action == action }
                                        val visual = getFavoriteActionVisual(action, useDistinctColors)
                                        var catalogItemRootPos by remember { mutableStateOf(Offset.Zero) }
                                        val currentCatalogHovered by rememberUpdatedState(catalogHoveredCell)

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .onGloballyPositioned { coords ->
                                                    catalogItemRootPos = coords.localToRoot(Offset.Zero)
                                                }
                                                .clip(RoundedCornerShape(14.dp))
                                                .pointerInput(action) {
                                                    detectTapGestures(
                                                        onTap = {
                                                            if (workingShortcuts.none { it.action == action }) {
                                                                addShortcutToFirstFreeSlot(action)
                                                            }
                                                        }
                                                    )
                                                }
                                                .pointerInput(action) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { startOffset ->
                                                            draggedCatalogAction = action
                                                            catalogFingerRootPos = catalogItemRootPos + startOffset
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            catalogFingerRootPos += dragAmount
                                                        },
                                                        onDragEnd = {
                                                            val target = currentCatalogHovered
                                                            val act = draggedCatalogAction
                                                            if (act != null && target != null) {
                                                                placeCatalogShortcutAt(act, target.first, target.second)
                                                            } else if (act != null && workingShortcuts.none { it.action == act }) {
                                                                addShortcutToFirstFreeSlot(act)
                                                            }
                                                            draggedCatalogAction = null
                                                        },
                                                        onDragCancel = {
                                                            draggedCatalogAction = null
                                                        }
                                                    )
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(46.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(
                                                            visual.distinctColor.copy(
                                                                alpha = if (isAlreadyAdded) 0.08f else if (colors.isDark) 0.22f else 0.14f
                                                            )
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isAlreadyAdded) colors.cardBorder else visual.distinctColor.copy(alpha = 0.45f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = visual.icon,
                                                        contentDescription = action.label,
                                                        tint = if (isAlreadyAdded) colors.textMuted else visual.distinctColor,
                                                        modifier = Modifier.size(21.dp)
                                                    )
                                                }

                                                if (isAlreadyAdded) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .offset(x = 1.dp, y = 1.dp)
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(colors.accent)
                                                            .border(1.5.dp, colors.dialogBackground, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(9.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = action.label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isAlreadyAdded) colors.textMuted else colors.textPrimary,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Annulla",
                                    color = Color(0xFFE55353),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { onSave(workingShortcuts.toList()) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
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

            if (draggedCatalogAction != null) {
                val visual = getFavoriteActionVisual(draggedCatalogAction!!, useDistinctColors)
                val localOffset = catalogFingerRootPos - dialogRootPos
                val halfSizePx = with(density) { 26.dp.toPx() }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer {
                            translationX = localOffset.x - halfSizePx
                            translationY = localOffset.y - halfSizePx
                            scaleX = 1.15f
                            scaleY = 1.15f
                            shadowElevation = 24f
                        }
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(visual.distinctColor)
                        .border(2.dp, Color.White, CircleShape)
                        .zIndex(100f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}