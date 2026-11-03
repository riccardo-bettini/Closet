@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import com.example.closet.ui.reminder.ReminderItem
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Immutable
data class CalendarEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val isAllDay: Boolean = false,
    val startDate: LocalDate,
    val endDate: LocalDate = startDate,
    val startTime: String = "",
    val endTime: String = "",
    val color: Color = Color(0xFF388AF6),
    val deletedTimestamp: Long? = null
)

@Composable
fun CalendarScreen(
    viewModel: ClosetViewModel,
    todayClickTrigger: Long = 0L,
    showTrashSheetExternal: Boolean = false,
    onDismissTrashSheetExternal: () -> Unit = {},
    onSelectedDateChanged: (LocalDate) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val colors = LocalOneUIColors.current

    if (onBack != null) {
        BackHandler(enabled = true) {
            onBack()
        }
    }

    val eventsMap = viewModel.eventsMap
    val reminders = viewModel.reminders
    val trashEvents = viewModel.trashEvents

    var selectedDate by remember { mutableStateOf(today) }
    var isWeekViewOnly by remember { mutableStateOf(false) }
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var reminderToShowDetails by remember { mutableStateOf<ReminderItem?>(null) }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    val baseMonth = remember { YearMonth.now() }
    val initialMonthPage = 600
    val monthPagerState = rememberPagerState(initialPage = initialMonthPage, pageCount = { 1200 })

    val baseMonday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val initialWeekPage = 2500
    val weekPagerState = rememberPagerState(initialPage = initialWeekPage, pageCount = { 5000 })

    LaunchedEffect(selectedDate) {
        onSelectedDateChanged(selectedDate)
    }

    LaunchedEffect(todayClickTrigger) {
        if (todayClickTrigger > 0L) {
            selectedDate = today
            val monthDiff = (today.year - baseMonth.year) * 12 + (today.monthValue - baseMonth.monthValue)
            val selectedMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekDiff = java.time.temporal.ChronoUnit.WEEKS.between(baseMonday, selectedMonday).toInt()
            coroutineScope.launch {
                monthPagerState.animateScrollToPage(initialMonthPage + monthDiff)
                weekPagerState.animateScrollToPage(initialWeekPage + weekDiff)
            }
        }
    }

    val visibleYearMonth = remember(monthPagerState.currentPage) {
        baseMonth.plusMonths((monthPagerState.currentPage - initialMonthPage).toLong())
    }

    val headerMonthYear = remember(isWeekViewOnly, weekPagerState.currentPage, visibleYearMonth) {
        if (isWeekViewOnly) {
            val currentWeekMonday = baseMonday.plusWeeks((weekPagerState.currentPage - initialWeekPage).toLong())
            val midWeekDay = currentWeekMonday.plusDays(3)
            "${midWeekDay.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).uppercase()} ${midWeekDay.year}"
        } else {
            "${visibleYearMonth.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).uppercase()} ${visibleYearMonth.year}"
        }
    }

    val daysOfWeek = remember { listOf("LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM") }

    val calendarHeight by animateDpAsState(
        targetValue = if (isWeekViewOnly) 62.dp else 265.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "calendarHeight"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onBack != null) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceSecondary,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        tonalElevation = 4.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Torna alle funzioni",
                                tint = if (viewModel.useDistinctFunctionColors) Color(0xFF388AF6) else colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMonthYearPicker = true }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = headerMonthYear,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Seleziona mese e anno",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(110.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEachIndexed { index, day ->
                Text(
                    text = day,
                    color = if (index == 6) Color(0xFFE55353) else colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(calendarHeight)
        ) {
            if (isWeekViewOnly) {
                HorizontalPager(
                    state = weekPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val monday = remember(page) {
                        baseMonday.plusWeeks((page - initialWeekPage).toLong())
                    }
                    WeekRowView(
                        startOfWeek = monday,
                        selectedDate = selectedDate,
                        today = today,
                        eventsMap = eventsMap,
                        reminders = reminders.toList(),
                        onDateSelected = { date -> selectedDate = date }
                    )
                }
            } else {
                HorizontalPager(
                    state = monthPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageMonth = remember(page) {
                        baseMonth.plusMonths((page - initialMonthPage).toLong())
                    }
                    MonthGrid(
                        month = pageMonth,
                        selectedDate = selectedDate,
                        today = today,
                        eventsMap = eventsMap,
                        reminders = reminders.toList(),
                        onDateSelected = { date ->
                            selectedDate = date
                            val targetMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            val weekDiff = java.time.temporal.ChronoUnit.WEEKS.between(baseMonday, targetMonday).toInt()
                            coroutineScope.launch {
                                weekPagerState.scrollToPage(initialWeekPage + weekDiff)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val dayEvents = remember(eventsMap, selectedDate) {
            eventsMap[selectedDate] ?: emptyList()
        }
        val dayReminders = remember(reminders.toList(), selectedDate) {
            reminders.filter { r ->
                if (r.isRecurring) r.recurringDays.contains(selectedDate.dayOfWeek.value)
                else r.date != null && r.date == selectedDate
            }
        }
        val dayOfWeekDisplayName = remember(selectedDate) {
            selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).uppercase()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.cardBackground)
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta < -18 && !isWeekViewOnly) {
                            val selMonday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            val weekDiff = java.time.temporal.ChronoUnit.WEEKS.between(baseMonday, selMonday).toInt()
                            coroutineScope.launch {
                                weekPagerState.scrollToPage(initialWeekPage + weekDiff)
                            }
                            isWeekViewOnly = true
                        } else if (delta > 18 && isWeekViewOnly) {
                            val mDiff = (selectedDate.year - baseMonth.year) * 12 + (selectedDate.monthValue - baseMonth.monthValue)
                            coroutineScope.launch {
                                monthPagerState.scrollToPage(initialMonthPage + mDiff)
                            }
                            isWeekViewOnly = false
                        }
                    },
                    orientation = Orientation.Vertical
                )
                .padding(top = 10.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(colors.textSecondary.copy(alpha = 0.4f), CircleShape)
                    .clickable { isWeekViewOnly = !isWeekViewOnly }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${selectedDate.dayOfMonth} $dayOfWeekDisplayName",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    if (selectedDate == today) {
                        Text(
                            text = "OGGI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier
                                .background(colors.accentContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceSecondary)
                        .clickable {
                            eventToEdit = null
                            showAddOrEditDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aggiungi evento",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (dayEvents.isEmpty() && dayReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun impegno o promemoria registrato",
                        color = colors.textMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    if (dayEvents.isNotEmpty()) {
                        item(key = "header_events") {
                            Text(
                                text = "Impegni (${dayEvents.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                        }
                        items(dayEvents, key = { it.id }) { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceSecondary)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(event.color, CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = event.title,
                                            color = colors.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val timeString = when {
                                            event.isAllDay -> "Tutto il giorno"
                                            event.startTime.isNotBlank() && event.endTime.isNotBlank() -> "${event.startTime} - ${event.endTime}"
                                            event.startTime.isNotBlank() -> event.startTime
                                            else -> ""
                                        }
                                        if (timeString.isNotBlank()) {
                                            Text(
                                                text = timeString,
                                                color = colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        eventToEdit = event
                                        showAddOrEditDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Modifica",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (dayReminders.isNotEmpty()) {
                        item(key = "header_reminders") {
                            Text(
                                text = "Promemoria (${dayReminders.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = 4.dp, top = if (dayEvents.isNotEmpty()) 10.dp else 0.dp, bottom = 2.dp)
                            )
                        }
                        items(dayReminders, key = { "rem_${it.id}" }) { reminder ->
                            CalendarReminderCard(
                                reminder = reminder,
                                onToggle = { isEnabled ->
                                    viewModel.toggleReminder(reminder.id, isEnabled)
                                },
                                onClick = {
                                    reminderToShowDetails = reminder
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentYearMonth = visibleYearMonth,
            onDismiss = { showMonthYearPicker = false },
            onYearMonthSelected = { selectedYM ->
                val monthDiff = (selectedYM.year - baseMonth.year) * 12 + (selectedYM.monthValue - baseMonth.monthValue)
                coroutineScope.launch {
                    monthPagerState.scrollToPage(initialMonthPage + monthDiff)
                }
                showMonthYearPicker = false
            }
        )
    }

    if (showAddOrEditDialog) {
        SamsungEventDialog(
            selectedDate = selectedDate,
            initialEvent = eventToEdit,
            onDismiss = {
                showAddOrEditDialog = false
                eventToEdit = null
            },
            onSave = { updatedOrNewEvent ->
                viewModel.saveCalendarEvent(updatedOrNewEvent)
                showAddOrEditDialog = false
                eventToEdit = null
            },
            onDelete = { eventToDelete ->
                viewModel.deleteCalendarEvent(eventToDelete)
                showAddOrEditDialog = false
                eventToEdit = null
            }
        )
    }

    if (showTrashSheetExternal) {
        TrashBottomSheet(
            trashList = trashEvents,
            onDismiss = onDismissTrashSheetExternal,
            onRestore = { restoredEvent ->
                viewModel.restoreTrashEvent(restoredEvent)
            },
            onClearAll = {
                viewModel.clearTrash()
            }
        )
    }

    if (reminderToShowDetails != null) {
        ReminderDetailsDialog(
            reminder = reminderToShowDetails!!,
            onDismiss = { reminderToShowDetails = null }
        )
    }
}

@Composable
fun MonthYearPickerDialog(
    currentYearMonth: YearMonth,
    onDismiss: () -> Unit,
    onYearMonthSelected: (YearMonth) -> Unit
) {
    val colors = LocalOneUIColors.current
    var selectedYear by remember { mutableIntStateOf(currentYearMonth.year) }
    var selectedMonth by remember { mutableIntStateOf(currentYearMonth.monthValue) }

    val months = remember {
        (1..12).map { m ->
            YearMonth.of(2026, m).month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).uppercase()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { selectedYear-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anno precedente", tint = colors.textSecondary)
                }
                Text(
                    text = selectedYear.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                IconButton(onClick = { selectedYear++ }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Anno successivo", tint = colors.textSecondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until 3) {
                            val monthIndex = row * 3 + col + 1
                            val isSelected = (monthIndex == selectedMonth)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) colors.accent else colors.surfaceSecondary)
                                    .clickable { selectedMonth = monthIndex },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = months[monthIndex - 1],
                                    color = if (isSelected) Color.White else colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onYearMonthSelected(YearMonth.of(selectedYear, selectedMonth))
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = CircleShape
            ) {
                Text("Conferma", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun WeekRowView(
    startOfWeek: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsMap: Map<LocalDate, List<CalendarEvent>>,
    reminders: List<ReminderItem> = emptyList(),
    onDateSelected: (LocalDate) -> Unit
) {
    val colors = LocalOneUIColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val isSelected = date == selectedDate
            val isToday = date == today
            val events = eventsMap[date] ?: emptyList()
            val dayReminders = remember(reminders, date) {
                reminders.filter { r ->
                    if (r.isRecurring) r.recurringDays.contains(date.dayOfWeek.value)
                    else r.date != null && r.date == date
                }
            }
            val indicatorColors = remember(events, dayReminders) {
                val list = mutableListOf<Color>()
                events.forEach { list.add(it.color) }
                dayReminders.forEach { r ->
                    val visualColor = getFunctionVisualInfo(r.associatedFunction).third
                    list.add(if (visualColor != Color.Unspecified) visualColor else Color(0xFFF59E0B))
                }
                list
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            if (colors.isDark) Color(0xFF282C34) else Color(0xFFD6DBE5)
                        } else colors.surfaceSecondary.copy(alpha = 0.6f)
                    )
                    .border(
                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                        color = if (isToday && !isSelected) colors.accent else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = when {
                            isSelected -> colors.textPrimary
                            i == 6 -> Color(0xFFE55353)
                            else -> colors.textPrimary
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                    )

                    if (indicatorColors.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            indicatorColors.take(3).forEach { barColor ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(barColor, CircleShape)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsMap: Map<LocalDate, List<CalendarEvent>>,
    reminders: List<ReminderItem> = emptyList(),
    onDateSelected: (LocalDate) -> Unit
) {
    val colors = LocalOneUIColors.current
    val firstDayOfWeek = remember(month) { month.atDay(1).dayOfWeek.value }
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val totalCells = 35

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val rows = totalCells / 7
        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    val dayNumber = index - (firstDayOfWeek - 2)
                    val isValidDay = dayNumber in 1..daysInMonth
                    val thisDate = if (isValidDay) month.atDay(dayNumber) else null
                    val isSelected = thisDate != null && thisDate == selectedDate
                    val isToday = thisDate != null && thisDate == today
                    val eventsForDay = if (thisDate != null) eventsMap[thisDate] ?: emptyList() else emptyList()
                    val remindersForDay = if (thisDate != null) {
                        reminders.filter { r ->
                            if (r.isRecurring) r.recurringDays.contains(thisDate.dayOfWeek.value)
                            else r.date != null && r.date == thisDate
                        }
                    } else emptyList()
                    val indicatorColors = remember(eventsForDay, remindersForDay) {
                        val list = mutableListOf<Color>()
                        eventsForDay.forEach { list.add(it.color) }
                        remindersForDay.forEach { r ->
                            val visualColor = getFunctionVisualInfo(r.associatedFunction).third
                            list.add(if (visualColor != Color.Unspecified) visualColor else Color(0xFFF59E0B))
                        }
                        list
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> if (colors.isDark) Color(0xFF282C34) else Color(0xFFD6DBE5)
                                    isValidDay -> colors.surfaceSecondary.copy(alpha = 0.5f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (isToday && !isSelected) 1.dp else 0.dp,
                                color = if (isToday && !isSelected) colors.accent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = isValidDay) {
                                thisDate?.let { onDateSelected(it) }
                            }
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (isValidDay && thisDate != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    color = when {
                                        isSelected -> colors.textPrimary
                                        c == 6 -> Color(0xFFE55353)
                                        else -> colors.textPrimary
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )

                                if (indicatorColors.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        indicatorColors.take(3).forEach { barColor ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .background(barColor, CircleShape)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamsungEventDialog(
    selectedDate: LocalDate,
    initialEvent: CalendarEvent? = null,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit = {}
) {
    val colors = LocalOneUIColors.current
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var isAllDay by remember { mutableStateOf(initialEvent?.isAllDay ?: false) }

    var startDate by remember { mutableStateOf(initialEvent?.startDate ?: selectedDate) }
    var endDate by remember { mutableStateOf(initialEvent?.endDate ?: selectedDate) }

    val initialStartLocal = remember {
        if (!initialEvent?.startTime.isNullOrBlank()) {
            try { LocalTime.parse(initialEvent?.startTime) } catch (e: Exception) { LocalTime.of(18, 0) }
        } else LocalTime.of(18, 0)
    }

    val initialEndLocal = remember {
        if (!initialEvent?.endTime.isNullOrBlank()) {
            try { LocalTime.parse(initialEvent?.endTime) } catch (e: Exception) { LocalTime.of(19, 0) }
        } else LocalTime.of(19, 0)
    }

    var startTime by remember { mutableStateOf(initialStartLocal) }
    var endTime by remember { mutableStateOf(initialEndLocal) }

    val palette = remember {
        listOf(
            Color(0xFF388AF6), // Samsung Blue
            Color(0xFF10B981), // Emerald
            Color(0xFFEF4444), // Crimson Red
            Color(0xFFF59E0B), // Amber
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4)  // Cyan
        )
    }
    var selectedColor by remember { mutableStateOf(initialEvent?.color ?: palette[0]) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    fun formatDate(d: LocalDate): String {
        val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).replaceFirstChar { it.uppercase() }
        val mon = d.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).replaceFirstChar { it.uppercase() }
        return "$dow ${d.dayOfMonth} $mon"
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.dialogBackground,
                border = BorderStroke(0.5.dp, colors.cardBorder),
                tonalElevation = 8.dp,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 660.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Header popup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(selectedColor.copy(alpha = if (colors.isDark) 0.22f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = selectedColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = if (initialEvent != null) "Modifica evento" else "Nuovo evento",
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(colors.surfaceSecondary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colors.cardBorder)

                    // Contenuto a schede One UI scrollabile
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. TITOLO EVENTO
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(selectedColor)
                                )

                                BasicTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    textStyle = ComposeTextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    ),
                                    cursorBrush = SolidColor(selectedColor),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (title.isEmpty()) {
                                            Text(
                                                text = "Titolo evento",
                                                color = colors.textSecondary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 2. PALETTE COLORI ONE UI
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                palette.forEach { color ->
                                    val isSelected = selectedColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 0.dp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = color },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. DATA E ORARI (SCHEDA UNIFICATA ONE UI)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Switch "Tutto il giorno"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Tutto il giorno",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )

                                    Switch(
                                        checked = isAllDay,
                                        onCheckedChange = { isAllDay = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = selectedColor,
                                            uncheckedThumbColor = colors.textSecondary,
                                            uncheckedTrackColor = colors.cardBorder
                                        )
                                    )
                                }

                                HorizontalDivider(
                                    color = colors.cardBorder.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                // Riga Inizio
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Inizio",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textSecondary
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.dialogBackground,
                                            border = BorderStroke(1.dp, colors.cardBorder),
                                            modifier = Modifier.clickable { showStartDatePicker = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarToday,
                                                    contentDescription = null,
                                                    tint = selectedColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = formatDate(startDate),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }

                                        if (!isAllDay) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = colors.dialogBackground,
                                                border = BorderStroke(1.dp, colors.cardBorder),
                                                modifier = Modifier.clickable { showStartTimePicker = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Schedule,
                                                        contentDescription = null,
                                                        tint = selectedColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = startTime.format(timeFormatter),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color = colors.cardBorder.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                // Riga Fine
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Fine",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textSecondary
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.dialogBackground,
                                            border = BorderStroke(1.dp, colors.cardBorder),
                                            modifier = Modifier.clickable { showEndDatePicker = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarToday,
                                                    contentDescription = null,
                                                    tint = selectedColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = formatDate(endDate),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }

                                        if (!isAllDay) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = colors.dialogBackground,
                                                border = BorderStroke(1.dp, colors.cardBorder),
                                                modifier = Modifier.clickable { showEndTimePicker = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Schedule,
                                                        contentDescription = null,
                                                        tint = selectedColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = endTime.format(timeFormatter),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. ELIMINA EVENTO (SOLO SE MODIFICA)
                        if (initialEvent != null) {
                            Button(
                                onClick = { onDelete(initialEvent) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                                    contentColor = Color(0xFFEF4444)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Elimina evento",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // AZIONI POPUP: ANNULLA & SALVA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = colors.surfaceSecondary,
                                contentColor = colors.textSecondary
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
                                onSave(
                                    CalendarEvent(
                                        id = initialEvent?.id ?: java.util.UUID.randomUUID().toString(),
                                        title = title.ifBlank { "Nuovo evento" },
                                        isAllDay = isAllDay,
                                        startDate = startDate,
                                        endDate = endDate,
                                        startTime = if (isAllDay) "" else startTime.format(timeFormatter),
                                        endTime = if (isAllDay) "" else endTime.format(timeFormatter),
                                        color = selectedColor
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = selectedColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Salva",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Picker data inizio Material 3
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        startDate = selected
                        if (endDate.isBefore(selected)) endDate = selected
                    }
                    showStartDatePicker = false
                }) { Text("OK", color = selectedColor) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Annulla", color = colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.dialogBackground)
        ) { DatePicker(state = datePickerState) }
    }

    // Picker data fine Material 3
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        endDate = if (selected.isBefore(startDate)) startDate else selected
                    }
                    showEndDatePicker = false
                }) { Text("OK", color = selectedColor) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Annulla", color = colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.dialogBackground)
        ) { DatePicker(state = datePickerState) }
    }

    // Picker orario inizio a scorrimento stile Samsung Orologio
    if (showStartTimePicker) {
        SamsungTimeWheelPickerDialog(
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { newTime ->
                startTime = newTime
                if (startDate == endDate && endTime.isBefore(startTime)) {
                    endTime = startTime.plusHours(1)
                }
                showStartTimePicker = false
            }
        )
    }

    // Picker orario fine a scorrimento stile Samsung Orologio
    if (showEndTimePicker) {
        SamsungTimeWheelPickerDialog(
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { newTime ->
                endTime = newTime
                showEndTimePicker = false
            }
        )
    }
}

@Composable
fun SamsungEventBottomSheet(
    selectedDate: LocalDate,
    initialEvent: CalendarEvent? = null,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit = {}
) = SamsungEventDialog(selectedDate, initialEvent, onDismiss, onSave, onDelete)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBottomSheet(
    trashList: List<CalendarEvent>,
    onDismiss: () -> Unit,
    onRestore: (CalendarEvent) -> Unit,
    onClearAll: () -> Unit
) {
    val colors = LocalOneUIColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(colors.textSecondary.copy(alpha = 0.4f), CircleShape)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Cestino",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Elementi cancellati negli ultimi 30 giorni",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                if (trashList.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Svuota tutto", color = Color(0xFFE55353), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (trashList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Il cestino Ã¨ vuoto",
                        color = colors.textMuted,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trashList, key = { it.id }) { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surfaceSecondary)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(event.color, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = event.title,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${event.startDate.dayOfMonth} ${event.startDate.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN)}",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onRestore(event) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Restore,
                                    contentDescription = "Ripristina",
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// CARD PROMEMORIA NEL CALENDARIO (NON MODIFICABILE COME EVENTO)
// =========================================================================

@Composable
fun CalendarReminderCard(
    reminder: ReminderItem,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val visualInfo = remember(reminder.associatedFunction) {
        if (!reminder.associatedFunction.isNullOrBlank()) {
            getFunctionVisualInfo(reminder.associatedFunction)
        } else {
            Triple("Promemoria", Icons.Outlined.Notifications, Color(0xFFF59E0B))
        }
    }
    val icon = visualInfo.second
    val accentTint = visualInfo.third

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentTint.copy(alpha = if (colors.isDark) 0.20f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (reminder.isEnabled) accentTint else colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = reminder.title,
                            color = if (reminder.isEnabled) colors.textPrimary else colors.textMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = accentTint.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Promemoria",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentTint
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val timeStr = reminder.time.format(DateTimeFormatter.ofPattern("HH:mm"))
                        Text(
                            text = timeStr,
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )

                        if (reminder.isRecurring) {
                            Text(
                                text = "•",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                            Icon(
                                imageVector = Icons.Outlined.Repeat,
                                contentDescription = "Ricorrente",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = formatRecurringDays(reminder.recurringDays),
                                color = colors.textSecondary,
                                fontSize = 11.5.sp
                            )
                        } else if (!reminder.associatedFunction.isNullOrBlank()) {
                            Text(
                                text = "•",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = visualInfo.first,
                                color = colors.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentTint,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.surfaceSecondary
                ),
                modifier = Modifier.scale(0.80f)
            )
        }
    }
}

@Composable
fun ReminderDetailsDialog(
    reminder: ReminderItem,
    onDismiss: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val visualInfo = remember(reminder.associatedFunction) {
        if (!reminder.associatedFunction.isNullOrBlank()) {
            getFunctionVisualInfo(reminder.associatedFunction)
        } else {
            Triple("Promemoria", Icons.Outlined.Notifications, Color(0xFFF59E0B))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogBackground,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(visualInfo.third.copy(alpha = if (colors.isDark) 0.22f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visualInfo.second,
                        contentDescription = null,
                        tint = visualInfo.third,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = reminder.title,
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Promemoria (Non modificabile come evento)",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Orario", color = colors.textSecondary, fontSize = 13.sp)
                    Text(
                        reminder.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pianificazione", color = colors.textSecondary, fontSize = 13.sp)
                    Text(
                        if (reminder.isRecurring) "Ricorrente (${formatRecurringDays(reminder.recurringDays)})"
                        else reminder.date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Giornaliero",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (reminder.hasLeadTime) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Notifica anticipata", color = colors.textSecondary, fontSize = 13.sp)
                        Text(
                            "${reminder.leadTimeMinutes} min prima",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!reminder.associatedFunction.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Funzione collegata", color = colors.textSecondary, fontSize = 13.sp)
                        Text(
                            visualInfo.first,
                            color = visualInfo.third,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Stato", color = colors.textSecondary, fontSize = 13.sp)
                    Text(
                        if (reminder.isEnabled) "Attivo" else "Disattivato",
                        color = if (reminder.isEnabled) Color(0xFF10B981) else colors.textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = CircleShape
            ) {
                Text("Chiudi", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
