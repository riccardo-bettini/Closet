@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.closet.ui.reminder.ReminderItem
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// =========================================================================
// POPUP LISTA PROMEMORIA (REMINDERS)
// =========================================================================

@Composable
fun ReminderListDialog(
    viewModel: ClosetViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalOneUIColors.current
    var editingReminder by remember { mutableStateOf<ReminderItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.dialogBackground,
            border = BorderStroke(1.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Tasto indietro a sinistra, Titolo al centro, Tasto + a destra
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "Promemoria",
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            val defaultTime = LocalTime.now().plusMinutes(5).withSecond(0).withNano(0)
                            editingReminder = ReminderItem(
                                title = "",
                                date = LocalDate.now(),
                                time = defaultTime
                            )
                            isCreatingNew = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nuovo promemoria",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(bottom = 12.dp))

                // Contenuto: Lista dei promemoria impostati
                if (viewModel.reminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentContainer.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "Nessun promemoria",
                                color = colors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tocca il tasto + in alto a destra\nper impostarne uno.",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.reminders, key = { it.id }) { reminder ->
                            ReminderCardItem(
                                reminder = reminder,
                                onToggle = { enabled ->
                                    viewModel.toggleReminder(reminder.id, enabled)
                                },
                                onClick = {
                                    editingReminder = reminder
                                    isCreatingNew = false
                                },
                                onDelete = {
                                    viewModel.deleteReminder(reminder.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog di modifica / creazione
    editingReminder?.let { rem ->
        ReminderEditDialog(
            reminder = rem,
            isNew = isCreatingNew,
            onDismiss = { editingReminder = null },
            onSave = { updated ->
                if (isCreatingNew) {
                    viewModel.addReminder(updated)
                } else {
                    viewModel.updateReminder(updated)
                }
                editingReminder = null
            },
            onDelete = { id ->
                viewModel.deleteReminder(id)
                editingReminder = null
            }
        )
    }
}

// =========================================================================
// CARD PROMEMORIA NELLA LISTA
// =========================================================================

@Composable
fun ReminderCardItem(
    reminder: ReminderItem,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (colors.isDark) Color(0xFF1E232E) else Color(0xFFF3F5FA),
        border = BorderStroke(0.5.dp, colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = reminder.title.ifBlank { "Promemoria" },
                        color = if (reminder.isEnabled) colors.textPrimary else colors.textSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Dettagli data / orario
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = reminder.time.format(timeFormatter),
                        color = if (reminder.isEnabled) colors.accent else colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "•",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )

                    if (!reminder.isRecurring) {
                        val dateText = reminder.date?.let { d ->
                            when (d) {
                                LocalDate.now() -> "Oggi"
                                LocalDate.now().plusDays(1) -> "Domani"
                                else -> d.format(DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN))
                            }
                        } ?: "Oggi"
                        Text(
                            text = dateText,
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    } else {
                        val daysLabel = formatRecurringDays(reminder.recurringDays)
                        Text(
                            text = daysLabel,
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Chip funzionali (Preavviso, Funzione collegata)
                if (reminder.hasLeadTime || !reminder.associatedFunction.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (reminder.hasLeadTime) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.accentContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${reminder.leadTimeMinutes}m prima",
                                        color = colors.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        reminder.associatedFunction?.let { fnKey ->
                            val (fnLabel, fnIcon, fnColor) = getFunctionVisualInfo(fnKey)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = fnColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, fnColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = fnIcon,
                                        contentDescription = null,
                                        tint = fnColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = fnLabel,
                                        color = fnColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Elimina",
                        tint = colors.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceSecondary
                    ),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

// =========================================================================
// DIALOG CREAZIONE / MODIFICA PROMEMORIA
// =========================================================================

@Composable
fun ReminderEditDialog(
    reminder: ReminderItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (ReminderItem) -> Unit,
    onDelete: (String) -> Unit
) {
    val colors = LocalOneUIColors.current

    var title by remember { mutableStateOf(reminder.title) }
    var isRecurring by remember { mutableStateOf(reminder.isRecurring) }
    var selectedDate by remember { mutableStateOf(reminder.date ?: LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(reminder.time) }
    var recurringDays by remember { mutableStateOf(reminder.recurringDays) }
    var hasLeadTime by remember { mutableStateOf(reminder.hasLeadTime) }
    var leadTimeMinutes by remember { mutableStateOf(reminder.leadTimeMinutes) }
    var isCustomLeadTime by remember {
        mutableStateOf(reminder.hasLeadTime && reminder.leadTimeMinutes !in listOf(5, 10, 15, 30, 60))
    }
    var customLeadMinutesText by remember {
        mutableStateOf(if (isCustomLeadTime) reminder.leadTimeMinutes.toString() else "45")
    }
    var leadTimeSound by remember { mutableStateOf(reminder.leadTimeSound) }
    var leadTimeVibration by remember { mutableStateOf(reminder.leadTimeVibration) }
    var associatedFunction by remember { mutableStateOf(reminder.associatedFunction) }
    var deleteOnCompletion by remember { mutableStateOf(reminder.deleteOnCompletion) }

    // Dialog native / personalizzate per Data e Ora
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimeWheelPicker by remember { mutableStateOf(false) }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.dialogBackground,
            border = BorderStroke(1.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Back a sinistra, Titolo, Salva a destra
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Annulla",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = if (isNew) "Nuovo promemoria" else "Modifica promemoria",
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            val finalLead = if (isCustomLeadTime) {
                                customLeadMinutesText.toIntOrNull()?.coerceAtLeast(1) ?: 15
                            } else leadTimeMinutes

                            val updated = reminder.copy(
                                title = title.ifBlank { "Promemoria" },
                                isRecurring = isRecurring,
                                date = if (isRecurring) null else selectedDate,
                                time = selectedTime,
                                recurringDays = if (isRecurring) {
                                    if (recurringDays.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else recurringDays
                                } else emptySet(),
                                hasLeadTime = hasLeadTime,
                                leadTimeMinutes = finalLead,
                                leadTimeSound = leadTimeSound,
                                leadTimeVibration = leadTimeVibration,
                                associatedFunction = associatedFunction,
                                deleteOnCompletion = if (!isRecurring) deleteOnCompletion else false,
                                isEnabled = true
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                    ) {
                        Text(
                            text = "Salva",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = colors.cardBorder)

                // Corpo scorrevole delle impostazioni
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. NOME NOTIFICA
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Nome notifica",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Es. Fare il bucato, ritirare scarpe...", color = colors.textSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.cardBorder,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedContainerColor = colors.surfaceSecondary,
                                unfocusedContainerColor = colors.surfaceSecondary
                            )
                        )
                    }

                    // 2. SLIDER A 2 POSIZIONI: SINGOLO GIORNO vs RIPETITIVO
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Frequenza",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        OneUISegmentedSlider(
                            options = listOf("Singolo giorno", "Ripetitivo"),
                            selectedIndex = if (!isRecurring) 0 else 1,
                            onSelectIndex = { index ->
                                isRecurring = (index == 1)
                            }
                        )

                        // Sotto-opzione se SINGOLO GIORNO: Selettore Data
                        AnimatedVisibility(visible = !isRecurring) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isToday = selectedDate == LocalDate.now()
                                    val isTomorrow = selectedDate == LocalDate.now().plusDays(1)

                                    FilterChip(
                                        selected = isToday,
                                        onClick = { selectedDate = LocalDate.now() },
                                        label = { Text("Oggi") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.accentContainer,
                                            selectedLabelColor = colors.accent
                                        )
                                    )
                                    FilterChip(
                                        selected = isTomorrow,
                                        onClick = { selectedDate = LocalDate.now().plusDays(1) },
                                        label = { Text("Domani") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.accentContainer,
                                            selectedLabelColor = colors.accent
                                        )
                                    )
                                    FilterChip(
                                        selected = !isToday && !isTomorrow,
                                        onClick = { showDatePicker = true },
                                        label = {
                                            Text(
                                                if (!isToday && !isTomorrow)
                                                    selectedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN))
                                                else "Altro giorno..."
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.CalendarMonth,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.accentContainer,
                                            selectedLabelColor = colors.accent
                                        )
                                    )
                                }
                            }
                        }

                        // Sotto-opzione se RIPETITIVO: Selettore giorni della settimana (L, M, M, G, V, S, D)
                        AnimatedVisibility(visible = isRecurring) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val daysData = listOf(
                                        1 to "L",
                                        2 to "M",
                                        3 to "M",
                                        4 to "G",
                                        5 to "V",
                                        6 to "S",
                                        7 to "D"
                                    )
                                    daysData.forEach { (dayInt, label) ->
                                        val isSelected = recurringDays.contains(dayInt)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) colors.accent else colors.surfaceSecondary)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) colors.accent else colors.cardBorder,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    recurringDays = if (isSelected) {
                                                        recurringDays - dayInt
                                                    } else {
                                                        recurringDays + dayInt
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else colors.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. ORARIO (APRE IL SELETTORE A SCORRIMENTO STILE SAMSUNG)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Orario notifica",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimeWheelPicker = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = selectedTime.format(timeFormatter),
                                        color = colors.textPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "Cambia orario",
                                    color = colors.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 4. PREAVVISO
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceSecondary,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = if (hasLeadTime) colors.accent else colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Preavviso anticipato",
                                        color = colors.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Switch(
                                    checked = hasLeadTime,
                                    onCheckedChange = { hasLeadTime = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.accent
                                    )
                                )
                            }

                            AnimatedVisibility(visible = hasLeadTime) {
                                Column(
                                    modifier = Modifier.padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(color = colors.cardBorder)

                                    // Chip preset (5 min, 10 min, 15 min, 30 min, 1 ora, Personalizzato)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val presets = listOf(
                                            5 to "5 min",
                                            10 to "10 min",
                                            15 to "15 min",
                                            30 to "30 min",
                                            60 to "1 ora"
                                        )
                                        presets.forEach { (mins, lbl) ->
                                            val isSelected = !isCustomLeadTime && leadTimeMinutes == mins
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    isCustomLeadTime = false
                                                    leadTimeMinutes = mins
                                                },
                                                label = { Text(lbl) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = colors.accentContainer,
                                                    selectedLabelColor = colors.accent
                                                )
                                            )
                                        }

                                        FilterChip(
                                            selected = isCustomLeadTime,
                                            onClick = { isCustomLeadTime = true },
                                            label = { Text("Personalizzato") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colors.accentContainer,
                                                selectedLabelColor = colors.accent
                                            )
                                        )
                                    }

                                    // Campo numerico personalizzato se attivo
                                    if (isCustomLeadTime) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = customLeadMinutesText,
                                                onValueChange = { customLeadMinutesText = it.filter { ch -> ch.isDigit() } },
                                                label = { Text("Minuti di preavviso") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.width(160.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            Text(
                                                text = "minuti prima",
                                                color = colors.textSecondary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    // Switch per Suono e Vibrazione preavviso
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Suono notifica preavviso",
                                            color = colors.textPrimary,
                                            fontSize = 13.sp
                                        )
                                        Switch(
                                            checked = leadTimeSound,
                                            onCheckedChange = { leadTimeSound = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = colors.accent
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Vibrazione preavviso",
                                            color = colors.textPrimary,
                                            fontSize = 13.sp
                                        )
                                        Switch(
                                            checked = leadTimeVibration,
                                            onCheckedChange = { leadTimeVibration = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = colors.accent
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. AUTO-ELIMINAZIONE (SOTTO AL PREAVVISO E SOLO PER SINGOLO GIORNO)
                    AnimatedVisibility(visible = !isRecurring) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Elimina dopo il completamento",
                                    color = colors.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Switch(
                                    checked = deleteOnCompletion,
                                    onCheckedChange = { deleteOnCompletion = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.accent
                                    )
                                )
                            }
                        }
                    }

                    // 6. ASSEGNAZIONE FUNZIONE SPECIFICA (IN FONDO, SENZA DESCRIZIONE E CON COLORI VIVIDI)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Funzione associata",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        val functionList = listOf(
                            null to ("Nessuna (Home)" to Icons.Outlined.Home to Color(0xFF64748B)),
                            "WARDROBE" to ("Armadio" to Icons.Outlined.Checkroom to Color(0xFF14B8A6)),
                            "CALENDAR" to ("Calendario" to Icons.Outlined.CalendarMonth to Color(0xFF388AF6)),
                            "CLOSET_PHOTOS" to ("Foto armadio" to Icons.Outlined.CameraAlt to Color(0xFF53B5A2)),
                            "MOVE" to ("Spostamento" to Icons.Outlined.SyncAlt to Color(0xFF10B981)),
                            "LAUNDRY" to ("Panni" to Icons.Outlined.LocalLaundryService to Color(0xFFEF4444)),
                            "TRIP" to ("Viaggio" to Icons.Outlined.Work to Color(0xFF8B5CF6)),
                            "TRANSFERS_ACTIVITY" to ("Trasferimenti" to Icons.Outlined.SwapHoriz to Color(0xFF6366F1)),
                            "TOOLS" to ("Strumenti" to Icons.Outlined.Build to Color(0xFFF59E0B)),
                            "SETTINGS" to ("Impostazioni" to Icons.Outlined.Settings to Color(0xFF475569))
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            functionList.forEach { (key, info) ->
                                val (pair, fnColor) = info
                                val (name, icon) = pair
                                val isSelected = associatedFunction == key

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) fnColor.copy(alpha = 0.22f) else colors.surfaceSecondary,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.8.dp else 0.8.dp,
                                        color = if (isSelected) fnColor else colors.cardBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { associatedFunction = key }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = fnColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = name,
                                            color = if (isSelected) fnColor else colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. PULSANTE ELIMINA (se in modifica)
                    if (!isNew) {
                        Button(
                            onClick = { onDelete(reminder.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                contentColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Elimina promemoria",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Picker data Material 3
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla", color = colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.dialogBackground)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Picker orario a scorrimento stile Samsung Orologio
    if (showTimeWheelPicker) {
        SamsungTimeWheelPickerDialog(
            initialTime = selectedTime,
            onDismiss = { showTimeWheelPicker = false },
            onConfirm = { newTime ->
                selectedTime = newTime
                showTimeWheelPicker = false
            }
        )
    }
}

// =========================================================================
// SELETTORE ORARIO A SCORRIMENTO STILE SAMSUNG OROLOGIO (WHEEL PICKER)
// =========================================================================

@Composable
fun SamsungTimeWheelPickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val colors = LocalOneUIColors.current
    var selectedHour by remember { mutableIntStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.dialogBackground,
            border = BorderStroke(1.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Imposta orario",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                val itemHeight = 52.dp
                val containerHeight = 168.dp

                // Contenitore ruote con capsula centrale One UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(containerHeight),
                    contentAlignment = Alignment.Center
                ) {
                    // Capsula centrale di selezione (evidenziazione unificata stile Samsung Clock)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.accent.copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, colors.accent.copy(alpha = 0.40f)),
                        modifier = Modifier
                            .width(224.dp)
                            .height(itemHeight)
                    ) {}

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SamsungWheelColumn(
                            maxValue = 24,
                            initialValue = initialTime.hour,
                            itemHeight = itemHeight,
                            containerHeight = containerHeight,
                            onValueChange = { selectedHour = it }
                        )

                        Text(
                            text = ":",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .offset(y = (-1).dp)
                        )

                        SamsungWheelColumn(
                            maxValue = 60,
                            initialValue = initialTime.minute,
                            itemHeight = itemHeight,
                            containerHeight = containerHeight,
                            onValueChange = { selectedMinute = it }
                        )
                    }
                }

                // Pulsanti azione One UI simmetrici
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceSecondary,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Text(
                            text = "Annulla",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            onConfirm(LocalTime.of(selectedHour, selectedMinute))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Fatto",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SamsungWheelColumn(
    maxValue: Int,
    initialValue: Int,
    itemHeight: Dp,
    containerHeight: Dp,
    onValueChange: (Int) -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val animatable = remember { Animatable(initialValue.toFloat()) }

    // Notifica il valore corrente selezionato
    LaunchedEffect(animatable.value) {
        val centerIndex = animatable.value.roundToInt()
        val normalized = ((centerIndex % maxValue) + maxValue) % maxValue
        onValueChange(normalized)
    }

    val currentValue = animatable.value
    val centerIndex = currentValue.roundToInt()

    Box(
        modifier = Modifier
            .width(84.dp)
            .height(containerHeight)
            .clipToBounds()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { deltaPx ->
                    coroutineScope.launch {
                        animatable.snapTo(animatable.value - deltaPx / itemHeightPx)
                    }
                },
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        // Inerzia fluida: proietta il target in base alla velocità
                        val momentumOffset = -(velocity / itemHeightPx) * 0.16f
                        val projected = (animatable.value + momentumOffset).roundToInt()
                        animatable.animateTo(
                            targetValue = projected.toFloat(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialVelocity = -velocity / itemHeightPx
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Disegna 5 elementi attorno al centro (-2, -1, 0, 1, 2)
        for (step in -2..2) {
            val itemIndex = centerIndex + step
            val displayValue = ((itemIndex % maxValue) + maxValue) % maxValue
            val diffFromCenter = itemIndex - currentValue
            val yOffsetPx = diffFromCenter * itemHeightPx

            val dist = kotlin.math.abs(diffFromCenter)
            if (dist <= 2.2f) {
                val scale = (1.08f - dist * 0.22f).coerceIn(0.72f, 1.08f)
                val alpha = (1f - dist * 0.62f).coerceIn(0.10f, 1f)
                val isSelected = dist < 0.5f

                // Inclinazione prospettica 3D stile tamburo/rotella Samsung Clock
                val rotationX = (-diffFromCenter * 22f).coerceIn(-50f, 50f)

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(0, yOffsetPx.roundToInt()) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            this.rotationX = rotationX
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (step != 0) {
                                coroutineScope.launch {
                                    animatable.animateTo(
                                        targetValue = itemIndex.toFloat(),
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", displayValue),
                        fontSize = if (isSelected) 32.sp else 22.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.accent else colors.textSecondary
                    )
                }
            }
        }
    }
}

// =========================================================================
// HELPER FUNZIONI E LABELS
// =========================================================================

fun formatRecurringDays(days: Set<Int>): String {
    if (days.isEmpty() || days.size == 7) return "Tutti i giorni"
    val dayNames = mapOf(
        1 to "Lun",
        2 to "Mar",
        3 to "Mer",
        4 to "Gio",
        5 to "Ven",
        6 to "Sab",
        7 to "Dom"
    )
    return days.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
}

fun getFunctionVisualInfo(functionKey: String?): Triple<String, ImageVector, Color> {
    return when (functionKey) {
        "WARDROBE" -> Triple("Armadio", Icons.Outlined.Checkroom, Color(0xFF14B8A6))
        "CALENDAR" -> Triple("Calendario", Icons.Outlined.CalendarMonth, Color(0xFF388AF6))
        "CLOSET_PHOTOS" -> Triple("Foto armadio", Icons.Outlined.CameraAlt, Color(0xFF53B5A2))
        "MOVE" -> Triple("Spostamento", Icons.Outlined.SyncAlt, Color(0xFF10B981))
        "LAUNDRY" -> Triple("Panni", Icons.Outlined.LocalLaundryService, Color(0xFFEF4444))
        "TRIP" -> Triple("Viaggio", Icons.Outlined.Work, Color(0xFF8B5CF6))
        "TRANSFERS_ACTIVITY" -> Triple("Trasferimenti", Icons.Outlined.SwapHoriz, Color(0xFF6366F1))
        "TOOLS" -> Triple("Strumenti", Icons.Outlined.Build, Color(0xFFF59E0B))
        "SETTINGS" -> Triple("Impostazioni", Icons.Outlined.Settings, Color(0xFF475569))
        else -> Triple("Nessuna", Icons.Outlined.Home, Color(0xFF64748B))
    }
}
