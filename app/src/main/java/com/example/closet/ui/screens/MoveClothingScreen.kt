@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ClosetViewModel
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import java.util.Locale
import java.time.format.DateTimeFormatter

@Composable
fun MoveClothingScreen(
    viewModel: ClosetViewModel,
    isLaundryMode: Boolean,
    onBack: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val closets = viewModel.availableClosets

    val screenAccent = if (viewModel.useDistinctFunctionColors) {
        if (isLaundryMode) Color(0xFFEF4444) else Color(0xFF10B981)
    } else {
        colors.accent
    }

    var viewMode by remember { mutableStateOf(ViewMode.GRID) }

    var sourceCloset by remember {
        mutableStateOf(
            if (viewModel.suitcaseSourceCloset.isNotBlank() && closets.contains(viewModel.suitcaseSourceCloset)) {
                viewModel.suitcaseSourceCloset
            } else {
                closets.firstOrNull() ?: "Armadio Principale"
            }
        )
    }

    var destCloset by remember {
        mutableStateOf(
            if (viewModel.suitcaseDestCloset.isNotBlank() && closets.contains(viewModel.suitcaseDestCloset)) {
                suitcaseDestSafe(closets, sourceCloset, viewModel.suitcaseDestCloset)
            } else {
                closets.getOrNull(1) ?: sourceCloset
            }
        )
    }

    var currentFolder by remember { mutableStateOf<String?>(null) }
    var showSourceMenu by remember { mutableStateOf(false) }
    var showDestMenu by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showTimeWheelPicker by remember { mutableStateOf(false) }
    var showDurationWheelPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val selectedQuantities = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        val saved = if (isLaundryMode) viewModel.savedLaundrySelections else viewModel.savedMoveSelections
        if (saved.isNotEmpty()) {
            selectedQuantities.putAll(saved)
        }
    }

    val handleBack: () -> Unit = {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
            filterTags = emptySet()
        } else if (currentFolder != null) {
            currentFolder = null
        } else if (selectedQuantities.isNotEmpty()) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    LaunchedEffect(sourceCloset) {
        viewModel.setSuitcaseClosets(sourceCloset, destCloset)
    }

    LaunchedEffect(destCloset) {
        viewModel.setSuitcaseClosets(sourceCloset, destCloset)
    }

    val allItemsInSource = remember(viewModel.clothingItems.toList(), sourceCloset) {
        viewModel.clothingItems.filter { it.closetName == sourceCloset }
    }

    val availableFoldersInSource = remember(viewModel.closetFolders.toList(), sourceCloset, searchQuery, filterTags, isSearchActive) {
        val folders = viewModel.getFoldersForCloset(sourceCloset)
        if (isSearchActive) {
            folders.filter { folder ->
                val folderItems = allItemsInSource.filter { it.folderName == folder.folderName }
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
                    if (searchQuery.isNotBlank()) (nameMatches || itemsMatch) && tagItemsMatch else tagItemsMatch
                } else if (searchQuery.isNotBlank()) {
                    nameMatches || itemsMatch
                } else {
                    true
                }
            }
        } else {
            folders
        }
    }

    val visibleItemsInSource = remember(allItemsInSource, currentFolder, searchQuery, filterTags, isSearchActive) {
        allItemsInSource.filter { item ->
            val matchFolder = if (isSearchActive) {
                if (currentFolder != null) item.folderName == currentFolder else true
            } else {
                item.folderName == currentFolder
            }
            val matchQuery = if (isSearchActive && searchQuery.isNotBlank()) {
                item.name.contains(searchQuery.trim(), ignoreCase = true) ||
                (item.folderName?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
                item.tags.any { it.contains(searchQuery.trim(), ignoreCase = true) }
            } else {
                true
            }
            val matchTag = if (isSearchActive && filterTags.isNotEmpty()) {
                filterTags.all { fTag -> item.tags.any { it.equals(fTag, ignoreCase = true) } }
            } else {
                true
            }
            matchFolder && matchQuery && matchTag
        }
    }

    val totalSelectedPieces = remember(selectedQuantities.toMap()) {
        selectedQuantities.values.sum()
    }

    val topFadeColor = when {
        viewModel.currentTheme == AppTheme.AMOLED -> Color.Black
        colors.isDark -> Color(0xFF13171E)
        else -> Color(0xFFF2F4F8)
    }
    val bottomFadeColor = when {
        viewModel.currentTheme == AppTheme.AMOLED -> Color.Black
        colors.isDark -> colors.fadeColorBottom
        else -> Color(0xFFE8EBF0)
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val isScrolled = if (viewMode == ViewMode.GRID) {
        gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 4
    } else {
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4
    }
    val topFadeAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(180),
        label = "moveTopFadeAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp)
        ) {
            // TOP BAR ONE UI CON TASTO INDIETRO E TITOLO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceSecondary,
                    border = BorderStroke(0.8.dp, colors.cardBorder),
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna alle funzioni",
                            tint = screenAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = if (isLaundryMode) "Panni" else "Spostamento",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = 0.3.sp
                )
            }

            // SELETTORE TRASFERIMENTO ARMADI (ONE UI STYLE)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surfaceSecondary,
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ARMADIO DI ORIGINE (DA)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBackground)
                            .border(BorderStroke(0.8.dp, colors.cardBorder), RoundedCornerShape(16.dp))
                            .clickable { showSourceMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = screenAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "DA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = screenAccent,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = sourceCloset,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f).basicMarquee()
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSourceMenu,
                            onDismissRequest = { showSourceMenu = false },
                            modifier = Modifier.background(colors.dialogBackground),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            closets.forEach { cl ->
                                DropdownMenuItem(
                                    text = { Text(cl, color = if (cl == sourceCloset) screenAccent else colors.textPrimary, fontSize = 13.sp) },
                                    trailingIcon = { if (cl == sourceCloset) Icon(Icons.Default.Check, contentDescription = null, tint = screenAccent, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        sourceCloset = cl
                                        if (destCloset == cl) {
                                            destCloset = closets.firstOrNull { it != cl } ?: cl
                                        }
                                        selectedQuantities.clear()
                                        currentFolder = null
                                        showSourceMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // TASTO INVERTI CENTRALE
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(screenAccent.copy(alpha = 0.18f))
                            .clickable {
                                val temp = sourceCloset
                                sourceCloset = destCloset
                                destCloset = temp
                                selectedQuantities.clear()
                                currentFolder = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Inverti armadi",
                            tint = screenAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // ARMADIO DI DESTINAZIONE (A)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBackground)
                            .border(BorderStroke(0.8.dp, screenAccent.copy(alpha = 0.45f)), RoundedCornerShape(16.dp))
                            .clickable { showDestMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = screenAccent
                            ) {
                                Text(
                                    text = "A",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = destCloset,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = screenAccent,
                                maxLines = 1,
                                modifier = Modifier.weight(1f).basicMarquee()
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = screenAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDestMenu,
                            onDismissRequest = { showDestMenu = false },
                            modifier = Modifier.background(colors.dialogBackground),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            closets.forEach { cl ->
                                DropdownMenuItem(
                                    text = { Text(cl, color = if (cl == destCloset) screenAccent else colors.textPrimary, fontSize = 13.sp) },
                                    trailingIcon = { if (cl == destCloset) Icon(Icons.Default.Check, contentDescription = null, tint = screenAccent, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        destCloset = cl
                                        if (sourceCloset == cl) {
                                            sourceCloset = closets.firstOrNull { it != cl } ?: cl
                                        }
                                        showDestMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (currentFolder != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = screenAccent.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currentFolder = null }
                        .padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = screenAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Cartella: $currentFolder (Tocca per tornare)",
                            color = screenAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (allItemsInSource.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedQuantities.isNotEmpty()) screenAccent.copy(alpha = 0.15f) else colors.surfaceSecondary,
                            border = BorderStroke(0.8.dp, if (selectedQuantities.isNotEmpty()) screenAccent.copy(alpha = 0.4f) else colors.cardBorder),
                            modifier = Modifier.clickable {
                                if (selectedQuantities.isNotEmpty()) {
                                    selectedQuantities.clear()
                                } else {
                                    allItemsInSource.forEach { item ->
                                        selectedQuantities[item.id] = if (item.isCountable) item.quantity else 1
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedQuantities.isNotEmpty()) "Deseleziona tutti" else "Seleziona tutti",
                                fontSize = 12.sp,
                                color = if (selectedQuantities.isNotEmpty()) screenAccent else colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = "$totalSelectedPieces selezionati",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = colors.surfaceSecondary,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (viewMode == ViewMode.GRID) screenAccent.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { viewMode = ViewMode.GRID },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Griglia",
                                tint = if (viewMode == ViewMode.GRID) screenAccent else colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (viewMode == ViewMode.LIST) screenAccent.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { viewMode = ViewMode.LIST },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Elenco",
                                tint = if (viewMode == ViewMode.LIST) screenAccent else colors.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (viewMode == ViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentFolder == null && availableFoldersInSource.isNotEmpty()) {
                            items(availableFoldersInSource, key = { "move_f_${it.folderName}" }) { folder ->
                                val folderCount = allItemsInSource.filter { it.folderName == folder.folderName }
                                    .sumOf { if (it.isCountable) it.quantity else 1 }
                                val effectiveCover = folder.coverImageUri ?: allItemsInSource.firstOrNull { it.folderName == folder.folderName }?.imageUri

                                FolderGridCard(
                                    folderName = folder.folderName,
                                    itemCount = folderCount,
                                    coverUri = effectiveCover,
                                    coverEmoji = folder.coverEmoji,
                                    isEditMode = false,
                                    onClick = { currentFolder = folder.folderName },
                                    onEditClick = {}
                                )
                            }
                        }

                        if (visibleItemsInSource.isEmpty() && (currentFolder != null || availableFoldersInSource.isEmpty())) {
                            item(span = { GridItemSpan(2) }) {
                                EmptyClosetState(isFolder = currentFolder != null, isSearch = isSearchActive)
                            }
                        } else {
                            items(visibleItemsInSource, key = { it.id }) { item ->
                                val selectedQty = selectedQuantities[item.id] ?: 0

                                SuitcaseClothingGridCard(
                                    item = item,
                                    selectedQty = selectedQty,
                                    itemAccentColor = screenAccent,
                                    onToggleSelection = {
                                        if (selectedQty > 0) {
                                            selectedQuantities.remove(item.id)
                                        } else {
                                            selectedQuantities[item.id] = 1
                                        }
                                    },
                                    onIncrement = {
                                        val current = selectedQuantities[item.id] ?: 0
                                        if (current < item.quantity) {
                                            selectedQuantities[item.id] = current + 1
                                        }
                                    },
                                    onDecrement = {
                                        val current = selectedQuantities[item.id] ?: 0
                                        if (current > 1) {
                                            selectedQuantities[item.id] = current - 1
                                        } else {
                                            selectedQuantities.remove(item.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentFolder == null && availableFoldersInSource.isNotEmpty()) {
                            items(availableFoldersInSource, key = { "move_f_list_${it.folderName}" }) { folder ->
                                val folderCount = allItemsInSource.filter { it.folderName == folder.folderName }
                                    .sumOf { if (it.isCountable) it.quantity else 1 }
                                val effectiveCover = folder.coverImageUri ?: allItemsInSource.firstOrNull { it.folderName == folder.folderName }?.imageUri

                                FolderListCard(
                                    folderName = folder.folderName,
                                    itemCount = folderCount,
                                    coverUri = effectiveCover,
                                    coverEmoji = folder.coverEmoji,
                                    isEditMode = false,
                                    onClick = { currentFolder = folder.folderName },
                                    onEditClick = {}
                                )
                            }
                        }

                        if (visibleItemsInSource.isEmpty() && (currentFolder != null || availableFoldersInSource.isEmpty())) {
                            item {
                                EmptyClosetState(isFolder = currentFolder != null, isSearch = isSearchActive)
                            }
                        } else {
                            items(visibleItemsInSource, key = { it.id }) { item ->
                                val selectedQty = selectedQuantities[item.id] ?: 0

                                SuitcaseClothingListCard(
                                    item = item,
                                    selectedQty = selectedQty,
                                    itemAccentColor = screenAccent,
                                    onToggleSelection = {
                                        if (selectedQty > 0) {
                                            selectedQuantities.remove(item.id)
                                        } else {
                                            selectedQuantities[item.id] = 1
                                        }
                                    },
                                    onIncrement = {
                                        val current = selectedQuantities[item.id] ?: 0
                                        if (current < item.quantity) {
                                            selectedQuantities[item.id] = current + 1
                                        }
                                    },
                                    onDecrement = {
                                        val current = selectedQuantities[item.id] ?: 0
                                        if (current > 1) {
                                            selectedQuantities[item.id] = current - 1
                                        } else {
                                            selectedQuantities.remove(item.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp)
                        .fillMaxWidth()
                        .height(24.dp)
                        .graphicsLayer { alpha = topFadeAlpha }
                        .background(Brush.verticalGradient(listOf(topFadeColor, topFadeColor.copy(alpha = 0.6f), Color.Transparent)))
                        .zIndex(10f)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, bottomFadeColor.copy(alpha = 0.6f), bottomFadeColor)))
                        .zIndex(10f)
                )
            }
        }

        AnimatedVisibility(
            visible = !isSearchActive,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp)
                .zIndex(30f)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.islandBackground,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(0.5.dp, colors.cardBorder),
                modifier = Modifier
                    .height(54.dp)
                    .wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (selectedQuantities.isNotEmpty()) {
                                showExitDialog = true
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Text(
                            text = "Annulla",
                            color = Color(0xFFE55353),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Tasto Cerca (a sinistra del pulsante opzioni)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSearchActive) screenAccent else colors.surfaceSecondary)
                            .clickable { isSearchActive = !isSearchActive },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cerca",
                            tint = if (isSearchActive) Color.White else colors.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceSecondary)
                            .clickable { showCustomizeSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Opzioni",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    if (isLaundryMode) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceSecondary)
                                .clickable { showHistorySheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Cronologia Spostamenti",
                                tint = screenAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val canMove = totalSelectedPieces > 0 && sourceCloset != destCloset
                    Button(
                        onClick = {
                            if (isLaundryMode || viewModel.laundryDurationEnabled) {
                                viewModel.startLaundryTransfer(
                                    itemSelections = selectedQuantities.toMap(),
                                    fromCloset = sourceCloset,
                                    toCloset = destCloset,
                                    isLaundry = isLaundryMode
                                )
                            } else {
                                viewModel.transferSuitcaseItems(
                                    itemSelections = selectedQuantities.toMap(),
                                    fromCloset = sourceCloset,
                                    toCloset = destCloset
                                )
                            }
                            selectedQuantities.clear()
                            onBack()
                        },
                        enabled = canMove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = screenAccent,
                            disabledContainerColor = screenAccent.copy(alpha = 0.35f)
                        ),
                        shape = CircleShape,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "Sposta",
                            color = if (canMove) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Barra di Ricerca (come in armadio)
        AnimatedVisibility(
            visible = isSearchActive,
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
                if (viewModel.availableTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewModel.availableTags.forEach { tag ->
                            val isSel = filterTags.contains(tag)
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) screenAccent else colors.islandBackground,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) screenAccent else colors.cardBorder
                                ),
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp,
                                modifier = Modifier.clickable {
                                    filterTags = if (isSel) {
                                        filterTags - tag
                                    } else {
                                        filterTags + tag
                                    }
                                }
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSel) Color.White else colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = colors.islandBackground,
                    border = BorderStroke(1.dp, screenAccent.copy(alpha = 0.5f)),
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
                            tint = screenAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cerca nei capi...",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = ComposeTextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(screenAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancella",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                                filterTags = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = screenAccent),
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

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = if (isLaundryMode) "Uscire da Panni?" else "Uscire da Spostamento?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Puoi salvare le selezioni correnti per continuare più tardi oppure annullare e scartare le modifiche.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isLaundryMode) {
                                viewModel.savedLaundrySelections.clear()
                                viewModel.savedLaundrySelections.putAll(selectedQuantities)
                            } else {
                                viewModel.savedMoveSelections.clear()
                                viewModel.savedMoveSelections.putAll(selectedQuantities)
                            }
                            showExitDialog = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = screenAccent),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text("Salva e continua dopo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (isLaundryMode) {
                                    viewModel.savedLaundrySelections.clear()
                                } else {
                                    viewModel.savedMoveSelections.clear()
                                }
                                selectedQuantities.clear()
                                showExitDialog = false
                                onBack()
                            }
                        ) {
                            Text("Scarta modifiche", color = Color(0xFFE55353), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        TextButton(
                            onClick = { showExitDialog = false }
                        ) {
                            Text("Continua", color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showHistorySheet) {
        Dialog(
            onDismissRequest = { showHistorySheet = false },
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
                    .heightIn(max = 620.dp)
                    .padding(vertical = 16.dp)
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
                            text = "Cronologia Spostamenti",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        IconButton(onClick = { showHistorySheet = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Text(
                        text = "Tocca uno spostamento per impostare in automatico la partenza inversa e pre-selezionare i capi trasferiti.",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )

                    if (viewModel.transferHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nessun trasferimento precedente registrato.", color = colors.textMuted, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(viewModel.transferHistory.take(5), key = { it.id }) { record ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.surfaceSecondary,
                                    border = BorderStroke(0.5.dp, colors.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            sourceCloset = record.toCloset
                                            destCloset = record.fromCloset
                                            selectedQuantities.clear()

                                            record.itemsMoved.forEach { detail ->
                                                val found = viewModel.clothingItems.firstOrNull {
                                                    (it.id == detail.itemId || it.name.equals(detail.itemName, ignoreCase = true)) &&
                                                            it.closetName == record.toCloset
                                                }
                                                if (found != null) {
                                                    selectedQuantities[found.id] = detail.quantity.coerceAtMost(found.quantity)
                                                }
                                            }

                                            currentFolder = null
                                            showHistorySheet = false
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = record.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'ore' HH:mm")),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = screenAccent
                                            )
                                            Text(
                                                text = "Tocca per invertire",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = screenAccent
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(record.fromCloset, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = screenAccent, modifier = Modifier.size(14.dp))
                                            Text(record.toCloset, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }

                                        val summaryText = record.itemsMoved.joinToString(", ") {
                                            if (it.quantity > 1) "${it.quantity}x ${it.itemName}" else it.itemName
                                        }
                                        Text(
                                            text = summaryText,
                                            color = colors.textSecondary,
                                            fontSize = 11.5.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showHistorySheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = screenAccent),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Chiudi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showCustomizeSheet) {
        Dialog(
            onDismissRequest = { showCustomizeSheet = false },
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
                    .heightIn(max = 640.dp)
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isLaundryMode) "Personalizzazione Panni" else "Personalizzazione Spostamento",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        IconButton(onClick = { showCustomizeSheet = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Switch Imposta Durata Trasferimento (senza descrizione)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Imposta Durata Trasferimento",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.laundryDurationEnabled,
                                onCheckedChange = { viewModel.laundryDurationEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = screenAccent)
                            )
                        }

                        if (viewModel.laundryDurationEnabled) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colors.surfaceSecondary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.laundryUseExactEndTime = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (!viewModel.laundryUseExactEndTime) screenAccent else Color.Transparent),
                                            shape = CircleShape,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Per Ore/Minuti", fontSize = 11.sp, color = if (!viewModel.laundryUseExactEndTime) Color.White else colors.textPrimary)
                                        }
                                        Button(
                                            onClick = { viewModel.laundryUseExactEndTime = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.laundryUseExactEndTime) screenAccent else Color.Transparent),
                                            shape = CircleShape,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Giorno e Ora di Fine", fontSize = 11.sp, color = if (viewModel.laundryUseExactEndTime) Color.White else colors.textPrimary)
                                        }
                                    }

                                    if (!viewModel.laundryUseExactEndTime) {
                                        // Selettore Durata con wheel picker
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = colors.dialogBackground,
                                            border = BorderStroke(0.5.dp, colors.cardBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showDurationWheelPicker = true }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Schedule,
                                                        contentDescription = null,
                                                        tint = screenAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "Durata",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colors.textPrimary
                                                    )
                                                }
                                                val durationDisplay = if (viewModel.laundryDurationMinutes > 0) {
                                                    "${viewModel.laundryDurationHours} h ${viewModel.laundryDurationMinutes} min"
                                                } else {
                                                    "${viewModel.laundryDurationHours} h"
                                                }
                                                Text(
                                                    text = durationDisplay,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = screenAccent
                                                )
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Selettore Giorno
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = colors.dialogBackground,
                                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showDatePicker = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.CalendarToday,
                                                            contentDescription = null,
                                                            tint = screenAccent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = "Giorno di Fine",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = colors.textPrimary
                                                        )
                                                    }
                                                    Text(
                                                        text = viewModel.laundryExactDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = screenAccent
                                                    )
                                                }
                                            }

                                            // Selettore Ora di Fine con wheel picker a scorrimento
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = colors.dialogBackground,
                                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showTimeWheelPicker = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Schedule,
                                                            contentDescription = null,
                                                            tint = screenAccent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = "Ora di Fine",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = colors.textPrimary
                                                        )
                                                    }
                                                    Text(
                                                        text = String.format(Locale.ROOT, "%02d:%02d", viewModel.laundryExactHour, viewModel.laundryExactMinute),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = screenAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Switch Visualizza capi nell'armadio di arrivo (senza descrizione)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Visualizza capi nell'armadio di arrivo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.laundryShowDimmedInDestination,
                                onCheckedChange = { viewModel.laundryShowDimmedInDestination = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = screenAccent)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showCustomizeSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = screenAccent),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Salva impostazioni", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showDurationWheelPicker) {
        SamsungTimeWheelPickerDialog(
            initialTime = LocalTime.of(viewModel.laundryDurationHours.coerceIn(0, 23), viewModel.laundryDurationMinutes.coerceIn(0, 59)),
            title = "Imposta durata",
            onDismiss = { showDurationWheelPicker = false },
            onConfirm = { time ->
                viewModel.laundryDurationHours = time.hour
                viewModel.laundryDurationMinutes = time.minute
                showDurationWheelPicker = false
            }
        )
    }

    if (showTimeWheelPicker) {
        SamsungTimeWheelPickerDialog(
            initialTime = LocalTime.of(viewModel.laundryExactHour.coerceIn(0, 23), viewModel.laundryExactMinute.coerceIn(0, 59)),
            title = "Imposta orario",
            onDismiss = { showTimeWheelPicker = false },
            onConfirm = { time ->
                viewModel.laundryExactHour = time.hour
                viewModel.laundryExactMinute = time.minute
                showTimeWheelPicker = false
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.laundryExactDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.laundryExactDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = screenAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla", color = colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.dialogBackground)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// --- CARD PER LA SELEZIONE DEI CAPI NELLA VALIGIA / SPOSTAMENTO / PANNI ---

@Composable
fun SuitcaseClothingGridCard(
    item: ClothingItem,
    selectedQty: Int,
    itemAccentColor: Color = LocalOneUIColors.current.accent,
    onToggleSelection: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val isSelected = selectedQty > 0

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) itemAccentColor.copy(alpha = 0.08f) else colors.cardBackground,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) itemAccentColor else colors.cardBorder
        ),
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUri != null) {
                    Image(
                        painter = rememberOptimizedPainter(item.imageUri, 180),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!item.emoji.isNullOrBlank()) {
                    Text(normalizeEmoji(item.emoji) ?: item.emoji.orEmpty(), fontSize = 42.sp)
                } else {
                    Icon(Icons.Default.Checkroom, contentDescription = null, tint = itemAccentColor, modifier = Modifier.size(38.dp))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) itemAccentColor else (if (item.imageUri != null) Color.Black.copy(alpha = 0.45f) else colors.cardBackground))
                        .border(
                            1.5.dp,
                            if (isSelected) itemAccentColor else (if (item.imageUri != null) Color.White.copy(alpha = 0.9f) else colors.cardBorder),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                if (item.isCountable) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .clickable { onDecrement() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                                Text(
                                    text = "$selectedQty/${item.quantity}",
                                    color = itemAccentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .clickable { onIncrement() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            } else {
                                Text(
                                    text = "Max: ${item.quantity}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = if (item.tags.isNotEmpty()) item.tags.joinToString(" • ") { "#$it" } else "Nessun tag",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SuitcaseClothingListCard(
    item: ClothingItem,
    selectedQty: Int,
    itemAccentColor: Color = LocalOneUIColors.current.accent,
    onToggleSelection: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val isSelected = selectedQty > 0

    val subtitle = remember(item.folderName, item.tags) {
        val folderPrefix = if (item.folderName != null) "📁 ${item.folderName}" else null
        val tagsText = if (item.tags.isNotEmpty()) item.tags.joinToString(" ") { "#$it" } else null
        listOfNotNull(folderPrefix, tagsText).ifEmpty { listOf("Nessun tag") }.joinToString(" • ")
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) itemAccentColor.copy(alpha = 0.08f) else colors.cardBackground,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) itemAccentColor else colors.cardBorder
        ),
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) itemAccentColor else Color.Transparent)
                        .border(1.5.dp, if (isSelected) itemAccentColor else colors.textSecondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.imageUri != null) {
                        Image(
                            painter = rememberOptimizedPainter(item.imageUri, 110),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!item.emoji.isNullOrBlank()) {
                        Text(normalizeEmoji(item.emoji) ?: item.emoji.orEmpty(), fontSize = 28.sp)
                    } else {
                        Icon(Icons.Default.Checkroom, contentDescription = null, tint = itemAccentColor, modifier = Modifier.size(26.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (item.isCountable) {
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        if (isSelected) {
                            IconButton(
                                onClick = onDecrement,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text("-", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                            Text(
                                text = "$selectedQty/${item.quantity}",
                                color = itemAccentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            IconButton(
                                onClick = onIncrement,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text("+", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        } else {
                            Text(
                                text = "Max: ${item.quantity}",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
