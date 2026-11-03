package com.example.closet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.closet.ui.viewmodel.ClosetViewModel

// =========================================================================
// MENU AGGIUNTA WIDGET ONE UI 9.0 (WIDGET PICKER DIALOG)
// =========================================================================

private data class WidgetCategoryGroup(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val widgets: List<WidgetType>,
    val supportsClosetSelection: (WidgetType) -> Boolean = { false }
)

@Composable
fun WidgetPickerDialog(
    viewModel: ClosetViewModel,
    availableClosets: List<String>,
    onDismiss: () -> Unit,
    onSelectWidget: (WidgetType, String?) -> Unit
) {
    val colors = LocalOneUIColors.current
    var pendingClosetWidget by remember { mutableStateOf<WidgetType?>(null) }

    val categories = remember(colors.accent) {
        listOf(
            WidgetCategoryGroup(
                id = "WARDROBE",
                title = "Armadio & Capi",
                subtitle = "Contatori, vetrine e azioni rapide",
                icon = Icons.Outlined.Checkroom,
                accentColor = colors.accent,
                widgets = listOf(
                    WidgetType.WARDROBE_COUNT_1X1,
                    WidgetType.WARDROBE_COUNT_1X2,
                    WidgetType.WARDROBE_COUNT_2X2,
                    WidgetType.WARDROBE_COUNT_2X4,
                    WidgetType.WARDROBE_LIST_1X4,
                    WidgetType.WARDROBE_LIST_2X2,
                    WidgetType.WARDROBE_LIST_2X4,
                    WidgetType.WARDROBE_ADD_ITEM_1X1,
                    WidgetType.WARDROBE_ADD_ITEM_1X2,
                    WidgetType.WARDROBE_ADD_ITEM_2X2,
                    WidgetType.WARDROBE_SEARCH_1X1,
                    WidgetType.WARDROBE_SEARCH_1X2
                ),
                supportsClosetSelection = { type ->
                    type in listOf(
                        WidgetType.WARDROBE_COUNT_1X1,
                        WidgetType.WARDROBE_COUNT_1X2,
                        WidgetType.WARDROBE_COUNT_2X2,
                        WidgetType.WARDROBE_COUNT_2X4,
                        WidgetType.WARDROBE_LIST_1X4,
                        WidgetType.WARDROBE_LIST_2X2,
                        WidgetType.WARDROBE_LIST_2X4
                    )
                }
            ),
            WidgetCategoryGroup(
                id = "CALENDAR",
                title = "Calendario & Impegni",
                subtitle = "Vista giornaliera, mensile e agenda",
                icon = Icons.Outlined.CalendarMonth,
                accentColor = Color(0xFF388AF6),
                widgets = listOf(
                    WidgetType.CALENDAR_TODAY_1X1,
                    WidgetType.CALENDAR_TODAY_2X1,
                    WidgetType.CALENDAR_TODAY_2X2,
                    WidgetType.CALENDAR_MONTH_2X2,
                    WidgetType.CALENDAR_AGENDA_4X2,
                    WidgetType.CALENDAR_ADD_EVENT_1X1,
                    WidgetType.CALENDAR_ADD_EVENT_1X2
                )
            ),
            WidgetCategoryGroup(
                id = "FUNCTIONS",
                title = "Funzioni Smart",
                subtitle = "Panni, spostamenti, viaggio e foto",
                icon = Icons.Default.DashboardCustomize,
                accentColor = Color(0xFF8B5CF6),
                widgets = listOf(
                    WidgetType.FUNCTION_QUICK_ACTIONS_1X4,
                    WidgetType.FUNCTION_QUICK_ACTIONS_2X2,
                    WidgetType.FUNCTION_LAUNDRY_1X1,
                    WidgetType.FUNCTION_LAUNDRY_1X2,
                    WidgetType.FUNCTION_LAUNDRY_2X2,
                    WidgetType.FUNCTION_LAUNDRY_2X4,
                    WidgetType.FUNCTION_TRIP_1X1,
                    WidgetType.FUNCTION_TRIP_1X2,
                    WidgetType.FUNCTION_TRIP_2X2,
                    WidgetType.FUNCTION_TRIP_2X4,
                    WidgetType.FUNCTION_MOVE_1X1,
                    WidgetType.FUNCTION_MOVE_1X2,
                    WidgetType.FUNCTION_MOVE_2X2,
                    WidgetType.FUNCTION_PHOTO_1X1,
                    WidgetType.FUNCTION_PHOTO_2X2,
                    WidgetType.FUNCTION_PHOTO_2X4,
                    WidgetType.FUNCTION_TRANSFERS_1X1,
                    WidgetType.FUNCTION_TRANSFERS_1X2,
                    WidgetType.FUNCTION_TRANSFERS_2X2,
                    WidgetType.FUNCTION_CALENDAR_1X1,
                    WidgetType.FUNCTION_TOOLS_1X1
                ),
                supportsClosetSelection = { type ->
                    type == WidgetType.FUNCTION_PHOTO_2X2 || type == WidgetType.FUNCTION_PHOTO_2X4
                }
            ),
            WidgetCategoryGroup(
                id = "CLOCK_TOOLS",
                title = "Orologio & Strumenti",
                subtitle = "Orologi, torcia LED, bussola e livella",
                icon = Icons.Outlined.Construction,
                accentColor = Color(0xFFEAB308),
                widgets = listOf(
                    WidgetType.CLOCK_DIGITAL_1X2,
                    WidgetType.CLOCK_DIGITAL_2X2,
                    WidgetType.CLOCK_DIGITAL_2X4,
                    WidgetType.CLOCK_ANALOG_1X1,
                    WidgetType.CLOCK_ANALOG_2X2,
                    WidgetType.CLOCK_ANALOG_2X4,
                    WidgetType.UTILITY_TORCH_1X1,
                    WidgetType.UTILITY_TORCH_1X2,
                    WidgetType.UTILITY_TORCH_2X2,
                    WidgetType.UTILITY_COMPASS_1X1,
                    WidgetType.UTILITY_COMPASS_1X2,
                    WidgetType.UTILITY_COMPASS_2X2,
                    WidgetType.UTILITY_COMPASS_2X4,
                    WidgetType.UTILITY_LEVEL_1X1,
                    WidgetType.UTILITY_LEVEL_1X2,
                    WidgetType.UTILITY_LEVEL_2X2,
                    WidgetType.UTILITY_LEVEL_2X4
                )
            )
        )
    }

    // Sezioni chiudibili: tutte chiuse come preimpostazione
    var expandedCategoryTitles by remember {
        mutableStateOf(emptySet<String>())
    }

    val topFadeColor = colors.dialogBackground
    val bottomFadeColor = colors.dialogBackground

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.dialogBackground,
            border = BorderStroke(1.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp)
                ) {
                    // =========================================================
                    // 1. HEADER FISSO ONE UI
                    // =========================================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aggiungi widget",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                            letterSpacing = 0.3.sp,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceSecondary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // =========================================================
                    // 2. AREA SCROLLABILE: CATEGORIE CHIUDIBILI
                    // =========================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(categories, key = { it.id }) { cat ->
                                    val isExpanded = expandedCategoryTitles.contains(cat.title)
                                    val arrowRotation by animateFloatAsState(
                                        targetValue = if (isExpanded) 180f else 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "arrowRotation_${cat.id}"
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(26.dp),
                                        color = colors.cardBackground,
                                        border = BorderStroke(0.5.dp, colors.cardBorder),
                                        tonalElevation = 2.dp,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            // Intestazione sezione chiudibile
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = colors.surfaceSecondary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedCategoryTitles = if (isExpanded) {
                                                            expandedCategoryTitles - cat.title
                                                        } else {
                                                            expandedCategoryTitles + cat.title
                                                        }
                                                    }
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
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(cat.accentColor.copy(alpha = 0.16f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = cat.icon,
                                                                contentDescription = null,
                                                                tint = cat.accentColor,
                                                                modifier = Modifier.size(19.dp)
                                                            )
                                                        }

                                                        Column {
                                                            Text(
                                                                text = cat.title,
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colors.textPrimary,
                                                                maxLines = 1,
                                                                modifier = Modifier.basicMarquee()
                                                            )
                                                            Text(
                                                                text = "${cat.widgets.size} widget disponibili",
                                                                fontSize = 11.5.sp,
                                                                color = colors.textSecondary
                                                            )
                                                        }
                                                    }

                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowDown,
                                                        contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                                                        tint = colors.textSecondary,
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .graphicsLayer { rotationZ = arrowRotation }
                                                    )
                                                }
                                            }

                                            // Contenuto espanso: lista card con anteprime
                                            AnimatedVisibility(
                                                visible = isExpanded,
                                                enter = fadeIn(tween(180)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                                                exit = fadeOut(tween(130)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    cat.widgets.forEach { wType ->
                                                        OneUIWidgetCard(
                                                            wType = wType,
                                                            cat = cat,
                                                            viewModel = viewModel,
                                                            onAddClick = {
                                                                if (cat.supportsClosetSelection(wType) && availableClosets.size > 1) {
                                                                    pendingClosetWidget = wType
                                                                } else {
                                                                    onSelectWidget(wType, null)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        // Sfumatura superiore in uscita
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            topFadeColor,
                                            topFadeColor.copy(alpha = 0.70f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .zIndex(10f)
                        )

                        // Sfumatura inferiore in uscita
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            bottomFadeColor.copy(alpha = 0.70f),
                                            bottomFadeColor
                                        )
                                    )
                                )
                                .zIndex(10f)
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // 5. DIALOG SCELTA ARMADIO (SE IL WIDGET LO SUPPORTA)
    // =========================================================================
    if (pendingClosetWidget != null) {
        val targetType = pendingClosetWidget!!
        AlertDialog(
            onDismissRequest = { pendingClosetWidget = null },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Scegli Armadio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (targetType != WidgetType.FUNCTION_PHOTO_2X2 && targetType != WidgetType.FUNCTION_PHOTO_2X4) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectWidget(targetType, null)
                                    pendingClosetWidget = null
                                }
                        ) {
                            Text(
                                text = "Tutti gli armadi (Totale)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    availableClosets.forEach { closet ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectWidget(targetType, closet)
                                    pendingClosetWidget = null
                                }
                        ) {
                            Text(
                                text = closet,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingClosetWidget = null }) {
                    Text("Annulla", color = colors.textSecondary)
                }
            }
        )
    }
}

