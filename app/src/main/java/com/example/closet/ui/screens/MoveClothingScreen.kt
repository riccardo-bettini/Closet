@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ClosetViewModel
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

    var showCustomizeSheet by remember { mutableStateOf(false) }
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
        if (currentFolder != null) {
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

    val availableFoldersInSource = remember(viewModel.closetFolders.toList(), sourceCloset) {
        viewModel.getFoldersForCloset(sourceCloset)
    }

    val allItemsInSource = remember(viewModel.clothingItems.toList(), sourceCloset) {
        viewModel.clothingItems.filter { it.closetName == sourceCloset }
    }

    val visibleItemsInSource = remember(allItemsInSource, currentFolder) {
        allItemsInSource.filter { it.folderName == currentFolder }
    }

    val totalSelectedPieces = remember(selectedQuantities.toMap()) {
        selectedQuantities.values.sum()
    }

    val topFadeColor = if (colors.isDark) Color(0xFF0F1217) else Color(0xFFF0F2F6)
    val bottomFadeColor = colors.fadeColorBottom

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.width(52.dp))

                Text(
                    text = if (isLaundryMode) "Panni" else "Spostamento",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.cardBackground,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable { showSourceMenu = true }
                                .padding(start = 20.dp, end = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = sourceCloset,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false).basicMarquee()
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
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

                        Box(
                            modifier = Modifier
                                .size(30.dp)
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
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable { showDestMenu = true }
                                .padding(start = 6.dp, end = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = destCloset,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = screenAccent,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false).basicMarquee()
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = screenAccent, modifier = Modifier.size(16.dp))
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

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 4.dp, y = (-8).dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.cardBackground,
                        border = BorderStroke(0.8.dp, screenAccent.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "DA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = screenAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = (-8).dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.cardBackground,
                        border = BorderStroke(0.8.dp, screenAccent.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "A",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = screenAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
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
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (allItemsInSource.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedQuantities.isNotEmpty()) {
                                    selectedQuantities.clear()
                                } else {
                                    allItemsInSource.forEach { item ->
                                        selectedQuantities[item.id] = if (item.isCountable) item.quantity else 1
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (selectedQuantities.isNotEmpty()) "Deseleziona tutti" else "Seleziona tutti",
                                fontSize = 12.sp,
                                color = screenAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "• $totalSelectedPieces selezionati",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = colors.cardBackground,
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
                                .background(if (viewMode == ViewMode.GRID) (if (colors.isDark) Color(0xFF383C44) else Color(0xFFE4E7EE)) else Color.Transparent)
                                .clickable { viewMode = ViewMode.GRID },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Griglia",
                                tint = if (viewMode == ViewMode.GRID) colors.textPrimary else colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (viewMode == ViewMode.LIST) (if (colors.isDark) Color(0xFF383C44) else Color(0xFFE4E7EE)) else Color.Transparent)
                                .clickable { viewMode = ViewMode.LIST },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Elenco",
                                tint = if (viewMode == ViewMode.LIST) colors.textPrimary else colors.textSecondary,
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
                                EmptyClosetState(isFolder = currentFolder != null)
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
                                EmptyClosetState(isFolder = currentFolder != null)
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
                        .height(20.dp)
                        .background(Brush.verticalGradient(listOf(topFadeColor, topFadeColor.copy(alpha = 0.5f), Color.Transparent)))
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

        Surface(
            shape = CircleShape,
            color = colors.islandBackground,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp)
                .height(54.dp)
                .wrapContentWidth()
                .zIndex(30f)
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

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceSecondary)
                        .clickable { showCustomizeSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Impostazioni Tempo",
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (canMove) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
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
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = colors.dialogBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(colors.textSecondary.copy(alpha = 0.3f), CircleShape)
                )
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Cronologia Spostamenti (Ultimi 5)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
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
            }
        }
    }

    if (showCustomizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomizeSheet = false },
            containerColor = colors.dialogBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(colors.textSecondary.copy(alpha = 0.3f), CircleShape)
                )
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isLaundryMode) "Personalizzazione Panni" else "Personalizzazione Spostamento",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Imposta Durata Trasferimento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("I capi rimarranno in transito per il tempo prefissato", fontSize = 11.sp, color = colors.textSecondary)
                    }
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
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Ore:", fontSize = 13.sp, color = colors.textPrimary)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(onClick = { if (viewModel.laundryDurationHours > 0) viewModel.laundryDurationHours-- }) {
                                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            }
                                            Text("${viewModel.laundryDurationHours} h", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = screenAccent)
                                            IconButton(onClick = { viewModel.laundryDurationHours++ }) {
                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Minuti:", fontSize = 13.sp, color = colors.textPrimary)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(onClick = {
                                                if (viewModel.laundryDurationMinutes >= 5) viewModel.laundryDurationMinutes -= 5
                                            }) {
                                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            }
                                            Text("${viewModel.laundryDurationMinutes} min", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = screenAccent)
                                            IconButton(onClick = {
                                                if (viewModel.laundryDurationMinutes <= 50) viewModel.laundryDurationMinutes += 5
                                            }) {
                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Giorno:", fontSize = 13.sp, color = colors.textPrimary)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(onClick = {
                                                viewModel.laundryExactDate = viewModel.laundryExactDate.minusDays(1)
                                            }) {
                                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            }
                                            Text(
                                                text = viewModel.laundryExactDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = screenAccent
                                            )
                                            IconButton(onClick = {
                                                viewModel.laundryExactDate = viewModel.laundryExactDate.plusDays(1)
                                            }) {
                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Ora di Fine:", fontSize = 13.sp, color = colors.textPrimary)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(onClick = {
                                                viewModel.laundryExactHour = (viewModel.laundryExactHour - 1 + 24) % 24
                                            }) {
                                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            }
                                            Text(
                                                text = String.format("%02d:%02d", viewModel.laundryExactHour, viewModel.laundryExactMinute),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = screenAccent
                                            )
                                            IconButton(onClick = {
                                                viewModel.laundryExactHour = (viewModel.laundryExactHour + 1) % 24
                                            }) {
                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.cardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Visualizza capi nell'armadio di arrivo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("I capi in viaggio saranno visibili a destinazione ma oscurati", fontSize = 11.sp, color = colors.textSecondary)
                    }
                    Switch(
                        checked = viewModel.laundryShowDimmedInDestination,
                        onCheckedChange = { viewModel.laundryShowDimmedInDestination = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = screenAccent)
                    )
                }

                Button(
                    onClick = { showCustomizeSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = screenAccent),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Salva impostazioni", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
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
        color = colors.cardBackground,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 0.5.dp,
            color = if (isSelected) itemAccentColor else colors.cardBorder
        ),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
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
                        .background(if (isSelected) itemAccentColor else Color.Black.copy(alpha = 0.45f))
                        .border(1.5.dp, if (isSelected) itemAccentColor else Color.White, CircleShape),
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
        color = colors.cardBackground,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 0.5.dp,
            color = if (isSelected) itemAccentColor else colors.cardBorder
        ),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
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
