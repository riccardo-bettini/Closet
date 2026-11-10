package com.example.closet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ActiveLaundryTransfer
import com.example.closet.ui.viewmodel.ActiveTrip
import com.example.closet.ui.viewmodel.ClosetViewModel
import com.example.closet.ui.viewmodel.TransferHistoryItem
import com.example.closet.ui.viewmodel.TripEndAction
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class TransfersTab(val label: String) {
    ACTIVE("In corso"),
    HISTORY("Cronologia")
}

private enum class HistoryFilter(val label: String) {
    ALL("Tutti"),
    MOVE("Spostamenti"),
    LAUNDRY("Panni")
}

@Composable
fun TransfersActivityScreen(
    viewModel: ClosetViewModel,
    onBack: () -> Unit,
    onNavigateToLaundry: () -> Unit = {},
    onNavigateToMove: () -> Unit = {},
    onNavigateToTrip: () -> Unit = {}
) {
    val colors = LocalOneUIColors.current
    val screenAccent = if (viewModel.useDistinctFunctionColors) Color(0xFFF97316) else colors.accent

    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val activeTransfers = viewModel.activeLaundryTransfers
    val activeTrip = viewModel.activeTrip
    val totalActiveCount = activeTransfers.size + (if (activeTrip != null) 1 else 0)

    var selectedTab by remember { mutableStateOf(if (totalActiveCount > 0) TransfersTab.ACTIVE else TransfersTab.HISTORY) }
    var historyFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var historySearchQuery by remember { mutableStateOf("") }

    // Dialog state
    var transferToCancel by remember { mutableStateOf<ActiveLaundryTransfer?>(null) }
    var showCancelTripDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var selectedHistoryForDetail by remember { mutableStateOf<TransferHistoryItem?>(null) }
    var selectedActiveForDetail by remember { mutableStateOf<ActiveLaundryTransfer?>(null) }

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

    // Filtered history
    val filteredHistory = remember(viewModel.transferHistory.toList(), historyFilter, historySearchQuery) {
        viewModel.transferHistory.filter { record ->
            val isLaundryRecord = record.toCloset.equals("Panni", ignoreCase = true) || record.fromCloset.equals("Panni", ignoreCase = true)
            val matchFilter = when (historyFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.MOVE -> !isLaundryRecord
                HistoryFilter.LAUNDRY -> isLaundryRecord
            }
            val matchQuery = if (historySearchQuery.isBlank()) true else {
                val q = historySearchQuery.trim()
                record.fromCloset.contains(q, ignoreCase = true) ||
                        record.toCloset.contains(q, ignoreCase = true) ||
                        record.itemsMoved.any { it.itemName.contains(q, ignoreCase = true) }
            }
            matchFilter && matchQuery
        }
    }

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
            // Header Superiore One UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(52.dp))

                Text(
                    text = "Trasferimenti",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selettore Tab a Pillola (One UI Segmented Switcher)
            Surface(
                shape = CircleShape,
                color = colors.surfaceSecondary,
                border = BorderStroke(0.8.dp, colors.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransfersTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val badgeCount = if (tab == TransfersTab.ACTIVE) totalActiveCount else viewModel.transferHistory.size
                        val tabColor by animateColorAsState(
                            targetValue = if (isSelected) screenAccent else Color.Transparent,
                            animationSpec = tween(200),
                            label = "tabBg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else colors.textSecondary,
                            animationSpec = tween(200),
                            label = "tabText"
                        )

                        Surface(
                            shape = CircleShape,
                            color = tabColor,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color.White.copy(alpha = 0.28f) else colors.cardBorder,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$badgeCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else colors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contenuto principale a schede
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "transfersTabContent",
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                when (targetTab) {
                    TransfersTab.ACTIVE -> {
                        ActiveTransfersTabContent(
                            activeTrip = activeTrip,
                            activeTransfers = activeTransfers,
                            currentMillis = currentMillis,
                            colors = colors,
                            screenAccent = screenAccent,
                            onCompleteTransfer = { id -> viewModel.completeLaundryTransferNow(id) },
                            onRequestCancelTransfer = { transfer -> transferToCancel = transfer },
                            onFinishTrip = { trip -> viewModel.finishTrip(trip) },
                            onRequestCancelTrip = { showCancelTripDialog = true },
                            onOpenActiveDetail = { transfer -> selectedActiveForDetail = transfer },
                            onNavigateToMove = onNavigateToMove,
                            onNavigateToLaundry = onNavigateToLaundry,
                            onNavigateToTrip = onNavigateToTrip
                        )
                    }
                    TransfersTab.HISTORY -> {
                        HistoryTabContent(
                            history = filteredHistory,
                            totalHistoryCount = viewModel.transferHistory.size,
                            historyFilter = historyFilter,
                            onFilterChange = { historyFilter = it },
                            searchQuery = historySearchQuery,
                            onSearchQueryChange = { historySearchQuery = it },
                            colors = colors,
                            screenAccent = screenAccent,
                            onRequestClearAll = { showClearHistoryDialog = true },
                            onRemoveRecord = { id -> viewModel.removeTransferHistoryItem(id) },
                            onInvertRecord = { record ->
                                // Pre-seleziona i capi nel carrello di arrivo per invertirli
                                viewModel.savedLaundrySelections.clear()
                                record.itemsMoved.forEach { detail ->
                                    val found = viewModel.clothingItems.firstOrNull {
                                        (it.id == detail.itemId || it.name.equals(detail.itemName, ignoreCase = true)) &&
                                                it.closetName == record.toCloset
                                    }
                                    if (found != null) {
                                        viewModel.savedLaundrySelections[found.id] = detail.quantity.coerceAtMost(found.quantity)
                                    }
                                }
                                val isLaundryRecord = record.toCloset.equals("Panni", ignoreCase = true) || record.fromCloset.equals("Panni", ignoreCase = true)
                                if (isLaundryRecord) {
                                    onNavigateToLaundry()
                                } else {
                                    onNavigateToMove()
                                }
                            },
                            onOpenDetail = { record -> selectedHistoryForDetail = record }
                        )
                    }
                }
            }
        }

        // Dissolvenza inferiore morbida
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            bottomFadeColor.copy(alpha = 0.7f),
                            bottomFadeColor
                        )
                    )
                )
                .zIndex(10f)
        )

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
                onClick = onBack,
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

    // Dialog di conferma Annulla Trasferimento
    transferToCancel?.let { tr ->
        AlertDialog(
            onDismissRequest = { transferToCancel = null },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Annullare il trasferimento?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "I capi attualmente in transito torneranno immediatamente nell'armadio di partenza '${tr.fromCloset}'.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelLaundryTransfer(tr.id)
                        transferToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) {
                    Text("Annulla Trasferimento", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transferToCancel = null }) {
                    Text("Non annullare", color = colors.textPrimary)
                }
            }
        )
    }

    // Dialog di conferma Annulla Viaggio
    if (showCancelTripDialog) {
        AlertDialog(
            onDismissRequest = { showCancelTripDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Annullare il viaggio in corso?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tutti i capi in valigia torneranno nelle loro posizioni originali nei rispettivi armadi.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelTrip()
                        showCancelTripDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) {
                    Text("Annulla Viaggio", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelTripDialog = false }) {
                    Text("Mantieni", color = colors.textPrimary)
                }
            }
        )
    }

    // Dialog di conferma Svuota Cronologia
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Cancellare la cronologia?",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tutti i record dei trasferimenti passati verranno eliminati. Questa azione non influenza i capi attualmente presenti negli armadi.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearTransferHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                    shape = CircleShape
                ) {
                    Text("Svuota tutto", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Annulla", color = colors.textPrimary)
                }
            }
        )
    }

    // Modal Dettaglio Record Cronologia
    selectedHistoryForDetail?.let { record ->
        TransferHistoryDetailDialog(
            record = record,
            colors = colors,
            screenAccent = screenAccent,
            onDismiss = { selectedHistoryForDetail = null }
        )
    }

    // Modal Dettaglio Trasferimento Attivo
    selectedActiveForDetail?.let { active ->
        ActiveTransferDetailDialog(
            transfer = active,
            currentMillis = currentMillis,
            colors = colors,
            screenAccent = screenAccent,
            onDismiss = { selectedActiveForDetail = null },
            onComplete = {
                viewModel.completeLaundryTransferNow(active.id)
                selectedActiveForDetail = null
            },
            onCancel = {
                transferToCancel = active
                selectedActiveForDetail = null
            }
        )
    }
}

