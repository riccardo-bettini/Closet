@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ClosetViewModel
import com.example.closet.ui.viewmodel.TripEndAction
import kotlinx.coroutines.delay
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
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCancelTripConfirmDialog by remember { mutableStateOf(false) }

    val selectedQuantities = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        if (viewModel.savedTripSelections.isNotEmpty()) {
            selectedQuantities.putAll(viewModel.savedTripSelections)
        }
    }

    BackHandler(enabled = true) {
        if (activeTrip != null) {
            onBack()
        } else if (selectedQuantities.isNotEmpty()) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    val allAvailableItems = remember(viewModel.clothingItems.toList(), selectedClosetFilter) {
        if (selectedClosetFilter == null || selectedClosetFilter == "Tutti gli armadi") {
            viewModel.clothingItems.toList()
        } else {
            viewModel.clothingItems.filter { it.closetName == selectedClosetFilter }
        }
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
        val handleBack: () -> Unit = {
            if (activeTrip != null) {
                onBack()
            } else if (selectedQuantities.isNotEmpty()) {
                showExitDialog = true
            } else {
                onBack()
            }
        }

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
                    Text(
                        text = if (activeTrip != null) "Viaggio in corso: ${activeTrip.name}" else "Prepara la valigia attingendo da qualsiasi armadio",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
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
                                            Text(tripItem.emoji, fontSize = 22.sp)
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
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
        ModalBottomSheet(
            onDismissRequest = { showCustomizeSheet = false },
            containerColor = colors.dialogBackground,
            dragHandle = {
                Box(modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp).background(colors.textSecondary.copy(alpha = 0.3f), CircleShape))
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
                Text("Personalizzazione Viaggio", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Imposta Durata Viaggio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("Al termine del viaggio i capi seguiranno la regola scelta sotto", fontSize = 11.sp, color = colors.textSecondary)
                    }
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
                                ) { Text("Giorno e Ora di Fine", fontSize = 11.sp, color = if (viewModel.tripUseExactEndTime) Color.White else colors.textPrimary) }
                            }

                            if (!viewModel.tripUseExactEndTime) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Durata (Ore):", fontSize = 13.sp, color = colors.textPrimary)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(onClick = { if (viewModel.tripDurationHours > 1) viewModel.tripDurationHours -= 6 }) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                                        Text("${viewModel.tripDurationHours} h", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                        IconButton(onClick = { viewModel.tripDurationHours += 6 }) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Giorno di rientro:", fontSize = 13.sp, color = colors.textPrimary)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(onClick = { viewModel.tripExactDate = viewModel.tripExactDate.minusDays(1) }) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                                            Text(viewModel.tripExactDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                            IconButton(onClick = { viewModel.tripExactDate = viewModel.tripExactDate.plusDays(1) }) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.cardBorder)

                Text("A fine viaggio:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripEndAction.entries.forEach { action ->
                        val isSel = (viewModel.tripEndAction == action)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) tripAccent.copy(alpha = 0.2f) else colors.surfaceSecondary,
                            border = BorderStroke(1.dp, if (isSel) tripAccent else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.tripEndAction = action }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = if (isSel) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
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
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Durata lavaggio:", fontSize = 12.sp, color = colors.textPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (viewModel.tripLaundryDurationHours > 1) viewModel.tripLaundryDurationHours-- }) { Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                                    Text("${viewModel.tripLaundryDurationHours}h", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                                    IconButton(onClick = { viewModel.tripLaundryDurationHours++ }) { Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showCustomizeSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = tripAccent),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) { Text("Salva impostazioni", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
