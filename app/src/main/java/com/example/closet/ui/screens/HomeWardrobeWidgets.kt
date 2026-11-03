package com.example.closet.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closet.ui.viewmodel.ClosetViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// =========================================================================
// WIDGET CALENDARIO
// =========================================================================

@Composable
fun CalendarTodayWidget1x1(viewModel: ClosetViewModel? = null) {
    val colors = LocalOneUIColors.current
    val today = remember { LocalDate.now() }
    val dayShort = remember(today) {
        today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALIAN)
            .replace(".", "")
            .uppercase()
    }
    val eventsCount = remember(viewModel?.eventsMap, today) {
        viewModel?.eventsMap?.get(today)?.size ?: 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayShort,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.accent,
                    lineHeight = 11.sp
                )
                Text(
                    text = today.dayOfMonth.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    lineHeight = 22.sp
                )
                if (eventsCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarTodayWidget2x1(viewModel: ClosetViewModel? = null) {
    val colors = LocalOneUIColors.current
    val today = remember { LocalDate.now() }
    val dayName = remember(today) {
        today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
    }
    val todayEvents = viewModel?.eventsMap?.get(today) ?: emptyList()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = today.dayOfMonth.toString(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = colors.accent
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1
            )
            if (todayEvents.isNotEmpty()) {
                Text(
                    text = todayEvents.first().title,
                    fontSize = 11.5.sp,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
fun CalendarTodayWidget2x2(viewModel: ClosetViewModel? = null) {
    val colors = LocalOneUIColors.current
    val today = remember { LocalDate.now() }
    val dayName = remember(today) {
        today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
    }
    val monthName = remember(today) {
        today.month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
    }
    val todayEvents = viewModel?.eventsMap?.get(today) ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = dayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = today.dayOfMonth.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        lineHeight = 34.sp
                    )
                    Text(
                        text = monthName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (todayEvents.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.eventChipBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nessun evento oggi",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                todayEvents.take(2).forEach { ev ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.accentContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                            Text(
                                text = ev.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarMonthWidget2x2(
    viewModel: ClosetViewModel? = null,
    innerPadding: PaddingValues = PaddingValues(14.dp)
) {
    val colors = LocalOneUIColors.current
    val today = remember { LocalDate.now() }
    val yearMonth = remember(today) { YearMonth.from(today) }
    val monthTitle = remember(yearMonth) {
        yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
            .replaceFirstChar { it.uppercase() }
    }
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOffset = (yearMonth.atDay(1).dayOfWeek.value - 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent
            )
            Text(
                text = yearMonth.year.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
        }

        val weekLetters = listOf("L", "M", "M", "G", "V", "S", "D")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekLetters.forEach { d ->
                Text(
                    text = d,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(16.dp)
                )
            }
        }

        val totalCellsNeeded = firstDayOffset + daysInMonth
        val totalWeeks = if (totalCellsNeeded > 35) 6 else 5
        val cellSize = if (totalWeeks == 6) 11.5.dp else 14.dp
        val dayFontSize = if (totalWeeks == 6) 7.sp else 8.5.sp
        for (week in 0 until totalWeeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dow in 0 until 7) {
                    val cellIdx = week * 7 + dow
                    val dayNum = cellIdx - firstDayOffset + 1
                    val isValidDay = dayNum in 1..daysInMonth
                    val isToday = isValidDay && dayNum == today.dayOfMonth
                    val hasEvent = isValidDay &&
                            (viewModel?.eventsMap?.get(yearMonth.atDay(dayNum))?.isNotEmpty() == true)

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isToday -> colors.accent
                                    hasEvent -> colors.accentContainer
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isValidDay) {
                            Text(
                                text = dayNum.toString(),
                                fontSize = dayFontSize,
                                fontWeight = if (isToday || hasEvent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Color.White else colors.textPrimary,
                                style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarAgendaWidget4x2(viewModel: ClosetViewModel? = null) {
    val colors = LocalOneUIColors.current
    val today = remember { LocalDate.now() }
    val todayEvents = viewModel?.eventsMap?.get(today) ?: emptyList()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            CalendarMonthWidget2x2(viewModel, innerPadding = PaddingValues(0.dp))
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.cardBorder)
        )

        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Impegni di oggi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Surface(
                    shape = CircleShape,
                    color = colors.accentContainer
                ) {
                    Text(
                        text = todayEvents.size.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            if (todayEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Nessun evento oggi",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    todayEvents.take(3).forEach { ev ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.eventChipBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent)
                                )
                                Text(
                                    text = ev.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarAddEventWidget1x1() {
    val colors = LocalOneUIColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = "Nuovo Evento",
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CalendarAddEventWidget1x2() {
    val colors = LocalOneUIColors.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "Nuovo Evento",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1
        )
    }
}

// =========================================================================
// WIDGET ARMADIO (CONTEGGIO, ELENCO, RICERCA, AGGIUNGI)
// =========================================================================

@Composable
fun WardrobeCountWidget1x1(viewModel: ClosetViewModel, targetCloset: String?) {
    val colors = LocalOneUIColors.current
    val count = remember(viewModel.clothingItems.toList(), targetCloset) {
        if (targetCloset == null) viewModel.clothingItems.size
        else viewModel.clothingItems.count { it.closetName == targetCloset }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = count.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun WardrobeCountWidget1x2(viewModel: ClosetViewModel, targetCloset: String?) {
    val colors = LocalOneUIColors.current
    val count = remember(viewModel.clothingItems.toList(), targetCloset) {
        if (targetCloset == null) viewModel.clothingItems.size
        else viewModel.clothingItems.count { it.closetName == targetCloset }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Checkroom,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$count Capi",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = targetCloset ?: "Tutti gli armadi",
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
fun WardrobeCountWidget2x2(viewModel: ClosetViewModel, targetCloset: String?) {
    val colors = LocalOneUIColors.current
    val items = remember(viewModel.clothingItems.toList(), targetCloset) {
        if (targetCloset == null) viewModel.clothingItems
        else viewModel.clothingItems.filter { it.closetName == targetCloset }
    }
    val foldersCount = remember(viewModel.closetFolders.toList(), targetCloset) {
        if (targetCloset == null) viewModel.closetFolders.size
        else viewModel.closetFolders.count { it.closetName == targetCloset }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.accentContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(19.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = colors.surfaceSecondary
            ) {
                Text(
                    text = "$foldersCount cartelle",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Column {
            Text(
                text = items.size.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                lineHeight = 34.sp
            )
            Text(
                text = targetCloset ?: "Totale Armadio",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
fun WardrobeCountWidget2x4(viewModel: ClosetViewModel, targetCloset: String?) {
    val colors = LocalOneUIColors.current
    val items = remember(viewModel.clothingItems.toList(), targetCloset) {
        if (targetCloset == null) viewModel.clothingItems
        else viewModel.clothingItems.filter { it.closetName == targetCloset }
    }
    val foldersCount = remember(viewModel.closetFolders.toList(), targetCloset) {
        if (targetCloset == null) viewModel.closetFolders.size
        else viewModel.closetFolders.count { it.closetName == targetCloset }
    }
    val closetsCount = viewModel.availableClosets.size

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DoorSliding,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = targetCloset ?: "Tutti gli armadi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }

            Text(
                text = items.size.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                lineHeight = 42.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surfaceSecondary,
            modifier = Modifier
                .width(135.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Ripartizione",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                Text(
                    text = "$closetsCount armadi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "$foldersCount cartelle",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
            }
        }
    }
}

@Composable
fun WardrobeListWidget1x4(viewModel: ClosetViewModel, targetCloset: String?) {
    val colors = LocalOneUIColors.current
    val items = remember(viewModel.clothingItems.toList(), targetCloset) {
        val filtered = if (targetCloset == null) viewModel.clothingItems
        else viewModel.clothingItems.filter { it.closetName == targetCloset }
        filtered.takeLast(6).reversed()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Checkroom,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        if (items.isEmpty()) {
            Text(
                text = "Nessun capo in ${targetCloset ?: "armadio"}",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        } else {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.take(5).forEach { item ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.imageUri != null) {
                            Image(
                                painter = rememberOptimizedPainter(item.imageUri, targetSizeDp = 70),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Checkroom,
                                contentDescription = item.name,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WardrobeListWidget2x2(viewModel: ClosetViewModel, targetCloset: String?) {
    val colors = LocalOneUIColors.current
    val items = remember(viewModel.clothingItems.toList(), targetCloset) {
        val filtered = if (targetCloset == null) viewModel.clothingItems
        else viewModel.clothingItems.filter { it.closetName == targetCloset }
        filtered.takeLast(3).reversed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = targetCloset ?: "Recenti",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Icon(
                imageVector = Icons.Outlined.Checkroom,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Armadio vuoto",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.imageUri != null) {
                                Image(
                                    painter = rememberOptimizedPainter(item.imageUri, targetSizeDp = 50),
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Checkroom,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = item.name,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).basicMarquee()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WardrobeListWidget2x4(viewModel: ClosetViewModel, targetCloset: String?) {
    val colors = LocalOneUIColors.current
    val items = remember(viewModel.clothingItems.toList(), targetCloset) {
        val filtered = if (targetCloset == null) viewModel.clothingItems
        else viewModel.clothingItems.filter { it.closetName == targetCloset }
        filtered.takeLast(4).reversed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = targetCloset ?: "Ultimi capi aggiunti",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )
            }

            Text(
                text = "Vedi tutti",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun capo presente. Tocca per aggiungerne uno.",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.cardBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.imageUri != null) {
                                    Image(
                                        painter = rememberOptimizedPainter(item.imageUri, targetSizeDp = 90),
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Checkroom,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 4.dp).basicMarquee()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WardrobeSearchWidget1x1() {
    val colors = LocalOneUIColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cerca",
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun WardrobeSearchWidget1x2() {
    val colors = LocalOneUIColors.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = "Cerca capo",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun WardrobeAddItemWidget1x1() {
    val colors = LocalOneUIColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Aggiungi Capo",
                tint = colors.accent,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun WardrobeAddItemWidget1x2() {
    val colors = LocalOneUIColors.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "Nuovo Capo",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun WardrobeAddItemWidget2x2(viewModel: ClosetViewModel) {
    val colors = LocalOneUIColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "Aggiungi Capo",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary
        )
    }
}