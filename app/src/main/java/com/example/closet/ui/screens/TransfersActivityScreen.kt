package com.example.closet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

@Composable
fun TransfersActivityScreen(
    viewModel: ClosetViewModel,
    onBack: () -> Unit,
    onNavigateToLaundry: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val bottomFadeColor = colors.fadeColorBottom

    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val activeTransfers = viewModel.activeLaundryTransfers
    val activeTrip = viewModel.activeTrip

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
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(52.dp))

                Text(
                    text = "Trasferimenti",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "In corso (${activeTransfers.size + if (activeTrip != null) 1 else 0})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }

                if (activeTrip != null) {
                    item {
                        val rem = if (activeTrip.endTimeMillis != null) (activeTrip.endTimeMillis - currentMillis).coerceAtLeast(0L) else null
                        val countdownStr = if (rem != null) {
                            val h = rem / 3600000L
                            val m = (rem % 3600000L) / 60000L
                            val s = (rem % 60000L) / 1000L
                            String.format("%02d:%02d:%02d", h, m, s)
                        } else "Indeterminato"

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.cardBackground,
                            border = BorderStroke(1.dp, colors.accent),
                            tonalElevation = 3.dp,
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = colors.accentContainer) {
                                        Text("Viaggio: $countdownStr", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    TextButton(onClick = { viewModel.finishTrip(activeTrip) }) {
                                        Text("Termina ora", color = colors.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Viaggio: ${activeTrip.name} (${activeTrip.items.sumOf { it.quantity }} capi)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Text("Regola: ${activeTrip.endAction.label}", fontSize = 11.5.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }

                if (activeTransfers.isEmpty() && activeTrip == null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.cardBackground,
                            border = BorderStroke(0.5.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Nessun trasferimento attualmente in corso.",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(activeTransfers, key = { it.id }) { transfer ->
                        val remaining = (transfer.endTimeMillis - currentMillis).coerceAtLeast(0L)
                        val hours = remaining / 3600000L
                        val minutes = (remaining % 3600000L) / 60000L
                        val seconds = (remaining % 60000L) / 1000L
                        val countdownStr = if (hours > 0) {
                            String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format("%02d:%02d", minutes, seconds)
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.cardBackground,
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
                            tonalElevation = 3.dp,
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.accentContainer
                                    ) {
                                        Text(
                                            text = if (remaining > 0) "⏳ $countdownStr" else "✓ Finito",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { viewModel.completeLaundryTransferNow(transfer.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Termina ora", color = colors.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(
                                            onClick = { viewModel.cancelLaundryTransfer(transfer.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Annulla", color = Color(0xFFE55353), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(transfer.fromCloset, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                    Text(transfer.toCloset, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                val itemsCount = transfer.destinationItemIds.size
                                Text(
                                    text = "$itemsCount capi trasferiti • ${if (transfer.isVisibleDimmed) "Visibili oscurati a destinazione" else "Nascosti fino al termine"}",
                                    color = colors.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                item {
                    Text(
                        text = "Cronologia trasferimenti",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                if (viewModel.transferHistory.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.cardBackground,
                            border = BorderStroke(0.5.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Nessun trasferimento precedente registrato.", color = colors.textMuted, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(viewModel.transferHistory, key = { it.id }) { record ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.cardBackground,
                            border = BorderStroke(0.5.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSuitcaseClosets(record.toCloset, record.fromCloset)
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
                                    onNavigateToLaundry()
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
                                        color = colors.accent
                                    )
                                    Text(
                                        text = "Inverti in Panni",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(record.fromCloset, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp)
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
        val screenAccent = if (viewModel.useDistinctFunctionColors) Color(0xFFF97316) else colors.accent
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
}
