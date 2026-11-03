@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.closet.ui.viewmodel.ClosetFolder
import com.example.closet.ui.viewmodel.ClothingSortOrder
import com.example.closet.ui.viewmodel.ClosetViewModel
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

enum class ViewMode {
    GRID,
    LIST
}

@Immutable
data class ClothingItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val tags: List<String> = emptyList(),
    val closetName: String,
    val folderName: String? = null,
    val isCountable: Boolean = false,
    val quantity: Int = 1,
    val color: Color = Color.Transparent,
    val imageUri: Uri? = null,
    val emoji: String? = null,
    val isLaundryInTransit: Boolean = false
)

@Composable
fun EmptyClosetState(isFolder: Boolean = false, isSearch: Boolean = false) {
    val colors = LocalOneUIColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else if (isFolder) Icons.Outlined.Folder else Icons.Default.Checkroom,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    isSearch -> "Nessun capo o cartella trovata"
                    isFolder -> "Questa cartella ÃƒÂ¨ vuota"
                    else -> "Nessun capo presente in questo armadio"
                },
                color = colors.textMuted,
                fontSize = 14.sp
            )
        }
    }
}
@Composable
fun WardrobeHeaderControls(
    isEditMode: Boolean,
    selectedCloset: String,
    availableClosets: List<String>,
    showClosetMenu: Boolean,
    onShowClosetMenuChange: (Boolean) -> Unit,
    onSelectCloset: (String) -> Unit,
    onManageClosetsClick: () -> Unit,
    currentSortOrder: ClothingSortOrder,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit,
    onSelectSortOrder: (ClothingSortOrder) -> Unit,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit
) {
    val colors = LocalOneUIColors.current

    val borderColor by animateColorAsState(
        targetValue = if (isEditMode) colors.accent else colors.cardBorder,
        animationSpec = tween(220),
        label = "headerBarBorderColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { })
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            Surface(
                shape = CircleShape,
                color = colors.cardBackground,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .height(38.dp)
                    .clickable {
                        if (isEditMode) {
                            onShowSortMenuChange(true)
                        } else {
                            onShowClosetMenuChange(true)
                        }
                    }
            ) {
                AnimatedContent(
                    targetState = isEditMode,
                    contentAlignment = Alignment.CenterStart,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180, delayMillis = 30)) + scaleIn(initialScale = 0.92f))
                            .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.92f))
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
                    label = "wardrobeHeaderBarMorph"
                ) { inEditMode ->
                    if (inEditMode) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Ordina: ${currentSortOrder.label}",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = selectedCloset,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { onShowSortMenuChange(false) },
                modifier = Modifier.background(colors.dialogBackground),
                shape = RoundedCornerShape(18.dp)
            ) {
                ClothingSortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = order.label,
                                color = if (order == currentSortOrder) colors.accent else colors.textPrimary,
                                fontWeight = if (order == currentSortOrder) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            if (order == currentSortOrder) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        onClick = {
                            onSelectSortOrder(order)
                            onShowSortMenuChange(false)
                        }
                    )
                }
            }

            DropdownMenu(
                expanded = showClosetMenu,
                onDismissRequest = { onShowClosetMenuChange(false) },
                modifier = Modifier.background(colors.dialogBackground),
                shape = RoundedCornerShape(18.dp)
            ) {
                availableClosets.forEach { closet ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                closet,
                                color = if (closet == selectedCloset) colors.accent else colors.textPrimary,
                                fontWeight = if (closet == selectedCloset) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            if (closet == selectedCloset) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        onClick = {
                            onSelectCloset(closet)
                            onShowClosetMenuChange(false)
                        }
                    )
                }
                HorizontalDivider(
                    color = colors.textSecondary.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Gestione armadi",
                            color = colors.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        onShowClosetMenuChange(false)
                        onManageClosetsClick()
                    }
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = colors.cardBackground,
            border = BorderStroke(1.dp, colors.cardBorder),
            modifier = Modifier.height(38.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (viewMode == ViewMode.GRID) (if (colors.isDark) Color(0xFF383C44) else Color(0xFFE4E7EE)) else Color.Transparent)
                        .clickable { onViewModeChange(ViewMode.GRID) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = "Griglia",
                        tint = if (viewMode == ViewMode.GRID) colors.textPrimary else colors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (viewMode == ViewMode.LIST) (if (colors.isDark) Color(0xFF383C44) else Color(0xFFE4E7EE)) else Color.Transparent)
                        .clickable { onViewModeChange(ViewMode.LIST) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = "Elenco",
                        tint = if (viewMode == ViewMode.LIST) colors.textPrimary else colors.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WardrobeScreen(
    viewModel: ClosetViewModel,
    showAddItemSheet: Boolean,
    onDismissAddItemSheet: () -> Unit,
    showManageClosetsDialog: Boolean,
    onDismissManageClosets: () -> Unit,
    onOpenManageClosets: () -> Unit = {},
    showManageTagsDialog: Boolean,
    onDismissManageTags: () -> Unit,
    isItemEditMode: Boolean,
    onEnterItemEditMode: () -> Unit = {},
    onExitItemEditMode: () -> Unit,
    selectedItemIds: MutableList<String>,
    selectedItemQuantities: SnapshotStateMap<String, Int> = remember { mutableStateMapOf() },
    triggerCreateFolder: Boolean,
    onFolderDialogConsumed: () -> Unit,
    triggerMoveItems: Boolean,
    onMoveDialogConsumed: () -> Unit,
    onCameraActiveChange: (Boolean) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    targetWardrobeCloset: String? = null,
    onTargetWardrobeClosetConsumed: () -> Unit = {},
    searchQuery: String = "",
    filterTags: Set<String> = emptySet(),
    scrollToTopTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val availableClosets = viewModel.availableClosets
    val clothingItems = viewModel.clothingItems

    val prefs = remember { context.getSharedPreferences("closet_app_prefs", Context.MODE_PRIVATE) }
    var viewMode by remember {
        mutableStateOf(
            try {
                val saved = prefs.getString("wardrobe_view_mode", ViewMode.GRID.name)
                ViewMode.valueOf(saved ?: ViewMode.GRID.name)
            } catch (_: Exception) {
                ViewMode.GRID
            }
        )
    }

    var selectedCloset by remember {
        mutableStateOf(
            run {
                val savedCloset = prefs.getString("wardrobe_selected_closet", null)
                if (savedCloset != null && availableClosets.contains(savedCloset)) {
                    savedCloset
                } else {
                    availableClosets.firstOrNull() ?: "Armadio Principale"
                }
            }
        )
    }
    var currentFolder by remember { mutableStateOf<String?>(null) }
    var showClosetMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var internalShowManageClosets by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ClothingItem?>(null) }
    var itemDetailId by remember { mutableStateOf<String?>(null) }
    var folderToEdit by remember { mutableStateOf<ClosetFolder?>(null) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            if (currentFolder != null) {
                currentFolder = null
            }
            if (viewMode == ViewMode.GRID) {
                gridState.animateScrollToItem(0)
            } else {
                listState.animateScrollToItem(0)
            }
        }
    }

    BackHandler(enabled = currentFolder != null) {
        currentFolder = null
    }

    LaunchedEffect(targetWardrobeCloset) {
        if (targetWardrobeCloset != null && availableClosets.contains(targetWardrobeCloset)) {
            selectedCloset = targetWardrobeCloset
            prefs.edit().putString("wardrobe_selected_closet", targetWardrobeCloset).apply()
            currentFolder = null
            onTargetWardrobeClosetConsumed()
        }
    }

    if (!availableClosets.contains(selectedCloset) && availableClosets.isNotEmpty()) {
        val fallback = availableClosets[0]
        selectedCloset = fallback
        prefs.edit().putString("wardrobe_selected_closet", fallback).apply()
        currentFolder = null
    }

    val availableFolders = remember(viewModel.closetFolders.toList(), selectedCloset) {
        viewModel.getFoldersForCloset(selectedCloset)
    }

    val filteredFolders = remember(availableFolders, clothingItems.toList(), selectedCloset, searchQuery, filterTags, isSearchActive) {
        if (!isSearchActive) {
            availableFolders
        } else {
            availableFolders.filter { folder ->
                val folderItems = clothingItems.filter { it.closetName == selectedCloset && it.folderName == folder.folderName }
                val nameMatches = if (searchQuery.isBlank()) true else folder.folderName.contains(searchQuery.trim(), ignoreCase = true)
                val itemsMatch = folderItems.any { item ->
                    val qMatch = if (searchQuery.isBlank()) true else {
                        item.name.contains(searchQuery.trim(), ignoreCase = true) ||
                                item.tags.any { it.contains(searchQuery.trim(), ignoreCase = true) }
                    }
                    val tMatch = if (filterTags.isEmpty()) true else filterTags.all { fTag ->
                        item.tags.any { it.equals(fTag, ignoreCase = true) }
                    }
                    qMatch && tMatch
                }

                if (filterTags.isNotEmpty()) {
                    val tagItemsMatch = folderItems.any { item ->
                        filterTags.all { fTag -> item.tags.any { it.equals(fTag, ignoreCase = true) } }
                    }
                    if (searchQuery.isNotBlank()) {
                        (nameMatches || itemsMatch) && tagItemsMatch
                    } else {
                        tagItemsMatch
                    }
                } else if (searchQuery.isNotBlank()) {
                    nameMatches || itemsMatch
                } else {
                    true
                }
            }
        }
    }

    val activeTransfers = viewModel.activeLaundryTransfers
    val currentMillis = System.currentTimeMillis()

    val folderItemCounts = remember(clothingItems.toList(), selectedCloset) {
        val counts = mutableMapOf<String, Int>()
        for (item in clothingItems) {
            if (item.closetName == selectedCloset && item.folderName != null) {
                counts[item.folderName] = (counts[item.folderName] ?: 0) + 1
            }
        }
        counts
    }

    val folderFirstImages = remember(clothingItems.toList(), selectedCloset) {
        val map = mutableMapOf<String, Uri?>()
        for (item in clothingItems) {
            if (item.closetName == selectedCloset && item.folderName != null && item.imageUri != null) {
                map.putIfAbsent(item.folderName, item.imageUri)
            }
        }
        map
    }

    val displayItems = remember(clothingItems.toList(), activeTransfers.toList(), selectedCloset) {
        clothingItems.map { rawItem ->
            val isDimmedTransit = activeTransfers.any { tr ->
                tr.toCloset == selectedCloset &&
                        tr.destinationItemIds.contains(rawItem.id) &&
                        tr.isVisibleDimmed &&
                        tr.endTimeMillis > currentMillis
            }
            rawItem.copy(isLaundryInTransit = isDimmedTransit)
        }
    }

    val filteredItems = remember(displayItems, selectedCloset, currentFolder, searchQuery, filterTags, isSearchActive) {
        displayItems.filter { item ->
            val matchCloset = item.closetName == selectedCloset
            val matchFolder = if (isSearchActive) {
                if (currentFolder != null) item.folderName == currentFolder else true
            } else {
                item.folderName == currentFolder
            }

            val matchQuery = if (searchQuery.isBlank()) true else {
                item.name.contains(searchQuery.trim(), ignoreCase = true) ||
                        (item.folderName?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
                        item.tags.any { it.contains(searchQuery.trim(), ignoreCase = true) }
            }
            val matchTag = if (filterTags.isEmpty()) true else filterTags.all { fTag ->
                item.tags.any { it.equals(fTag, ignoreCase = true) }
            }
            matchCloset && matchFolder && matchQuery && matchTag
        }
    }

    val foldersToShow = if (currentFolder != null) {
        emptyList()
    } else if (isSearchActive) {
        filteredFolders
    } else {
        availableFolders
    }

    val workingItems = remember {
        mutableStateListOf<ClothingItem>().apply { addAll(filteredItems) }
    }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragInitialIndex by remember { mutableIntStateOf(-1) }
    var dragAccumulatedX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }
    var dragFingerViewportY by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    var cellTotalWidthPx by remember { mutableFloatStateOf(0f) }
    var cellTotalHeightPx by remember { mutableFloatStateOf(0f) }
    var totalSlotHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(filteredItems, draggedItemId) {
        if (draggedItemId == null) {
            val currentIds = workingItems.map { it.id }
            val newIds = filteredItems.map { it.id }
            if (currentIds != newIds) {
                workingItems.clear()
                workingItems.addAll(filteredItems)
            } else {
                for (i in filteredItems.indices) {
                    if (workingItems[i] != filteredItems[i]) {
                        workingItems[i] = filteredItems[i]
                    }
                }
            }
        }
    }

    LaunchedEffect(isItemEditMode) {
        if (!isItemEditMode) {
            draggedItemId = null
            dragInitialIndex = -1
            dragAccumulatedX = 0f
            dragAccumulatedY = 0f
            dragFingerViewportY = 0f
        }
    }

    val displayItemsList = if (workingItems.isNotEmpty()) workingItems else filteredItems

    // Auto-scroll continuo e fluido durante il trascinamento dei capi (sia in griglia che in lista)
    LaunchedEffect(draggedItemId, viewMode) {
        if (draggedItemId == null) return@LaunchedEffect
        val topScrollZonePx = with(density) { 110.dp.toPx() }
        val bottomScrollZonePx = with(density) { 150.dp.toPx() }
        val maxScrollStepPx = with(density) { 12.dp.toPx() }

        while (draggedItemId != null) {
            if (viewportHeightPx > 0f) {
                val fingerY = dragFingerViewportY
                val topZone = topScrollZonePx
                val bottomZone = viewportHeightPx - bottomScrollZonePx

                val canScrollUp = if (viewMode == ViewMode.GRID) gridState.canScrollBackward else listState.canScrollBackward
                val canScrollDown = if (viewMode == ViewMode.GRID) gridState.canScrollForward else listState.canScrollForward

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
                    val consumed = if (viewMode == ViewMode.GRID) {
                        gridState.scrollBy(scrollDelta)
                    } else {
                        listState.scrollBy(scrollDelta)
                    }

                    if (consumed != 0f) {
                        dragAccumulatedY += consumed

                        val curId = draggedItemId
                        if (curId != null && dragInitialIndex != -1) {
                            val curIdx = workingItems.indexOfFirst { it.id == curId }
                            if (curIdx != -1) {
                                if (viewMode == ViewMode.GRID && cellTotalHeightPx > 0f && cellTotalWidthPx > 0f) {
                                    val dragInitialRow = dragInitialIndex / 2
                                    val dragInitialCol = dragInitialIndex % 2
                                    val maxRow = (workingItems.size - 1) / 2

                                    val minAllowedY = (0 - dragInitialRow) * cellTotalHeightPx - cellTotalHeightPx * 0.35f
                                    val maxAllowedY = (maxRow - dragInitialRow) * cellTotalHeightPx + cellTotalHeightPx * 0.35f
                                    val effectiveY = dragAccumulatedY.coerceIn(minAllowedY, maxAllowedY)

                                    val minAllowedX = (0 - dragInitialCol) * cellTotalWidthPx - cellTotalWidthPx * 0.35f
                                    val maxAllowedX = (1 - dragInitialCol) * cellTotalWidthPx + cellTotalWidthPx * 0.35f
                                    val effectiveX = dragAccumulatedX.coerceIn(minAllowedX, maxAllowedX)

                                    val targetRow = (dragInitialRow + (effectiveY / cellTotalHeightPx).roundToInt()).coerceIn(0, maxRow)
                                    val targetCol = (dragInitialCol + (effectiveX / cellTotalWidthPx).roundToInt()).coerceIn(0, 1)
                                    val targetIdx = (targetRow * 2 + targetCol).coerceIn(0, workingItems.size - 1)
                                    if (targetIdx != curIdx) {
                                        val moved = workingItems.removeAt(curIdx)
                                        workingItems.add(targetIdx, moved)
                                    }
                                } else if (viewMode == ViewMode.LIST && totalSlotHeightPx > 0f) {
                                    val minAllowedY = (0 - dragInitialIndex) * totalSlotHeightPx - totalSlotHeightPx * 0.35f
                                    val maxAllowedY = (workingItems.size - 1 - dragInitialIndex) * totalSlotHeightPx + totalSlotHeightPx * 0.35f
                                    val effectiveY = dragAccumulatedY.coerceIn(minAllowedY, maxAllowedY)

                                    val targetIdx = (dragInitialIndex + (effectiveY / totalSlotHeightPx).roundToInt())
                                        .coerceIn(0, workingItems.size - 1)
                                    if (targetIdx != curIdx) {
                                        val moved = workingItems.removeAt(curIdx)
                                        workingItems.add(targetIdx, moved)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            delay(16)
        }
    }

    val emptySpaceGestureModifier = if (!isItemEditMode && !isSearchActive) {
        Modifier.pointerInput(isItemEditMode, isSearchActive) {
            detectTapGestures(
                onLongPress = {
                    onEnterItemEditMode()
                }
            )
        }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewportHeightPx = size.height.toFloat()
            }
            .background(Color.Transparent)
            .then(emptySpaceGestureModifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (viewMode == ViewMode.GRID) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val itemWidthDp = (maxWidth - 40.dp - 12.dp) / 2
                    val itemHeightDp = itemWidthDp + 76.dp
                    LaunchedEffect(itemWidthDp, itemHeightDp) {
                        cellTotalWidthPx = with(density) { (itemWidthDp + 12.dp).toPx() }
                        cellTotalHeightPx = with(density) { (itemHeightDp + 12.dp).toPx() }
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .then(emptySpaceGestureModifier),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 150.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(500f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onLongPress = { })
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (currentFolder != null) {
                                            Surface(
                                                shape = CircleShape,
                                                color = colors.surfaceSecondary,
                                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                                tonalElevation = 4.dp,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { currentFolder = null },
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Torna all'armadio",
                                                        tint = colors.accent,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }
                                        }
                                        val wardrobeTitle = if (isItemEditMode) "Gestione elementi" else (currentFolder ?: "Armadio")
                                        AnimatedContent(
                                            targetState = wardrobeTitle,
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(220, delayMillis = 40)) + slideInVertically { it / 3 })
                                                    .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutVertically { -it / 3 })
                                            },
                                            label = "wardrobeGridTitleAnim"
                                        ) { title ->
                                            Text(
                                                text = title,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colors.textPrimary,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(500f)
                            ) {
                                WardrobeHeaderControls(
                                    isEditMode = isItemEditMode,
                                    selectedCloset = selectedCloset,
                                    availableClosets = availableClosets,
                                    showClosetMenu = showClosetMenu,
                                    onShowClosetMenuChange = { showClosetMenu = it },
                                    onSelectCloset = { newCloset ->
                                        selectedCloset = newCloset
                                        prefs.edit().putString("wardrobe_selected_closet", newCloset).apply()
                                        currentFolder = null
                                    },
                                    onManageClosetsClick = {
                                        internalShowManageClosets = true
                                        onOpenManageClosets()
                                    },
                                    currentSortOrder = viewModel.sortOrder,
                                    showSortMenu = showSortMenu,
                                    onShowSortMenuChange = { showSortMenu = it },
                                    onSelectSortOrder = { viewModel.setSortingOrder(it, selectedCloset) },
                                    viewMode = viewMode,
                                    onViewModeChange = { newMode ->
                                        viewMode = newMode
                                        prefs.edit().putString("wardrobe_view_mode", newMode.name).apply()
                                    }
                                )
                            }
                        }

                        if (foldersToShow.isNotEmpty()) {
                            itemsIndexed(foldersToShow, key = { _, f -> "folder_${f.folderName}" }) { _, folder ->
                                val count = folderItemCounts[folder.folderName] ?: 0
                                val effectiveCover = folder.coverImageUri ?: folderFirstImages[folder.folderName]
                                val folderScale by animateFloatAsState(
                                    targetValue = if (isItemEditMode) 0.95f else 1f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioNoBouncy
                                    ),
                                    label = "gridFolderScale"
                                )
                                Box(
                                    modifier = Modifier
                                        .zIndex(200f)
                                        .graphicsLayer {
                                            scaleX = folderScale
                                            scaleY = folderScale
                                        }
                                ) {
                                    FolderGridCard(
                                        folderName = folder.folderName,
                                        itemCount = count,
                                        coverUri = effectiveCover,
                                        coverEmoji = folder.coverEmoji,
                                        isEditMode = isItemEditMode,
                                        onClick = { currentFolder = folder.folderName },
                                        onEditClick = { folderToEdit = folder }
                                    )
                                }
                            }
                        }

                        if (displayItemsList.isEmpty() && foldersToShow.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                EmptyClosetState(isFolder = currentFolder != null, isSearch = isSearchActive)
                            }
                        } else {
                            itemsIndexed(displayItemsList, key = { _, item -> item.id }) { index, item ->
                                val isDraggedItem = draggedItemId == item.id
                                val isSelected = selectedItemIds.contains(item.id)
                                val currentScale by animateFloatAsState(
                                    targetValue = when {
                                        isDraggedItem -> 1.06f
                                        isItemEditMode -> 0.95f
                                        else -> 1f
                                    },
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioNoBouncy
                                    ),
                                    label = "gridItemScale"
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
                                            if (isItemEditMode) {
                                                Modifier.pointerInput(item.id) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset ->
                                                            draggedItemId = item.id
                                                            dragInitialIndex = workingItems.indexOfFirst { it.id == item.id }
                                                            dragAccumulatedX = 0f
                                                            dragAccumulatedY = 0f
                                                            dragFingerViewportY = itemRootY + offset.y
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragAccumulatedX += dragAmount.x
                                                            dragAccumulatedY += dragAmount.y
                                                            dragFingerViewportY += dragAmount.y

                                                            val curIdx = workingItems.indexOfFirst { it.id == item.id }
                                                            if (curIdx != -1 && cellTotalHeightPx > 0f && cellTotalWidthPx > 0f && dragInitialIndex != -1) {
                                                                val dragInitialRow = dragInitialIndex / 2
                                                                val dragInitialCol = dragInitialIndex % 2
                                                                val maxRow = (workingItems.size - 1) / 2

                                                                val minAllowedY = (0 - dragInitialRow) * cellTotalHeightPx - cellTotalHeightPx * 0.35f
                                                                val maxAllowedY = (maxRow - dragInitialRow) * cellTotalHeightPx + cellTotalHeightPx * 0.35f
                                                                val effectiveY = dragAccumulatedY.coerceIn(minAllowedY, maxAllowedY)

                                                                val minAllowedX = (0 - dragInitialCol) * cellTotalWidthPx - cellTotalWidthPx * 0.35f
                                                                val maxAllowedX = (1 - dragInitialCol) * cellTotalWidthPx + cellTotalWidthPx * 0.35f
                                                                val effectiveX = dragAccumulatedX.coerceIn(minAllowedX, maxAllowedX)

                                                                val targetRow = (dragInitialRow + (effectiveY / cellTotalHeightPx).roundToInt()).coerceIn(0, maxRow)
                                                                val targetCol = (dragInitialCol + (effectiveX / cellTotalWidthPx).roundToInt()).coerceIn(0, 1)
                                                                val targetIdx = (targetRow * 2 + targetCol).coerceIn(0, workingItems.size - 1)
                                                                if (targetIdx != curIdx) {
                                                                    val moved = workingItems.removeAt(curIdx)
                                                                    workingItems.add(targetIdx, moved)
                                                                }
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            viewModel.updateClothingOrder(workingItems.map { it.id })
                                                            draggedItemId = null
                                                            dragInitialIndex = -1
                                                            dragAccumulatedX = 0f
                                                            dragAccumulatedY = 0f
                                                            dragFingerViewportY = 0f
                                                        },
                                                        onDragCancel = {
                                                            viewModel.updateClothingOrder(workingItems.map { it.id })
                                                            draggedItemId = null
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
                                                val curIdx = workingItems.indexOfFirst { it.id == item.id }
                                                val effectiveIdx = if (curIdx != -1) curIdx else dragInitialIndex
                                                val curRow = effectiveIdx / 2
                                                val curCol = effectiveIdx % 2
                                                val dragInitialRow = dragInitialIndex / 2
                                                val dragInitialCol = dragInitialIndex % 2
                                                val maxRow = (workingItems.size - 1) / 2

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
                                    val selectedQty = selectedItemQuantities[item.id] ?: item.quantity
                                    ClothingGridCard(
                                        item = item,
                                        isEditMode = isItemEditMode,
                                        isInsideFolder = currentFolder != null,
                                        isSelected = isSelected,
                                        selectedQuantity = selectedQty,
                                        isDragging = isDraggedItem,
                                        onIncrement = { viewModel.incrementQuantity(item.id) },
                                        onDecrement = { viewModel.decrementQuantity(item.id) },
                                        onIncrementSelected = {
                                            val curr = selectedItemQuantities[item.id] ?: item.quantity
                                            if (curr < item.quantity) {
                                                selectedItemQuantities[item.id] = curr + 1
                                            }
                                        },
                                        onDecrementSelected = {
                                            val curr = selectedItemQuantities[item.id] ?: item.quantity
                                            if (curr > 1) {
                                                selectedItemQuantities[item.id] = curr - 1
                                            }
                                        },
                                        onToggleSelect = {
                                            if (isSelected) {
                                                selectedItemIds.remove(item.id)
                                                selectedItemQuantities.remove(item.id)
                                            } else {
                                                selectedItemIds.add(item.id)
                                                if (item.isCountable) {
                                                    selectedItemQuantities[item.id] = item.quantity
                                                }
                                            }
                                        },
                                        onClick = { itemDetailId = item.id },
                                        onEditClick = { itemToEdit = item }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val listItemHeightDp = 76.dp
                    val listItemSpacingDp = 10.dp
                    LaunchedEffect(listItemHeightDp, listItemSpacingDp) {
                        totalSlotHeightPx = with(density) { (listItemHeightDp + listItemSpacingDp).toPx() }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(emptySpaceGestureModifier),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(500f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onLongPress = { })
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (currentFolder != null) {
                                            Surface(
                                                shape = CircleShape,
                                                color = colors.surfaceSecondary,
                                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                                tonalElevation = 4.dp,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { currentFolder = null },
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Torna all'armadio",
                                                        tint = colors.accent,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }
                                        }
                                        val wardrobeTitle = if (isItemEditMode) "Gestione elementi" else (currentFolder ?: "Armadio")
                                        AnimatedContent(
                                            targetState = wardrobeTitle,
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(220, delayMillis = 40)) + slideInVertically { it / 3 })
                                                    .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutVertically { -it / 3 })
                                            },
                                            label = "wardrobeGridTitleAnim"
                                        ) { title ->
                                            Text(
                                                text = title,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colors.textPrimary,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(500f)
                            ) {
                                WardrobeHeaderControls(
                                    isEditMode = isItemEditMode,
                                    selectedCloset = selectedCloset,
                                    availableClosets = availableClosets,
                                    showClosetMenu = showClosetMenu,
                                    onShowClosetMenuChange = { showClosetMenu = it },
                                    onSelectCloset = { newCloset ->
                                        selectedCloset = newCloset
                                        prefs.edit().putString("wardrobe_selected_closet", newCloset).apply()
                                        currentFolder = null
                                    },
                                    onManageClosetsClick = {
                                        internalShowManageClosets = true
                                        onOpenManageClosets()
                                    },
                                    currentSortOrder = viewModel.sortOrder,
                                    showSortMenu = showSortMenu,
                                    onShowSortMenuChange = { showSortMenu = it },
                                    onSelectSortOrder = { viewModel.setSortingOrder(it, selectedCloset) },
                                    viewMode = viewMode,
                                    onViewModeChange = { newMode ->
                                        viewMode = newMode
                                        prefs.edit().putString("wardrobe_view_mode", newMode.name).apply()
                                    }
                                )
                            }
                        }

                        if (foldersToShow.isNotEmpty()) {
                            itemsIndexed(foldersToShow, key = { _, f -> "folder_${f.folderName}" }) { _, folder ->
                                val count = folderItemCounts[folder.folderName] ?: 0
                                val effectiveCover = folder.coverImageUri ?: folderFirstImages[folder.folderName]
                                val folderScale by animateFloatAsState(
                                    targetValue = if (isItemEditMode) 0.95f else 1f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioNoBouncy
                                    ),
                                    label = "listFolderScale"
                                )
                                Box(
                                    modifier = Modifier
                                        .zIndex(200f)
                                        .graphicsLayer {
                                            scaleX = folderScale
                                            scaleY = folderScale
                                        }
                                ) {
                                    FolderListCard(
                                        folderName = folder.folderName,
                                        itemCount = count,
                                        coverUri = effectiveCover,
                                        coverEmoji = folder.coverEmoji,
                                        isEditMode = isItemEditMode,
                                        onClick = { currentFolder = folder.folderName },
                                        onEditClick = { folderToEdit = folder }
                                    )
                                }
                            }
                        }

                        if (displayItemsList.isEmpty() && foldersToShow.isEmpty()) {
                            item { EmptyClosetState(isFolder = currentFolder != null, isSearch = isSearchActive) }
                        } else {
                            itemsIndexed(displayItemsList, key = { _, item -> item.id }) { index, item ->
                                val isDraggedItem = draggedItemId == item.id
                                val isSelected = selectedItemIds.contains(item.id)
                                val currentScale by animateFloatAsState(
                                    targetValue = when {
                                        isDraggedItem -> 1.04f
                                        isItemEditMode -> 0.95f
                                        else -> 1f
                                    },
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioNoBouncy
                                    ),
                                    label = "listItemScale"
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
                                            if (size.height > 0) {
                                                totalSlotHeightPx = size.height.toFloat() + with(density) { 10.dp.toPx() }
                                            }
                                        }
                                        .then(
                                            if (isItemEditMode) {
                                                Modifier.pointerInput(item.id) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset ->
                                                            draggedItemId = item.id
                                                            dragInitialIndex = workingItems.indexOfFirst { it.id == item.id }
                                                            dragAccumulatedY = 0f
                                                            dragFingerViewportY = itemRootY + offset.y
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragAccumulatedY += dragAmount.y
                                                            dragFingerViewportY += dragAmount.y

                                                            val curIdx = workingItems.indexOfFirst { it.id == item.id }
                                                            if (curIdx != -1 && totalSlotHeightPx > 0f && dragInitialIndex != -1) {
                                                                val minAllowedY = (0 - dragInitialIndex) * totalSlotHeightPx - totalSlotHeightPx * 0.35f
                                                                val maxAllowedY = (workingItems.size - 1 - dragInitialIndex) * totalSlotHeightPx + totalSlotHeightPx * 0.35f
                                                                val effectiveY = dragAccumulatedY.coerceIn(minAllowedY, maxAllowedY)

                                                                val targetIdx = (dragInitialIndex + (effectiveY / totalSlotHeightPx).roundToInt())
                                                                    .coerceIn(0, workingItems.size - 1)
                                                                if (targetIdx != curIdx) {
                                                                    val moved = workingItems.removeAt(curIdx)
                                                                    workingItems.add(targetIdx, moved)
                                                                }
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            viewModel.updateClothingOrder(workingItems.map { it.id })
                                                            draggedItemId = null
                                                            dragInitialIndex = -1
                                                            dragAccumulatedY = 0f
                                                            dragFingerViewportY = 0f
                                                        },
                                                        onDragCancel = {
                                                            viewModel.updateClothingOrder(workingItems.map { it.id })
                                                            draggedItemId = null
                                                            dragInitialIndex = -1
                                                            dragAccumulatedY = 0f
                                                            dragFingerViewportY = 0f
                                                        }
                                                    )
                                                }
                                            } else Modifier
                                        )
                                        .graphicsLayer {
                                            if (isDraggedItem && dragInitialIndex != -1 && totalSlotHeightPx > 0f) {
                                                val curIdx = workingItems.indexOfFirst { it.id == item.id }
                                                val effectiveIdx = if (curIdx != -1) curIdx else dragInitialIndex
                                                val minAllowedY = (0 - dragInitialIndex) * totalSlotHeightPx - totalSlotHeightPx * 0.35f
                                                val maxAllowedY = (workingItems.size - 1 - dragInitialIndex) * totalSlotHeightPx + totalSlotHeightPx * 0.35f
                                                val effectiveY = dragAccumulatedY.coerceIn(minAllowedY, maxAllowedY)

                                                translationY = effectiveY - (effectiveIdx - dragInitialIndex) * totalSlotHeightPx
                                                shadowElevation = 20f
                                            } else {
                                                translationY = 0f
                                                shadowElevation = 0f
                                            }
                                        }
                                ) {
                                    val selectedQty = selectedItemQuantities[item.id] ?: item.quantity
                                    ClothingListCard(
                                        item = item,
                                        isEditMode = isItemEditMode,
                                        isInsideFolder = currentFolder != null,
                                        isSelected = isSelected,
                                        selectedQuantity = selectedQty,
                                        isDragging = isDraggedItem,
                                        onIncrement = { viewModel.incrementQuantity(item.id) },
                                        onDecrement = { viewModel.decrementQuantity(item.id) },
                                        onIncrementSelected = {
                                            val curr = selectedItemQuantities[item.id] ?: item.quantity
                                            if (curr < item.quantity) {
                                                selectedItemQuantities[item.id] = curr + 1
                                            }
                                        },
                                        onDecrementSelected = {
                                            val curr = selectedItemQuantities[item.id] ?: item.quantity
                                            if (curr > 1) {
                                                selectedItemQuantities[item.id] = curr - 1
                                            }
                                        },
                                        onToggleSelect = {
                                            if (isSelected) {
                                                selectedItemIds.remove(item.id)
                                                selectedItemQuantities.remove(item.id)
                                            } else {
                                                selectedItemIds.add(item.id)
                                                if (item.isCountable) {
                                                    selectedItemQuantities[item.id] = item.quantity
                                                }
                                            }
                                        },
                                        onClick = { itemDetailId = item.id },
                                        onEditClick = { itemToEdit = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeDetailItem = if (itemDetailId != null) {
        displayItems.firstOrNull { it.id == itemDetailId }
    } else null

    if (activeDetailItem != null) {
        ClothingDetailDialog(
            item = activeDetailItem,
            onDismiss = { itemDetailId = null },
            onIncrement = { viewModel.incrementQuantity(activeDetailItem.id) },
            onDecrement = { viewModel.decrementQuantity(activeDetailItem.id) },
            onEditClick = {
                itemDetailId = null
                itemToEdit = activeDetailItem
            }
        )
    }

    if (showManageTagsDialog) {
        ManageTagsDialog(
            viewModel = viewModel,
            onDismiss = onDismissManageTags
        )
    }

    if (showManageClosetsDialog || internalShowManageClosets) {
        ManageClosetsDialog(
            viewModel = viewModel,
            onDismiss = {
                onDismissManageClosets()
                internalShowManageClosets = false
            }
        )
    }

    if (folderToEdit != null) {
        FolderEditDialog(
            folder = folderToEdit!!,
            onDismiss = { folderToEdit = null },
            onCameraActiveChange = onCameraActiveChange,
            onSave = { updatedFolder ->
                val oldName = folderToEdit!!.folderName
                val newName = updatedFolder.folderName
                viewModel.deleteFolder(folderToEdit!!.closetName, oldName)
                viewModel.addFolder(updatedFolder.closetName, newName, updatedFolder.coverImageUri, updatedFolder.coverEmoji)
                if (oldName != newName) {
                    val itemsInFolder = clothingItems.filter { it.closetName == folderToEdit!!.closetName && it.folderName == oldName }
                    viewModel.moveItemsToTarget(itemsInFolder.map { it.id }.toSet(), folderToEdit!!.closetName, newName)
                }
                folderToEdit = null
            }
        )
    }

    if (triggerCreateFolder) {
        FolderCreateDialog(
            selectedCloset = selectedCloset,
            selectedItemIds = selectedItemIds,
            onDismiss = onFolderDialogConsumed,
            onCameraActiveChange = onCameraActiveChange,
            onSave = { folderName, coverUri, coverEmoji ->
                val clean = folderName.trim()
                if (clean.isNotBlank()) {
                    viewModel.addFolder(selectedCloset, clean, coverUri, coverEmoji)
                    if (selectedItemIds.isNotEmpty()) {
                        viewModel.moveItemsToTarget(
                            itemIds = selectedItemIds.toSet(),
                            targetCloset = selectedCloset,
                            targetFolder = clean,
                            itemQuantities = selectedItemQuantities.toMap()
                        )
                        selectedItemIds.clear()
                        selectedItemQuantities.clear()
                    }
                }
                onFolderDialogConsumed()
            }
        )
    }

    if (triggerMoveItems) {
        var destCloset by remember { mutableStateOf(selectedCloset) }
        var destFolder by remember { mutableStateOf<String?>(null) }
        var showClosetPickerMenu by remember { mutableStateOf(false) }
        val targetClosetFolders = remember(destCloset) { viewModel.getFoldersForCloset(destCloset) }

        AlertDialog(
            onDismissRequest = onMoveDialogConsumed,
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                val totalPiecesToMove = selectedItemIds.sumOf { id ->
                    val cItem = clothingItems.firstOrNull { it.id == id }
                    if (cItem?.isCountable == true) (selectedItemQuantities[id] ?: cItem.quantity) else 1
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = colors.accent)
                    Text(
                        text = if (totalPiecesToMove == 1) "Sposta 1 elemento" else "Sposta $totalPiecesToMove elementi",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Armadio di destinazione", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceSecondary,
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier.fillMaxWidth().clickable { showClosetPickerMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                        Text(destCloset, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.textSecondary)
                                }
                            }
                            DropdownMenu(
                                expanded = showClosetPickerMenu,
                                onDismissRequest = { showClosetPickerMenu = false },
                                modifier = Modifier.background(colors.dialogBackground),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                availableClosets.forEach { closet ->
                                    val isCurrentSelected = (closet == destCloset)
                                    DropdownMenuItem(
                                        text = { Text(closet, color = if (isCurrentSelected) colors.accent else colors.textPrimary, fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp) },
                                        trailingIcon = { if (isCurrentSelected) Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp)) },
                                        onClick = { destCloset = closet; destFolder = null; showClosetPickerMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder, thickness = 0.8.dp)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Seleziona cartella o radice", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                val isRootSelected = (destFolder == null)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isRootSelected) colors.accentContainer else colors.surfaceSecondary,
                                    border = BorderStroke(if (isRootSelected) 1.dp else 0.5.dp, if (isRootSelected) colors.accent else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().clickable { destFolder = null }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = if (isRootSelected) colors.accent else colors.textSecondary, modifier = Modifier.size(18.dp))
                                            Text("Nessuna cartella (Radice armadio)", color = if (isRootSelected) colors.accent else colors.textPrimary, fontSize = 13.sp, fontWeight = if (isRootSelected) FontWeight.Bold else FontWeight.Medium)
                                        }
                                        if (isRootSelected) Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            itemsIndexed(targetClosetFolders) { _, folder ->
                                val isFolderSelected = (destFolder == folder.folderName)
                                val count = clothingItems.count { it.closetName == destCloset && it.folderName == folder.folderName }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isFolderSelected) colors.accentContainer else colors.surfaceSecondary,
                                    border = BorderStroke(if (isFolderSelected) 1.dp else 0.5.dp, if (isFolderSelected) colors.accent else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().clickable { destFolder = folder.folderName }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = if (isFolderSelected) colors.accent else colors.textSecondary, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(folder.folderName, color = if (isFolderSelected) colors.accent else colors.textPrimary, fontSize = 13.sp, fontWeight = if (isFolderSelected) FontWeight.Bold else FontWeight.Medium)
                                                Text("$count capi", color = colors.textSecondary, fontSize = 11.sp)
                                            }
                                        }
                                        if (isFolderSelected) Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveItemsToTarget(
                            itemIds = selectedItemIds.toSet(),
                            targetCloset = destCloset,
                            targetFolder = destFolder,
                            itemQuantities = selectedItemQuantities.toMap()
                        )
                        selectedItemIds.clear()
                        selectedItemQuantities.clear()
                        onMoveDialogConsumed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = CircleShape
                ) { Text("Conferma Spostamento", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = onMoveDialogConsumed) { Text("Annulla", color = colors.textSecondary) }
            }
        )
    }

    // Dialog con Morphing per la Creazione del Capo
    if (showAddItemSheet) {
        AddOrEditClothingDialog(
            viewModel = viewModel,
            availableClosets = availableClosets,
            initialCloset = selectedCloset,
            initialFolder = currentFolder,
            itemToEdit = null,
            onDismiss = onDismissAddItemSheet,
            onCameraActiveChange = onCameraActiveChange,
            onSave = { newItem ->
                viewModel.addClothingItem(newItem)
                if (selectedCloset != newItem.closetName) {
                    selectedCloset = newItem.closetName
                    prefs.edit().putString("wardrobe_selected_closet", newItem.closetName).apply()
                    currentFolder = newItem.folderName
                }
                onDismissAddItemSheet()
            }
        )
    }

    // Dialog con Morphing per la Modifica del Capo
    if (itemToEdit != null) {
        AddOrEditClothingDialog(
            viewModel = viewModel,
            availableClosets = availableClosets,
            initialCloset = selectedCloset,
            initialFolder = currentFolder,
            itemToEdit = itemToEdit,
            onDismiss = { itemToEdit = null },
            onCameraActiveChange = onCameraActiveChange,
            onSave = { updatedItem ->
                viewModel.updateClothingItem(updatedItem)
                itemToEdit = null
            }
        )
    }
}