// =========================================================================
// TAB 1: TRASFERIMENTI IN CORSO
// =========================================================================

@Composable
private fun ActiveTransfersTabContent(
    activeTrip: ActiveTrip?,
    activeTransfers: List<ActiveLaundryTransfer>,
    currentMillis: Long,
    colors: OneUIColors,
    screenAccent: Color,
    onCompleteTransfer: (String) -> Unit,
    onRequestCancelTransfer: (ActiveLaundryTransfer) -> Unit,
    onFinishTrip: (ActiveTrip) -> Unit,
    onRequestCancelTrip: () -> Unit,
    onOpenActiveDetail: (ActiveLaundryTransfer) -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToLaundry: () -> Unit,
    onNavigateToTrip: () -> Unit
) {
    if (activeTrip == null && activeTransfers.isEmpty()) {
        // Empty State Elegante One UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = colors.cardBackground,
                border = BorderStroke(0.8.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(screenAccent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            tint = screenAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Nessun trasferimento in corso",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "I capi in viaggio, in lavaggio o in trasferimento temporaneo appariranno qui con il conto alla rovescia in tempo reale.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Avvia una funzione rapida:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = screenAccent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToMove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("Sposta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = onNavigateToLaundry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("Panni", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = onNavigateToTrip,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("Viaggio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 70.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Scheda Viaggio Attivo
            if (activeTrip != null) {
                item(key = "active_trip_${activeTrip.id}") {
                    ActiveTripCard(
                        trip = activeTrip,
                        currentMillis = currentMillis,
                        colors = colors,
                        onFinishTrip = { onFinishTrip(activeTrip) },
                        onCancelTrip = onRequestCancelTrip
                    )
                }
            }

            // Schede Trasferimenti (Panni / Spostamento)
            items(activeTransfers, key = { it.id }) { transfer ->
                ActiveTransferCard(
                    transfer = transfer,
                    currentMillis = currentMillis,
                    colors = colors,
                    onComplete = { onCompleteTransfer(transfer.id) },
                    onCancel = { onRequestCancelTransfer(transfer) },
                    onOpenDetail = { onOpenActiveDetail(transfer) }
                )
            }
        }
    }
}

@Composable
private fun ActiveTripCard(
    trip: ActiveTrip,
    currentMillis: Long,
    colors: OneUIColors,
    onFinishTrip: () -> Unit,
    onCancelTrip: () -> Unit
) {
    val tripAccent = Color(0xFF8B5CF6)
    val rem = if (trip.endTimeMillis != null) (trip.endTimeMillis - currentMillis).coerceAtLeast(0L) else null
    val countdownStr = if (rem != null) {
        val h = rem / 3600000L
        val m = (rem % 3600000L) / 60000L
        val s = (rem % 60000L) / 1000L
        String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s)
    } else "Tempo indeterminato"

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.2.dp, tripAccent.copy(alpha = 0.5f)),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(tripAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Work, contentDescription = null, tint = tripAccent, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = trip.name.ifBlank { "Viaggio Attivo" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Azione al rientro: ${trip.endAction.label}",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Countdown Pill
                Surface(
                    shape = CircleShape,
                    color = tripAccent.copy(alpha = 0.16f),
                    border = BorderStroke(0.8.dp, tripAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = tripAccent, modifier = Modifier.size(13.dp))
                        Text(
                            text = countdownStr,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = tripAccent
                        )
                    }
                }
            }

            // Visualizzatore Percorso
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceSecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dagli Armadi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = tripAccent, modifier = Modifier.size(16.dp))
                    Text("In Valigia", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tripAccent)
                }
            }

            // Capi in valigia
            val totalPieces = trip.items.sumOf { it.quantity }
            val itemPreview = trip.items.take(4).joinToString(", ") {
                if (it.quantity > 1) "${it.quantity}x ${it.itemName}" else it.itemName
            } + if (trip.items.size > 4) " (+ altri ${trip.items.size - 4})" else ""

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Capi in valigia ($totalPieces):",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                Text(
                    text = itemPreview,
                    fontSize = 12.sp,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Pulsanti Azione
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelTrip,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE55353)),
                    border = BorderStroke(1.dp, Color(0xFFE55353).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Annulla", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onFinishTrip,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = tripAccent),
                    modifier = Modifier.weight(1.3f).height(38.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Termina Viaggio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ActiveTransferCard(
    transfer: ActiveLaundryTransfer,
    currentMillis: Long,
    colors: OneUIColors,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val isLaundry = transfer.toCloset.equals("Panni", ignoreCase = true) || transfer.fromCloset.equals("Panni", ignoreCase = true)
    val cardAccent = if (isLaundry) Color(0xFFEF4444) else Color(0xFF10B981)
    val cardTitle = if (isLaundry) "Lavaggio in corso" else "Spostamento in transito"
    val cardIcon: ImageVector = if (isLaundry) Icons.Outlined.LocalLaundryService else Icons.Outlined.SyncAlt

    val rem = (transfer.endTimeMillis - currentMillis).coerceAtLeast(0L)
    val h = rem / 3600000L
    val m = (rem % 3600000L) / 60000L
    val s = (rem % 60000L) / 1000L
    val countdownStr = if (h > 0) {
        String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", m, s)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.2.dp, cardAccent.copy(alpha = 0.5f)),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(cardAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(cardIcon, contentDescription = null, tint = cardAccent, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = cardTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (transfer.isVisibleDimmed) "Visibile a destinazione oscurato" else "In transito",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                // Countdown Pill
                Surface(
                    shape = CircleShape,
                    color = cardAccent.copy(alpha = 0.16f),
                    border = BorderStroke(0.8.dp, cardAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = cardAccent, modifier = Modifier.size(13.dp))
                        Text(
                            text = countdownStr,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = cardAccent
                        )
                    }
                }
            }

            // Visualizzatore Percorso
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceSecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DA:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                        Text(transfer.fromCloset, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = cardAccent, modifier = Modifier.size(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("A:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                        Text(transfer.toCloset, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cardAccent)
                    }
                }
            }

            // Capi in transito
            val totalPieces = transfer.itemSelections.values.sum().coerceAtLeast(transfer.destinationItemIds.size)
            Text(
                text = "$totalPieces capi in transito (Tocca la scheda per i dettagli)",
                fontSize = 11.5.sp,
                color = colors.textSecondary
            )

            // Pulsanti Azione
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE55353)),
                    border = BorderStroke(1.dp, Color(0xFFE55353).copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Annulla", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onComplete,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = cardAccent),
                    modifier = Modifier.weight(1.3f).height(38.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Completa Ora", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: CRONOLOGIA TRASFERIMENTI
// =========================================================================

@Composable
private fun HistoryTabContent(
    history: List<TransferHistoryItem>,
    totalHistoryCount: Int,
    historyFilter: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    colors: OneUIColors,
    screenAccent: Color,
    onRequestClearAll: () -> Unit,
    onRemoveRecord: (String) -> Unit,
    onInvertRecord: (TransferHistoryItem) -> Unit,
    onOpenDetail: (TransferHistoryItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Barra di ricerca rapida nella cronologia
        Surface(
            shape = CircleShape,
            color = colors.surfaceSecondary,
            border = BorderStroke(0.8.dp, colors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = screenAccent, modifier = Modifier.size(18.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Cerca per armadio o capo...", fontSize = 12.5.sp, color = colors.textSecondary)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = ComposeTextStyle(color = colors.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(screenAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancella", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Filtri per tipologia & Pulsante Svuota
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HistoryFilter.entries.forEach { f ->
                    val isSel = historyFilter == f
                    Surface(
                        shape = CircleShape,
                        color = if (isSel) screenAccent else colors.surfaceSecondary,
                        border = BorderStroke(0.8.dp, if (isSel) screenAccent else colors.cardBorder),
                        modifier = Modifier.clickable { onFilterChange(f) }
                    ) {
                        Text(
                            text = f.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (totalHistoryCount > 0) {
                TextButton(
                    onClick = onRequestClearAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFE55353), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Svuota", fontSize = 11.5.sp, color = Color(0xFFE55353), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardBackground,
                    border = BorderStroke(0.8.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(36.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Nessun risultato trovato" else "Cronologia vuota",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Prova a modificare i termini di ricerca o il filtro selezionato." else "I trasferimenti completati o invertiti appariranno qui automaticamente.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 70.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { record ->
                    val isLaundryRecord = record.toCloset.equals("Panni", ignoreCase = true) || record.fromCloset.equals("Panni", ignoreCase = true)
                    val recordAccent = if (isLaundryRecord) Color(0xFFEF4444) else Color(0xFF10B981)

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(0.8.dp, colors.cardBorder),
                        tonalElevation = 2.dp,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDetail(record) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Intestazione record con data e chip tipo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = recordAccent.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLaundryRecord) Icons.Outlined.LocalLaundryService else Icons.Outlined.SyncAlt,
                                            contentDescription = null,
                                            tint = recordAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (isLaundryRecord) "Panni" else "Spostamento",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = recordAccent
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = record.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                    IconButton(
                                        onClick = { onRemoveRecord(record.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Elimina record",
                                            tint = colors.textSecondary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Percorso
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(8.dp), color = colors.surfaceSecondary) {
                                    Text(
                                        text = record.fromCloset,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = recordAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Surface(shape = RoundedCornerShape(8.dp), color = recordAccent.copy(alpha = 0.14f)) {
                                    Text(
                                        text = record.toCloset,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = recordAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Capi spostati
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

                            // Tasto Inverti
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { onInvertRecord(record) },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = recordAccent),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isLaundryRecord) "Inverti in Panni" else "Inverti Spostamento",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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

// =========================================================================
// DIALOG DI DETTAGLIO
// =========================================================================

@Composable
private fun TransferHistoryDetailDialog(
    record: TransferHistoryItem,
    colors: OneUIColors,
    screenAccent: Color,
    onDismiss: () -> Unit
) {
    val isLaundryRecord = record.toCloset.equals("Panni", ignoreCase = true) || record.fromCloset.equals("Panni", ignoreCase = true)
    val recordAccent = if (isLaundryRecord) Color(0xFFEF4444) else Color(0xFF10B981)

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
                .heightIn(max = 620.dp)
                .padding(vertical = 16.dp)
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
                    Column {
                        Text(
                            text = if (isLaundryRecord) "Dettagli Panni" else "Dettagli Spostamento",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = record.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'alle' HH:mm")),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = colors.textSecondary)
                    }
                }

                // Percorso
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceSecondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Partenza", fontSize = 11.sp, color = colors.textSecondary)
                            Text(record.fromCloset, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = recordAccent, modifier = Modifier.size(20.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Destinazione", fontSize = 11.sp, color = colors.textSecondary)
                            Text(record.toCloset, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = recordAccent)
                        }
                    }
                }

                Text(
                    text = "Capi trasferiti (${record.itemsMoved.sumOf { it.quantity }} pezzi):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                // Elenco capi
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(record.itemsMoved) { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cardBackground,
                            border = BorderStroke(0.8.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.itemName,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = recordAccent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "${item.quantity} pz",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = recordAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = screenAccent),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Chiudi", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActiveTransferDetailDialog(
    transfer: ActiveLaundryTransfer,
    currentMillis: Long,
    colors: OneUIColors,
    screenAccent: Color,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val isLaundry = transfer.toCloset.equals("Panni", ignoreCase = true) || transfer.fromCloset.equals("Panni", ignoreCase = true)
    val cardAccent = if (isLaundry) Color(0xFFEF4444) else Color(0xFF10B981)

    val rem = (transfer.endTimeMillis - currentMillis).coerceAtLeast(0L)
    val h = rem / 3600000L
    val m = (rem % 3600000L) / 60000L
    val s = (rem % 60000L) / 1000L
    val countdownStr = if (h > 0) String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%02d:%02d", m, s)

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
                    Column {
                        Text(
                            text = if (isLaundry) "Dettaglio Lavaggio" else "Dettaglio Spostamento",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Tempo rimanente: $countdownStr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = cardAccent
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = colors.textSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceSecondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Da", fontSize = 11.sp, color = colors.textSecondary)
                            Text(transfer.fromCloset, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = cardAccent, modifier = Modifier.size(20.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("A", fontSize = 11.sp, color = colors.textSecondary)
                            Text(transfer.toCloset, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cardAccent)
                        }
                    }
                }

                val totalPieces = transfer.itemSelections.values.sum().coerceAtLeast(transfer.destinationItemIds.size)
                Text(
                    text = "Totale pezzi in transito: $totalPieces",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE55353)),
                        border = BorderStroke(1.dp, Color(0xFFE55353).copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("Annulla", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onComplete,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = cardAccent),
                        modifier = Modifier.weight(1.3f).height(42.dp)
                    ) {
                        Text("Completa Ora", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
