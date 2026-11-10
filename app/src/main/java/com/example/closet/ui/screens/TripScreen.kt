@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Work
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ClosetViewModel
import com.example.closet.ui.viewmodel.TripEndAction
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import java.util.Locale
import java.time.format.DateTimeFormatter

@Composable
fun TripScreen(
    viewModel: ClosetViewModel,
    onBack: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val closets = viewModel.availableClosets
    val activeTrip = viewModel.activeTrip
    val tripAccent = if (viewModel.useDistinctFunctionColors) Color(0xFF8B5CF6) else colors.accent

    var selectedClosetFilter by remember { mutableStateOf<String?>("Tutti gli armadi") }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showTripTimeWheelPicker by remember { mutableStateOf(false) }
    var showTripDurationWheelPicker by remember { mutableStateOf(false) }
    var showTripDatePicker by remember { mutableStateOf(false) }
    var showTripLaundryDurationWheelPicker by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCancelTripConfirmDialog by remember { mutableStateOf(false) }

    val selectedQuantities = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        if (viewModel.savedTripSelections.isNotEmpty()) {
            selectedQuantities.putAll(viewModel.savedTripSelections)
        }
    }

    val handleBack: () -> Unit = {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
            filterTags = emptySet()
        } else if (activeTrip != null) {
            onBack()
        } else if (selectedQuantities.isNotEmpty()) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    val allAvailableItems = remember(viewModel.clothingItems.toList(), selectedClosetFilter, searchQuery, filterTags, isSearchActive) {
        val baseItems = if (selectedClosetFilter == null || selectedClosetFilter == "Tutti gli armadi") {
            viewModel.clothingItems.toList()
        } else {
            viewModel.clothingItems.filter { it.closetName == selectedClosetFilter }
        }

        if (isSearchActive) {
            baseItems.filter { item ->
                val matchQuery = if (searchQuery.isBlank()) true else {
                    item.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    (item.folderName?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
                    item.tags.any { it.contains(searchQuery.trim(), ignoreCase = true) }
                }
                val matchTag = if (filterTags.isEmpty()) true else filterTags.all { fTag ->
                    item.tags.any { it.equals(fTag, ignoreCase = true) }
                }
                matchQuery && matchTag
            }
        } else {
            baseItems
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
        label = "tripTopFadeAlpha"
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(52.dp))

                Column {
                    Text(
                        text = "Viaggio",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                    if (activeTrip != null) {
                        Text(
                            text = "Viaggio in corso: ${activeTrip.name}",
                            fontSize = 12.sp,
                            color = colors.accent
                        )
                    }
                }
            }

            if (activeTrip != null) {
                var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        currentMillis = System.currentTimeMillis()
                        delay(1000L)
                    }
                }

                val remaining = if (activeTrip.endTimeMillis != null) {
                    (activeTrip.endTimeMillis - currentMillis).coerceAtLeast(0L)
                } else null

                val countdownStr = if (remaining != null) {
                    val hours = remaining / 3600000L
                    val minutes = (remaining % 3600000L) / 60000L
                    val seconds = (remaining % 60000L) / 1000L
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else "Tempo indeterminato"

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardBackground,
                    border = BorderStroke(1.dp, tripAccent.copy(alpha = 0.6f)),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = tripAccent.copy(alpha = 0.2f)) {
                                Text(
                                    text = "⏳ $countdownStr",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tripAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "${activeTrip.items.sumOf { it.quantity }} capi in valigia",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }

                        Text(
                            text = "Regola di fine viaggio:",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = when (activeTrip.endAction) {
                                TripEndAction.RETURN_TO_ORIGIN -> "I capi torneranno ciascuno nella posizione di partenza"
                                TripEndAction.GO_TO_CLOSET -> "I capi andranno in: ${activeTrip.targetCloset ?: "Armadio specifico"}"
                                TripEndAction.GO_TO_LAUNDRY -> "I capi entreranno direttamente in Panni (${activeTrip.laundryDurationHours}h ${activeTrip.laundryDurationMinutes}m)"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                Text(
                    text = "Capi presenti nella valigia:",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeTrip.items, key = { it.itemId }) { tripItem ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.cardBackground,
                            border = BorderStroke(0.5.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.surfaceSecondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (tripItem.imageUri != null) {
                                            Image(
                                                painter = rememberOptimizedPainter(tripItem.imageUri, 84),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else if (!tripItem.emoji.isNullOrBlank()) {
                                            Text(normalizeEmoji(tripItem.emoji) ?: tripItem.emoji.orEmpty(), fontSize = 22.sp)
                                        } else {
                                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = tripAccent, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Column {
                                        Text(tripItem.itemName, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                        Text("Da: ${tripItem.originalCloset}${if (tripItem.originalFolder != null) " • 📁 ${tripItem.originalFolder}" else ""}", fontSize = 11.5.sp, color = colors.textSecondary)
                                    }
                                }

                                if (tripItem.isCountable) {
                                    Text("x${tripItem.quantity}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf("Tutti gli armadi") + closets
                    filterOptions.forEach { cl ->
                        val isSel = (selectedClosetFilter == cl)
                        Surface(
                            shape = CircleShape,
                            color = if (isSel) tripAccent else colors.cardBackground,
                            border = BorderStroke(1.dp, if (isSel) tripAccent else colors.cardBorder),
                            modifier = Modifier.clickable { selectedClosetFilter = cl }
                        ) {
                            Text(
                                text = cl,
                                color = if (isSel) Color.White else colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (allAvailableItems.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    if (selectedQuantities.isNotEmpty()) {
                                        selectedQuantities.clear()
                                    } else {
                                        allAvailableItems.forEach { item ->
                                            selectedQuantities[item.id] = if (item.isCountable) item.quantity else 1
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedQuantities.isNotEmpty()) "Deseleziona tutti" else "Seleziona tutti",
                                    fontSize = 12.sp,
                                    color = tripAccent,
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
                                Icon(Icons.Default.GridView, contentDescription = "Griglia", tint = if (viewMode == ViewMode.GRID) colors.textPrimary else colors.textSecondary, modifier = Modifier.size(15.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (viewMode == ViewMode.LIST) (if (colors.isDark) Color(0xFF383C44) else Color(0xFFE4E7EE)) else Color.Transparent)
                                    .clickable { viewMode = ViewMode.LIST },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = "Elenco", tint = if (viewMode == ViewMode.LIST) colors.textPrimary else colors.textSecondary, modifier = Modifier.size(17.dp))
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
                            if (allAvailableItems.isEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    EmptyClosetState(isFolder = false, isSearch = isSearchActive)
                                }
                            } else {
                                items(allAvailableItems, key = { it.id }) { item ->
                                val selectedQty = selectedQuantities[item.id] ?: 0

                                SuitcaseClothingGridCard(
                                    item = item,
                                    selectedQty = selectedQty,
                                    itemAccentColor = tripAccent,
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
                            if (allAvailableItems.isEmpty()) {
                                item {
                                    EmptyClosetState(isFolder = false, isSearch = isSearchActive)
                                }
                            } else {
                                items(allAvailableItems, key = { it.id }) { item ->
                                val selectedQty = selectedQuantities[item.id] ?: 0

                                SuitcaseClothingListCard(
                                    item = item,
                                    selectedQty = selectedQty,
                                    itemAccentColor = tripAccent,
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
                    if (activeTrip != null) {
                        TextButton(onClick = { showCancelTripConfirmDialog = true }) {
                            Text("Annulla Viaggio", color = Color(0xFFE55353), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.finishTrip(activeTrip) },
                            colors = ButtonDefaults.buttonColors(containerColor = tripAccent),
                            shape = CircleShape,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Termina Viaggio", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
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
                                .background(if (isSearchActive) tripAccent else colors.surfaceSecondary)
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

                        // Pulsante Opzioni con icona Icons.Outlined.Tune
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
                                contentDescription = "Personalizza Viaggio",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        val canStart = totalSelectedPieces > 0
                        Button(
                            onClick = {
                                viewModel.startTrip(selectedQuantities.toMap())
                                selectedQuantities.clear()
                                onBack()
                            },
                            enabled = canStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tripAccent,
                                disabledContainerColor = tripAccent.copy(alpha = 0.35f)
                            ),
                            shape = CircleShape,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Work, contentDescription = null, tint = if (canStart) Color.White else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                Text("Parti", color = if (canStart) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
                                color = if (isSel) tripAccent else colors.islandBackground,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) tripAccent else colors.cardBorder
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
                    border = BorderStroke(1.dp, tripAccent.copy(alpha = 0.5f)),
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
                            tint = tripAccent,
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
                                cursorBrush = SolidColor(tripAccent),
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
                            colors = ButtonDefaults.buttonColors(containerColor = tripAccent),
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
                onClick = handleBack,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Torna alle funzioni",
                    tint = tripAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showCancelTripConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelTripConfirmDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Annullare il viaggio?", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Tutti i capi in valigia torneranno all'istante nei rispettivi armadi di provenienza.", color = colors.textSecondary, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelTrip()
                        showCancelTripConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) { Text("Annulla viaggio", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelTripConfirmDialog = false }) { Text("Continua viaggio", color = colors.textPrimary) }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Uscire da Viaggio?", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Puoi salvare la valigia preparata per continuare più tardi oppure annullare e scartare le selezioni.", color = colors.textSecondary, fontSize = 14.sp) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.savedTripSelections.clear()
                            viewModel.savedTripSelections.putAll(selectedQuantities)
                            showExitDialog = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tripAccent),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) { Text("Salva e continua dopo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            viewModel.savedTripSelections.clear()
                            selectedQuantities.clear()
                            showExitDialog = false
                            onBack()
                        }) { Text("Scarta modifiche", color = Color(0xFFE55353), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }

                        TextButton(onClick = { showExitDialog = false }) { Text("Continua", color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    }
                }
            },
            dismissButton = null
        )
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
                            text = "Personalizzazione Viaggio",
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
                        // Switch Imposta Durata Viaggio (senza descrizione)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Imposta Durata Viaggio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = viewModel.tripDurationEnabled,
                                onCheckedChange = { viewModel.tripDurationEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = tripAccent)
                            )
                        }

                        if (viewModel.tripDurationEnabled) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colors.surfaceSecondary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.tripUseExactEndTime = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (!viewModel.tripUseExactEndTime) tripAccent else Color.Transparent),
                                            shape = CircleShape,
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Per Ore", fontSize = 11.sp, color = if (!viewModel.tripUseExactEndTime) Color.White else colors.textPrimary) }

                                        Button(
                                            onClick = { viewModel.tripUseExactEndTime = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.tripUseExactEndTime) tripAccent else Color.Transparent),
                                            shape = CircleShape,
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Giorno e Ora", fontSize = 11.sp, color = if (viewModel.tripUseExactEndTime) Color.White else colors.textPrimary) }
                                    }

                                    if (!viewModel.tripUseExactEndTime) {
                                        // Selettore Durata (Ore) con wheel picker a scorrimento
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = colors.dialogBackground,
                                            border = BorderStroke(0.5.dp, colors.cardBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showTripDurationWheelPicker = true }
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
                                                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = tripAccent, modifier = Modifier.size(18.dp))
                                                    Text("Durata (Ore)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                                }
                                                Text("${viewModel.tripDurationHours} h", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Selettore Giorno di Ritorno
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = colors.dialogBackground,
                                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showTripDatePicker = true }
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
                                                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = tripAccent, modifier = Modifier.size(18.dp))
                                                        Text("Giorno di Ritorno", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                                    }
                                                    Text(viewModel.tripExactDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                                }
                                            }

                                            // Selettore Ora di Ritorno con wheel picker a scorrimento
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = colors.dialogBackground,
                                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showTripTimeWheelPicker = true }
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
                                                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = tripAccent, modifier = Modifier.size(18.dp))
                                                        Text("Ora di Ritorno", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                                    }
                                                    Text(
                                                        text = String.format(Locale.ROOT, "%02d:%02d", viewModel.tripExactHour, viewModel.tripExactMinute),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = tripAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Text("Azione al termine del viaggio:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TripEndAction.entries.forEach { action ->
                                val isSel = viewModel.tripEndAction == action
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) tripAccent.copy(alpha = 0.12f) else colors.surfaceSecondary,
                                    border = BorderStroke(1.dp, if (isSel) tripAccent else colors.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.tripEndAction = action }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSel) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSel) tripAccent else colors.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(action.label, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = colors.textPrimary)
                                    }
                                }
                            }
                        }

                        if (viewModel.tripEndAction == TripEndAction.GO_TO_CLOSET) {
                            var showClosetMenu by remember { mutableStateOf(false) }
                            val currentTarget = viewModel.tripTargetCloset ?: closets.firstOrNull() ?: "Armadio Principale"

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showClosetMenu = true }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Armadio di destinazione: $currentTarget", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.textSecondary)
                                    }
                                }
                                DropdownMenu(expanded = showClosetMenu, onDismissRequest = { showClosetMenu = false }) {
                                    closets.forEach { cl ->
                                        DropdownMenuItem(
                                            text = { Text(cl, color = colors.textPrimary) },
                                            onClick = {
                                                viewModel.tripTargetCloset = cl
                                                showClosetMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (viewModel.tripEndAction == TripEndAction.GO_TO_LAUNDRY) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceSecondary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Opzioni Panni preconfigurate:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.dialogBackground,
                                        border = BorderStroke(0.5.dp, colors.cardBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showTripLaundryDurationWheelPicker = true }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = tripAccent, modifier = Modifier.size(16.dp))
                                                Text("Durata lavaggio", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                            }
                                            Text("${viewModel.tripLaundryDurationHours} h", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showCustomizeSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = tripAccent),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) { Text("Salva impostazioni", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
            }
        }
    }

    if (showTripDurationWheelPicker) {
        SamsungTimeWheelPickerDialog(
            initialTime = LocalTime.of(viewModel.tripDurationHours.coerceIn(1, 23), 0),
            title = "Imposta durata (Ore)",
            onDismiss = { showTripDurationWheelPicker = false },
            onConfirm = { time ->
                viewModel.tripDurationHours = time.hour.coerceAtLeast(1)
                showTripDurationWheelPicker = false
            }
        )
    }

    if (showTripTimeWheelPicker) {
        SamsungTimeWheelPickerDialog(
            initialTime = LocalTime.of(viewModel.tripExactHour.coerceIn(0, 23), viewModel.tripExactMinute.coerceIn(0, 59)),
            title = "Imposta orario",
            onDismiss = { showTripTimeWheelPicker = false },
            onConfirm = { time ->
                viewModel.tripExactHour = time.hour
                viewModel.tripExactMinute = time.minute
                showTripTimeWheelPicker = false
            }
        )
    }

    if (showTripDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.tripExactDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showTripDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.tripExactDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showTripDatePicker = false
                }) { Text("OK", color = tripAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showTripDatePicker = false }) { Text("Annulla", color = colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.dialogBackground)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTripLaundryDurationWheelPicker) {
        SamsungTimeWheelPickerDialog(
            initialTime = LocalTime.of(viewModel.tripLaundryDurationHours.coerceIn(1, 23), 0),
            title = "Durata lavaggio (Ore)",
            onDismiss = { showTripLaundryDurationWheelPicker = false },
            onConfirm = { time ->
                viewModel.tripLaundryDurationHours = time.hour.coerceAtLeast(1)
                showTripLaundryDurationWheelPicker = false
            }
        )
    }
}
