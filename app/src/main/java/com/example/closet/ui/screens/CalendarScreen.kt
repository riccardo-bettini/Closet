@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.closet.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
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
data class SchoolLesson(
    val id: String = java.util.UUID.randomUUID().toString(),
    val subject: String,
    val room: String = "",
    val dayOfWeek: DayOfWeek,
    val startTime: String,
    val endTime: String,
    val color: Color = Color(0xFF388AF6)
)

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
    var isDayEventsPanelExpanded by remember { mutableStateOf(false) }
    var showSchoolScheduleDialog by remember { mutableStateOf(false) }
    var showAddOrEditLessonDialog by remember { mutableStateOf(false) }
    var lessonToEdit by remember { mutableStateOf<SchoolLesson?>(null) }
    var selectedDayForSchedule by remember { mutableStateOf(DayOfWeek.MONDAY) }

    val baseMonth = remember { YearMonth.now() }
    val initialMonthPage = 600
    val monthPagerState = rememberPagerState(initialPage = initialMonthPage, pageCount = { 1200 })
    val verticalMonthPagerState = rememberPagerState(initialPage = initialMonthPage, pageCount = { 1200 })

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
                verticalMonthPagerState.animateScrollToPage(initialMonthPage + monthDiff)
                weekPagerState.animateScrollToPage(initialWeekPage + weekDiff)
            }
        }
    }

    val visibleYearMonth = remember(isDayEventsPanelExpanded, monthPagerState.currentPage, verticalMonthPagerState.currentPage) {
        val page = if (!isDayEventsPanelExpanded) verticalMonthPagerState.currentPage else monthPagerState.currentPage
        baseMonth.plusMonths((page - initialMonthPage).toLong())
    }

    val headerMonthYear = remember(isDayEventsPanelExpanded, isWeekViewOnly, weekPagerState.currentPage, visibleYearMonth) {
        if (isDayEventsPanelExpanded && isWeekViewOnly) {
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

            // Toggle visibilità orario lezioni
            Surface(
                shape = CircleShape,
                color = if (viewModel.showSchoolLessonsOnCalendar) {
                    if (viewModel.useDistinctFunctionColors) Color(0xFF388AF6) else colors.accent
                } else colors.surfaceSecondary,
                border = BorderStroke(0.5.dp, colors.cardBorder),
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = { viewModel.toggleSchoolLessonsVisibility() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = if (viewModel.showSchoolLessonsOnCalendar) "Nascondi lezioni" else "Mostra lezioni",
                        tint = if (viewModel.showSchoolLessonsOnCalendar) Color.White else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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

        val dayEvents = remember(eventsMap, selectedDate) {
            eventsMap[selectedDate] ?: emptyList()
        }
        val dayReminders = remember(reminders.toList(), selectedDate) {
            reminders.filter { r ->
                if (r.isRecurring) r.recurringDays.contains(selectedDate.dayOfWeek.value)
                else r.date != null && r.date == selectedDate
            }
        }
        val dayLessons = remember(viewModel.schoolLessons.toList(), viewModel.schoolScheduleStartDate, viewModel.schoolScheduleEndDate, viewModel.showSchoolLessonsOnCalendar, selectedDate) {
            if (viewModel.showSchoolLessonsOnCalendar) {
                viewModel.getLessonsForDate(selectedDate)
            } else {
                emptyList()
            }
        }
        val dayOfWeekDisplayName = remember(selectedDate) {
            selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).uppercase()
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val totalHeight = maxHeight
            val totalWidth = maxWidth

            val targetCalHeight = when {
                !isDayEventsPanelExpanded -> (totalHeight - 64.dp).coerceAtLeast(100.dp)
                isWeekViewOnly -> 62.dp
                else -> 265.dp
            }
            val animatedCalHeight by animateDpAsState(
                targetValue = targetCalHeight,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                label = "animatedCalHeight"
            )

            val targetPanelHeight = when {
                !isDayEventsPanelExpanded -> 54.dp
                isWeekViewOnly -> (totalHeight - 62.dp - 12.dp).coerceAtLeast(180.dp)
                else -> (totalHeight - 265.dp - 12.dp).coerceAtLeast(180.dp)
            }
            val animatedPanelHeight by animateDpAsState(
                targetValue = targetPanelHeight,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                label = "animatedPanelHeight"
            )

            val animatedPanelWidth by animateDpAsState(
                targetValue = if (isDayEventsPanelExpanded) totalWidth else 190.dp,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                label = "animatedPanelWidth"
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Area Calendario
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animatedCalHeight)
                ) {
                    if (!isDayEventsPanelExpanded) {
                        VerticalPager(
                            state = verticalMonthPagerState,
                            modifier = Modifier.fillMaxSize(),
                            pageSpacing = 16.dp
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
                                lessonsForDateProvider = { d ->
                                    if (viewModel.showSchoolLessonsOnCalendar) viewModel.getLessonsForDate(d)
                                    else emptyList()
                                },
                                onDateSelected = { date ->
                                    selectedDate = date
                                    val targetMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                    val weekDiff = java.time.temporal.ChronoUnit.WEEKS.between(baseMonday, targetMonday).toInt()
                                    coroutineScope.launch {
                                        weekPagerState.scrollToPage(initialWeekPage + weekDiff)
                                        monthPagerState.scrollToPage(verticalMonthPagerState.currentPage)
                                    }
                                }
                            )
                        }
                    } else if (isWeekViewOnly) {
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
                                lessonsForDateProvider = { d ->
                                    if (viewModel.showSchoolLessonsOnCalendar) viewModel.getLessonsForDate(d)
                                    else emptyList()
                                },
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
                                lessonsForDateProvider = { d ->
                                    if (viewModel.showSchoolLessonsOnCalendar) viewModel.getLessonsForDate(d)
                                    else emptyList()
                                },
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

                // Riga Inferiore: Pillola/Pannello Morfabile + Tasto Orario
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Surface(
                        modifier = Modifier
                            .width(animatedPanelWidth)
                            .height(animatedPanelHeight)
                            .then(
                                if (!isDayEventsPanelExpanded) {
                                    Modifier
                                        .draggable(
                                            state = rememberDraggableState { delta ->
                                                if (delta < -8) {
                                                    coroutineScope.launch {
                                                        monthPagerState.scrollToPage(verticalMonthPagerState.currentPage)
                                                    }
                                                    isDayEventsPanelExpanded = true
                                                }
                                            },
                                            orientation = Orientation.Vertical
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                monthPagerState.scrollToPage(verticalMonthPagerState.currentPage)
                                            }
                                            isDayEventsPanelExpanded = true
                                        }
                                } else Modifier
                            ),
                        shape = RoundedCornerShape(28.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(0.5.dp, colors.cardBorder),
                        tonalElevation = 6.dp,
                        shadowElevation = 10.dp
                    ) {
                        if (!isDayEventsPanelExpanded) {
                            // Layout Pillola
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(3.5.dp)
                                        .background(colors.textSecondary.copy(alpha = 0.45f), CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).replaceFirstChar { it.uppercase() }}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        } else {
                            // Layout Pannello Eventi Espanso
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 10.dp, start = 18.dp, end = 18.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                // Maniglia trascinamento superiore
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = 10.dp)
                                        .width(36.dp)
                                        .height(4.dp)
                                        .background(colors.textSecondary.copy(alpha = 0.4f), CircleShape)
                                        .draggable(
                                            state = rememberDraggableState { delta ->
                                                if (delta > 15) {
                                                    if (isWeekViewOnly) {
                                                        isWeekViewOnly = false
                                                    } else {
                                                        coroutineScope.launch {
                                                            verticalMonthPagerState.scrollToPage(monthPagerState.currentPage)
                                                        }
                                                        isDayEventsPanelExpanded = false
                                                    }
                                                } else if (delta < -15) {
                                                    if (!isWeekViewOnly) {
                                                        val selMonday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                                        val weekDiff = java.time.temporal.ChronoUnit.WEEKS.between(baseMonday, selMonday).toInt()
                                                        coroutineScope.launch {
                                                            weekPagerState.scrollToPage(initialWeekPage + weekDiff)
                                                        }
                                                        isWeekViewOnly = true
                                                    }
                                                }
                                            },
                                            orientation = Orientation.Vertical
                                        )
                                        .clickable {
                                            if (isWeekViewOnly) {
                                                isWeekViewOnly = false
                                            } else {
                                                coroutineScope.launch {
                                                    verticalMonthPagerState.scrollToPage(monthPagerState.currentPage)
                                                }
                                                isDayEventsPanelExpanded = false
                                            }
                                        }
                                )

                                // Riga Intestazione Giorno
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .draggable(
                                            state = rememberDraggableState { delta ->
                                                if (delta > 20) {
                                                    if (isWeekViewOnly) {
                                                        isWeekViewOnly = false
                                                    } else {
                                                        coroutineScope.launch {
                                                            verticalMonthPagerState.scrollToPage(monthPagerState.currentPage)
                                                        }
                                                        isDayEventsPanelExpanded = false
                                                    }
                                                }
                                            },
                                            orientation = Orientation.Vertical
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "${selectedDate.dayOfMonth} $dayOfWeekDisplayName",
                                            fontSize = 20.sp,
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

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(colors.surfaceSecondary)
                                                .clickable { showSchoolScheduleDialog = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = "Orario lezioni",
                                                tint = if (viewModel.useDistinctFunctionColors) Color(0xFF388AF6) else colors.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
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
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val eventListState = rememberLazyListState()
                                val topFadeAlpha by animateFloatAsState(
                                    targetValue = if (eventListState.firstVisibleItemIndex > 0 || eventListState.firstVisibleItemScrollOffset > 10) 1f else 0f,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "eventsTopFade"
                                )
                                val bottomFadeAlpha by animateFloatAsState(
                                    targetValue = if (eventListState.canScrollForward) 1f else 0f,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "eventsBottomFade"
                                )

                                if (dayLessons.isEmpty() && dayEvents.isEmpty() && dayReminders.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Nessun evento, promemoria o lezione",
                                            color = colors.textMuted,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        LazyColumn(
                                            state = eventListState,
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                                        ) {
                                            if (dayLessons.isNotEmpty()) {
                                                item(key = "header_lessons") {
                                                    Text(
                                                        text = "Lezioni (${dayLessons.size})",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textSecondary,
                                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                                    )
                                                }
                                                items(dayLessons, key = { "lesson_${it.id}" }) { lesson ->
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
                                                                    .background(lesson.color, CircleShape)
                                                            )
                                                            Column {
                                                                Text(
                                                                    text = lesson.subject,
                                                                    color = colors.textPrimary,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "${lesson.startTime} - ${lesson.endTime}",
                                                                        color = colors.textSecondary,
                                                                        fontSize = 12.sp
                                                                    )
                                                                    if (lesson.room.isNotBlank()) {
                                                                        Text(
                                                                            text = "• ${lesson.room}",
                                                                            color = colors.textSecondary,
                                                                            fontSize = 12.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                lessonToEdit = lesson
                                                                selectedDayForSchedule = lesson.dayOfWeek
                                                                showAddOrEditLessonDialog = true
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Outlined.Edit,
                                                                contentDescription = "Modifica lezione",
                                                                tint = colors.textSecondary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (dayEvents.isNotEmpty()) {
                                                item(key = "header_events") {
                                                    Text(
                                                        text = "Impegni (${dayEvents.size})",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textSecondary,
                                                        modifier = Modifier.padding(start = 4.dp, top = if (dayLessons.isNotEmpty()) 8.dp else 0.dp, bottom = 2.dp)
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
                                                                modifier = Modifier.size(18.dp)
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
                                                        modifier = Modifier.padding(start = 4.dp, top = if (dayEvents.isNotEmpty() || dayLessons.isNotEmpty()) 8.dp else 0.dp, bottom = 2.dp)
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

                                        // Sfumatura superiore morbida
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .fillMaxWidth()
                                                .height(24.dp)
                                                .graphicsLayer { alpha = topFadeAlpha }
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(colors.cardBackground, Color.Transparent)
                                                    )
                                                )
                                        )

                                        // Sfumatura inferiore morbida
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(28.dp)
                                                .graphicsLayer { alpha = bottomFadeAlpha }
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, colors.cardBackground)
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!isDayEventsPanelExpanded) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = colors.cardBackground,
                            border = BorderStroke(0.5.dp, colors.cardBorder),
                            tonalElevation = 6.dp,
                            shadowElevation = 10.dp,
                            modifier = Modifier.size(54.dp)
                        ) {
                            IconButton(
                                onClick = { showSchoolScheduleDialog = true },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Orario settimanale",
                                    tint = if (viewModel.useDistinctFunctionColors) Color(0xFF388AF6) else colors.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
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
                    verticalMonthPagerState.scrollToPage(initialMonthPage + monthDiff)
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

    if (showSchoolScheduleDialog) {
        SchoolScheduleDialog(
            viewModel = viewModel,
            selectedDay = selectedDayForSchedule,
            onDaySelected = { selectedDayForSchedule = it },
            onDismiss = { showSchoolScheduleDialog = false },
            onAddLessonClick = {
                lessonToEdit = null
                showAddOrEditLessonDialog = true
            },
            onEditLessonClick = { lesson ->
                lessonToEdit = lesson
                selectedDayForSchedule = lesson.dayOfWeek
                showAddOrEditLessonDialog = true
            }
        )
    }

    if (showAddOrEditLessonDialog) {
        AddOrEditSchoolLessonDialog(
            initialLesson = lessonToEdit,
            defaultDayOfWeek = selectedDayForSchedule,
            onDismiss = {
                showAddOrEditLessonDialog = false
                lessonToEdit = null
            },
            onSave = { lesson ->
                if (lessonToEdit == null) {
                    viewModel.addSchoolLesson(lesson)
                } else {
                    viewModel.updateSchoolLesson(lesson)
                }
                showAddOrEditLessonDialog = false
                lessonToEdit = null
            },
            onDelete = { lessonId ->
                viewModel.deleteSchoolLesson(lessonId)
                showAddOrEditLessonDialog = false
                lessonToEdit = null
            }
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
    lessonsForDateProvider: (LocalDate) -> List<SchoolLesson> = { emptyList() },
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
            val lessonsForDay = lessonsForDateProvider(date)
            val indicatorColors = remember(events, dayReminders, lessonsForDay) {
                val list = mutableListOf<Color>()
                events.forEach { list.add(it.color) }
                lessonsForDay.forEach { list.add(it.color) }
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
    lessonsForDateProvider: (LocalDate) -> List<SchoolLesson> = { emptyList() },
    onDateSelected: (LocalDate) -> Unit
) {
    val colors = LocalOneUIColors.current
    val firstDayOfWeek = remember(month) { month.atDay(1).dayOfWeek.value }
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val prevMonth = remember(month) { month.minusMonths(1) }
    val daysInPrevMonth = remember(prevMonth) { prevMonth.lengthOfMonth() }
    val nextMonth = remember(month) { month.plusMonths(1) }

    val leadingDays = firstDayOfWeek - 1
    val totalCells = if (leadingDays + daysInMonth > 35) 42 else 35
    val rows = totalCells / 7

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    val (thisDate, isAdjacentMonth) = when {
                        index < leadingDays -> {
                            val dayNum = daysInPrevMonth - (leadingDays - 1 - index)
                            Pair(prevMonth.atDay(dayNum), true)
                        }
                        index < leadingDays + daysInMonth -> {
                            val dayNum = index - leadingDays + 1
                            Pair(month.atDay(dayNum), false)
                        }
                        else -> {
                            val dayNum = index - (leadingDays + daysInMonth) + 1
                            Pair(nextMonth.atDay(dayNum), true)
                        }
                    }

                    val isSelected = thisDate == selectedDate
                    val isToday = thisDate == today
                    val eventsForDay = eventsMap[thisDate] ?: emptyList()
                    val remindersForDay = remember(reminders, thisDate) {
                        reminders.filter { r ->
                            if (r.isRecurring) r.recurringDays.contains(thisDate.dayOfWeek.value)
                            else r.date != null && r.date == thisDate
                        }
                    }
                    val lessonsForDay = lessonsForDateProvider(thisDate)

                    val indicatorColors = remember(eventsForDay, remindersForDay, lessonsForDay) {
                        val list = mutableListOf<Color>()
                        eventsForDay.forEach { list.add(it.color) }
                        lessonsForDay.forEach { list.add(it.color) }
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
                                    isAdjacentMonth -> colors.surfaceSecondary.copy(alpha = 0.22f)
                                    else -> colors.surfaceSecondary.copy(alpha = 0.5f)
                                }
                            )
                            .border(
                                width = if (isToday && !isSelected) 1.dp else 0.dp,
                                color = if (isToday && !isSelected) colors.accent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onDateSelected(thisDate) }
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = thisDate.dayOfMonth.toString(),
                                color = when {
                                    isSelected -> colors.textPrimary
                                    isAdjacentMonth -> colors.textMuted.copy(alpha = 0.38f)
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
                                                .background(
                                                    if (isAdjacentMonth) barColor.copy(alpha = 0.45f) else barColor,
                                                    CircleShape
                                                )
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


// =========================================================================
// GESTIONE ORARIO SETTIMANALE DELLE LEZIONI (SAMSUNG ONE UI)
// =========================================================================

@Composable
fun SchoolScheduleDialog(
    viewModel: ClosetViewModel,
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
    onDismiss: () -> Unit,
    onAddLessonClick: () -> Unit,
    onEditLessonClick: (SchoolLesson) -> Unit
) {
    val colors = LocalOneUIColors.current
    val daysOfWeek = remember {
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    }
    val dayNames = remember { listOf("LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDate = viewModel.schoolScheduleStartDate
    val endDate = viewModel.schoolScheduleEndDate

    val lessonsForSelectedDay = remember(viewModel.schoolLessons.toList(), selectedDay) {
        viewModel.schoolLessons.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startTime }
    }

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
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Titolo & Chiudi
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.accentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = if (viewModel.useDistinctFunctionColors) Color(0xFF388AF6) else colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Orario Settimanale",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Configura lezioni e periodo",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card Periodo di Validità
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceSecondary,
                    border = BorderStroke(0.5.dp, colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Periodo di validità",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                            if (startDate != null || endDate != null) {
                                Text(
                                    text = "Resetta",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.updateSchoolSchedulePeriod(null, null)
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Chip Data Inizio
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (startDate != null) colors.accentContainer else colors.cardBackground,
                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartDatePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = if (startDate != null) colors.accent else colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (startDate != null) {
                                            val f = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)
                                            "Dal: ${startDate.format(f)}"
                                        } else "Data inizio",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (startDate != null) colors.textPrimary else colors.textMuted
                                    )
                                }
                            }

                            // Chip Data Fine
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (endDate != null) colors.accentContainer else colors.cardBackground,
                                border = BorderStroke(0.5.dp, colors.cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndDatePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = if (endDate != null) colors.accent else colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (endDate != null) {
                                            val f = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)
                                            "Al: ${endDate.format(f)}"
                                        } else "Data fine",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (endDate != null) colors.textPrimary else colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Giorni Settimana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeek.forEachIndexed { idx, day ->
                        val isDaySelected = day == selectedDay
                        val dayLabel = dayNames[idx]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isDaySelected) {
                                        if (viewModel.useDistinctFunctionColors) Color(0xFF388AF6) else colors.accent
                                    } else colors.surfaceSecondary
                                )
                                .clickable { onDaySelected(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isDaySelected) Color.White else (if (idx == 6) Color(0xFFE55353) else colors.textPrimary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Elenco lezioni per il giorno selezionato
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (lessonsForSelectedDay.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nessuna lezione per ${selectedDay.getDisplayName(TextStyle.FULL, Locale.ITALIAN).lowercase()}",
                                fontSize = 13.sp,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(lessonsForSelectedDay, key = { it.id }) { lesson ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.surfaceSecondary)
                                        .clickable { onEditLessonClick(lesson) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(lesson.color, CircleShape)
                                        )
                                        Column {
                                            Text(
                                                text = lesson.subject,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${lesson.startTime} - ${lesson.endTime}",
                                                    fontSize = 11.sp,
                                                    color = colors.textSecondary
                                                )
                                                if (lesson.room.isNotBlank()) {
                                                    Text(
                                                        text = "• ${lesson.room}",
                                                        fontSize = 11.sp,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onEditLessonClick(lesson) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "Modifica",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteSchoolLesson(lesson.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Elimina",
                                                tint = Color(0xFFE55353),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pulsante Aggiungi Lezione
                Button(
                    onClick = onAddLessonClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.useDistinctFunctionColors) Color(0xFF388AF6) else colors.accent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aggiungi Lezione",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showStartDatePicker) {
        val initialEpoch = startDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
            ?: LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.updateSchoolSchedulePeriod(selected, endDate)
                    }
                    showStartDatePicker = false
                }) { Text("OK", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Annulla", color = colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.dialogBackground)
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val initialEpoch = endDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
            ?: LocalDate.now().plusMonths(6).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.updateSchoolSchedulePeriod(startDate, selected)
                    }
                    showEndDatePicker = false
                }) { Text("OK", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Annulla", color = colors.textSecondary) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.dialogBackground)
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun AddOrEditSchoolLessonDialog(
    initialLesson: SchoolLesson? = null,
    defaultDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    onDismiss: () -> Unit,
    onSave: (SchoolLesson) -> Unit,
    onDelete: (String) -> Unit = {}
) {
    val colors = LocalOneUIColors.current
    var subject by remember { mutableStateOf(initialLesson?.subject ?: "") }
    var room by remember { mutableStateOf(initialLesson?.room ?: "") }
    var selectedDay by remember { mutableStateOf(initialLesson?.dayOfWeek ?: defaultDayOfWeek) }
    var startTimeStr by remember { mutableStateOf(initialLesson?.startTime ?: "08:00") }
    var endTimeStr by remember { mutableStateOf(initialLesson?.endTime ?: "10:00") }
    var selectedColor by remember { mutableStateOf(initialLesson?.color ?: Color(0xFF388AF6)) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val daysOfWeek = remember {
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    }
    val dayNames = remember { listOf("LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM") }

    val colorPalette = remember {
        listOf(
            Color(0xFF388AF6), // Blue
            Color(0xFF10B981), // Emerald
            Color(0xFF8B5CF6), // Purple
            Color(0xFFF59E0B), // Amber
            Color(0xFFEF4444), // Red
            Color(0xFF06B6D4), // Cyan
            Color(0xFFEC4899), // Pink
            Color(0xFF6366F1)  // Indigo
        )
    }

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
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (initialLesson == null) "Nuova Lezione" else "Modifica Lezione",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Materia
                Text(
                    text = "Materia",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    textStyle = ComposeTextStyle(
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { innerTextField ->
                        if (subject.isEmpty()) {
                            Text("Es. Matematica, Fisica...", color = colors.textMuted, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Luogo / Aula
                Text(
                    text = "Luogo / Aula (opzionale)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = room,
                    onValueChange = { room = it },
                    textStyle = ComposeTextStyle(
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { innerTextField ->
                        if (room.isEmpty()) {
                            Text("Es. Aula 3, Lab B...", color = colors.textMuted, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Giorno della settimana
                Text(
                    text = "Giorno della settimana",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeek.forEachIndexed { idx, day ->
                        val isDaySelected = day == selectedDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDaySelected) selectedColor else colors.surfaceSecondary)
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNames[idx],
                                fontSize = 11.sp,
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDaySelected) Color.White else colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selettori Orario Inizio e Fine
                Text(
                    text = "Orario",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = selectedColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text("Inizio", fontSize = 10.sp, color = colors.textSecondary)
                                Text(startTimeStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = selectedColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text("Fine", fontSize = 10.sp, color = colors.textSecondary)
                                Text(endTimeStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tavolozza Colori
                Text(
                    text = "Colore",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorPalette.forEach { col ->
                        val isChosen = col == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = col },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
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

                Spacer(modifier = Modifier.height(20.dp))

                // Tasti Azione
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (initialLesson != null) {
                        Button(
                            onClick = { onDelete(initialLesson.id) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353).copy(alpha = 0.15f)),
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Text("Elimina", color = Color(0xFFE55353), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (subject.isNotBlank()) {
                                onSave(
                                    SchoolLesson(
                                        id = initialLesson?.id ?: java.util.UUID.randomUUID().toString(),
                                        subject = subject.trim(),
                                        room = room.trim(),
                                        dayOfWeek = selectedDay,
                                        startTime = startTimeStr,
                                        endTime = endTimeStr,
                                        color = selectedColor
                                    )
                                )
                            }
                        },
                        enabled = subject.isNotBlank(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salva", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        val parts = startTimeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        SamsungTimeWheelPickerDialog(
            initialTime = LocalTime.of(h, m),
            onDismiss = { showStartTimePicker = false },
            onConfirm = { t ->
                startTimeStr = String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        val parts = endTimeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 10
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        SamsungTimeWheelPickerDialog(
            initialTime = LocalTime.of(h, m),
            onDismiss = { showEndTimePicker = false },
            onConfirm = { t ->
                endTimeStr = String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
                showEndTimePicker = false
            }
        )
    }
}