// =========================================================================
// CARD OPZIONE WIDGET CON ANTEPRIMA MINIATURIZZATA ONE UI
// =========================================================================

@Composable
private fun OneUIWidgetCard(
    wType: WidgetType,
    cat: WidgetCategoryGroup,
    viewModel: ClosetViewModel,
    onAddClick: () -> Unit
) {
    val colors = LocalOneUIColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceSecondary,
        border = BorderStroke(0.5.dp, colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Box contenitore anteprima proporzionato
            Box(
                modifier = Modifier
                    .width(86.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBackground)
                    .border(0.5.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                WidgetScaledPreview(type = wType, viewModel = viewModel)
            }

            // Colonna informazioni: Titolo sopra, badge e pulsante Aggiungi sotto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = wType.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = cat.accentColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${wType.colSpan} \u00D7 ${wType.rowSpan}",
                                fontSize = 11.sp,
                                color = cat.accentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        val cellCount = wType.colSpan * wType.rowSpan
                        Text(
                            text = if (cellCount == 1) "1 cella" else "$cellCount celle",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Tasto Aggiungi a pillola One UI 9.0 (ricreato esteticamente)
                    Surface(
                        shape = CircleShape,
                        color = cat.accentColor.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = cat.accentColor.copy(alpha = if (colors.isDark) 0.45f else 0.35f)
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onAddClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = cat.accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Aggiungi",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = cat.accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCALER ANTEPRIMA WIDGET REAL-TIME
// =========================================================================

@Composable
fun WidgetScaledPreview(
    type: WidgetType,
    viewModel: ClosetViewModel,
    modifier: Modifier = Modifier,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val baseCell = 68.dp
    val spacing = 6.dp
    val fullWidth = (baseCell * type.colSpan) + (spacing * (type.colSpan - 1))
    val fullHeight = (baseCell * type.rowSpan) + (spacing * (type.rowSpan - 1))

    val maxTargetWidth = 80f
    val maxTargetHeight = 52f
    val scaleFactor = minOf(
        maxTargetWidth / fullWidth.value,
        maxTargetHeight / fullHeight.value
    ).coerceAtMost(0.55f)

    val previewWidthDp = fullWidth * scaleFactor
    val previewHeightDp = fullHeight * scaleFactor

    Box(
        modifier = modifier
            .size(width = previewWidthDp, height = previewHeightDp)
            .pointerInput(Unit) { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredSize(width = fullWidth, height = fullHeight)
                .graphicsLayer {
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .clip(RoundedCornerShape(if (type.colSpan == 1 && type.rowSpan == 1) fullHeight / 2 else 22.dp))
                .background(colors.cardBackground)
                .border(
                    width = 1.2.dp,
                    color = colors.cardBorder,
                    shape = RoundedCornerShape(if (type.colSpan == 1 && type.rowSpan == 1) fullHeight / 2 else 22.dp)
                )
        ) {
            WidgetLivePreviewContent(type = type, viewModel = viewModel, useDistinctColors = useDistinctColors)
        }
    }
}

// =========================================================================
// RENDERING CONTENUTO ANTEPRIMA WIDGET
// =========================================================================

@Composable
private fun WidgetLivePreviewContent(
    type: WidgetType,
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current

    when (type) {
        WidgetType.CALENDAR_TODAY_1X1 -> CalendarTodayWidget1x1(viewModel)
        WidgetType.CALENDAR_TODAY_2X1 -> CalendarTodayWidget2x1(viewModel)
        WidgetType.CALENDAR_TODAY_2X2 -> CalendarTodayWidget2x2(viewModel)
        WidgetType.CALENDAR_MONTH_2X2 -> CalendarMonthWidget2x2(viewModel)
        WidgetType.CALENDAR_AGENDA_4X2 -> CalendarAgendaWidget4x2(viewModel)
        WidgetType.CALENDAR_ADD_EVENT_1X1 -> CalendarAddEventWidget1x1()
        WidgetType.CALENDAR_ADD_EVENT_1X2 -> CalendarAddEventWidget1x2()

        WidgetType.WARDROBE_COUNT_1X1 -> WardrobeCountWidget1x1(viewModel, null)
        WidgetType.WARDROBE_COUNT_1X2 -> WardrobeCountWidget1x2(viewModel, null)
        WidgetType.WARDROBE_COUNT_2X2 -> WardrobeCountWidget2x2(viewModel, null)
        WidgetType.WARDROBE_COUNT_2X4 -> WardrobeCountWidget2x4(viewModel, null)

        WidgetType.WARDROBE_LIST_1X4 -> WardrobeListWidget1x4(viewModel, null)
        WidgetType.WARDROBE_LIST_2X2 -> WardrobeListWidget2x2(viewModel, null)
        WidgetType.WARDROBE_LIST_2X4 -> WardrobeListWidget2x4(viewModel, null)

        WidgetType.WARDROBE_SEARCH_1X1 -> WardrobeSearchWidget1x1()
        WidgetType.WARDROBE_SEARCH_1X2 -> WardrobeSearchWidget1x2()
        WidgetType.WARDROBE_ADD_ITEM_1X1 -> WardrobeAddItemWidget1x1()
        WidgetType.WARDROBE_ADD_ITEM_1X2 -> WardrobeAddItemWidget1x2()
        WidgetType.WARDROBE_ADD_ITEM_2X2 -> WardrobeAddItemWidget2x2(viewModel)

        WidgetType.FUNCTION_CALENDAR_1X1 -> FunctionShortcutWidget1x1(
            icon = Icons.Outlined.CalendarMonth,
            tint = if (useDistinctColors) Color(0xFF388AF6) else colors.accent,
            label = "Calendario"
        )
        WidgetType.FUNCTION_TOOLS_1X1 -> FunctionShortcutWidget1x1(
            icon = Icons.Outlined.Construction,
            tint = if (useDistinctColors) Color(0xFFEAB308) else colors.accent,
            label = "Strumenti"
        )
        WidgetType.FUNCTION_QUICK_ACTIONS_1X4 -> FunctionQuickActionsWidget1x4(
            useDistinctColors = useDistinctColors,
            isEditMode = true,
            onSelectFunction = {}
        )
        WidgetType.FUNCTION_QUICK_ACTIONS_2X2 -> FunctionQuickActionsWidget2x2(
            useDistinctColors = useDistinctColors,
            isEditMode = true,
            onSelectFunction = {}
        )
        WidgetType.FUNCTION_PHOTO_1X1 -> FunctionShortcutWidget1x1(
            icon = Icons.Outlined.CameraAlt,
            tint = if (useDistinctColors) Color(0xFF53B5A2) else colors.accent,
            label = "Foto"
        )
        WidgetType.FUNCTION_PHOTO_2X2 -> FunctionPhotoWidget2x2(viewModel, null, useDistinctColors = useDistinctColors)
        WidgetType.FUNCTION_PHOTO_2X4 -> FunctionPhotoWidget2x4(viewModel, null, useDistinctColors = useDistinctColors)

        WidgetType.FUNCTION_MOVE_1X1 -> FunctionShortcutWidget1x1(
            icon = Icons.Outlined.SyncAlt,
            tint = if (useDistinctColors) Color(0xFF10B981) else colors.accent,
            label = "Sposta"
        )
        WidgetType.FUNCTION_MOVE_1X2 -> FunctionMoveWidget1x2(viewModel, useDistinctColors = useDistinctColors)
        WidgetType.FUNCTION_MOVE_2X2 -> FunctionMoveWidget2x2(viewModel, useDistinctColors = useDistinctColors)

        WidgetType.FUNCTION_LAUNDRY_1X1 -> FunctionLaundryWidget1x1(viewModel, useDistinctColors = useDistinctColors)
        WidgetType.FUNCTION_LAUNDRY_1X2 -> FunctionLaundryWidget1x2(viewModel, useDistinctColors = useDistinctColors)
        WidgetType.FUNCTION_LAUNDRY_2X2 -> FunctionLaundryWidget2x2(viewModel, useDistinctColors = useDistinctColors)
        WidgetType.FUNCTION_LAUNDRY_2X4 -> FunctionLaundryWidget2x4(viewModel, useDistinctColors = useDistinctColors)

        WidgetType.FUNCTION_TRIP_1X1 -> FunctionShortcutWidget1x1(
            icon = Icons.Outlined.Work,
            tint = if (useDistinctColors) Color(0xFF8B5CF6) else colors.accent,
            label = "Viaggio"
        )
        WidgetType.FUNCTION_TRIP_1X2 -> FunctionTripWidget1x2(viewModel, useDistinctColors = useDistinctColors)
        WidgetType.FUNCTION_TRIP_2X2 -> FunctionTripWidget2x2(viewModel, useDistinctColors = useDistinctColors)
        WidgetType.FUNCTION_TRIP_2X4 -> FunctionTripWidget2x4(viewModel, useDistinctColors = useDistinctColors)

        WidgetType.FUNCTION_TRANSFERS_1X1 -> FunctionShortcutWidget1x1(
            icon = Icons.Outlined.History,
            tint = if (useDistinctColors) Color(0xFFF97316) else colors.accent,
            label = "Registro"
        )
        WidgetType.FUNCTION_TRANSFERS_1X2 -> FunctionTransfersWidget1x2(viewModel, useDistinctColors = useDistinctColors)
        WidgetType.FUNCTION_TRANSFERS_2X2 -> FunctionTransfersWidget2x2(viewModel, useDistinctColors = useDistinctColors)

        WidgetType.UTILITY_TORCH_1X1 -> UtilityTorchWidget1x1(isEditMode = true)
        WidgetType.UTILITY_TORCH_1X2 -> UtilityTorchWidget1x2(isEditMode = true)
        WidgetType.UTILITY_TORCH_2X2 -> UtilityTorchWidget2x2(isEditMode = true)

        WidgetType.UTILITY_COMPASS_1X1 -> UtilityCompassWidget1x1()
        WidgetType.UTILITY_COMPASS_1X2 -> UtilityCompassWidget1x2()
        WidgetType.UTILITY_COMPASS_2X2 -> UtilityCompassWidget2x2()
        WidgetType.UTILITY_COMPASS_2X4 -> UtilityCompassWidget2x4()

        WidgetType.UTILITY_LEVEL_1X1 -> UtilityLevelWidget1x1()
        WidgetType.UTILITY_LEVEL_1X2 -> UtilityLevelWidget1x2()
        WidgetType.UTILITY_LEVEL_2X2 -> UtilityLevelWidget2x2()
        WidgetType.UTILITY_LEVEL_2X4 -> UtilityLevelWidget2x4()

        WidgetType.CLOCK_DIGITAL_1X2 -> ClockDigitalWidget1x2()
        WidgetType.CLOCK_DIGITAL_2X2 -> ClockDigitalWidget2x2()
        WidgetType.CLOCK_DIGITAL_2X4 -> ClockDigitalWidget2x4()
        WidgetType.CLOCK_ANALOG_1X1 -> ClockAnalogWidget1x1()
        WidgetType.CLOCK_ANALOG_2X2 -> ClockAnalogWidget2x2()
        WidgetType.CLOCK_ANALOG_2X4 -> ClockAnalogWidget2x4(viewModel)
    }
}