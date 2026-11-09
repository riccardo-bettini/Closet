package com.example.closet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.FormatLineSpacing
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.delay

// =========================================================================
// SLIDER A SEGMENTI ONE UI 9 CON ANIMAZIONE A MOLLA
// =========================================================================

@Composable
fun OneUISegmentedSlider(
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean = true,
    height: Dp = 46.dp,
    onSelectIndex: (Int) -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val safeIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))

    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    val animatedIndex by animateFloatAsState(
        targetValue = safeIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "segmentedSliderThumbAnim"
    )

    val thumbPadding = 3.dp
    val thumbPaddingPx = with(density) { thumbPadding.toPx() }
    val segmentCount = options.size.coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (colors.isDark) Color(0xFF161A22) else Color(0xFFE9ECF2),
        border = BorderStroke(0.5.dp, colors.cardBorder),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.38f
            }
            .onGloballyPositioned { coords ->
                containerWidthPx = coords.size.width.toFloat()
            }
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(options) {
                            detectTapGestures { offset ->
                                if (size.width > 0) {
                                    val segmentWidth = size.width / segmentCount.toFloat()
                                    val tappedIndex = (offset.x / segmentWidth).toInt().coerceIn(0, segmentCount - 1)
                                    onSelectIndex(tappedIndex)
                                }
                            }
                        }
                        .pointerInput(options) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                if (size.width > 0) {
                                    val segmentWidth = size.width / segmentCount.toFloat()
                                    val draggedIndex = (change.position.x / segmentWidth).toInt().coerceIn(0, segmentCount - 1)
                                    if (draggedIndex != safeIndex) {
                                        onSelectIndex(draggedIndex)
                                    }
                                }
                            }
                        }
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(thumbPadding)
        ) {
            val netWidthPx = (containerWidthPx - thumbPaddingPx * 2).coerceAtLeast(1f)
            val segmentWidthPx = netWidthPx / segmentCount.toFloat()
            val thumbWidthDp = with(density) { segmentWidthPx.toDp() }
            val thumbOffsetXDp = with(density) { (animatedIndex * segmentWidthPx).toDp() }

            // Pillola/Cursore mobile con animazione a molla
            if (containerWidthPx > 0f) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (colors.isDark) Color(0xFF282D37) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (enabled) colors.accent.copy(alpha = 0.5f) else Color.Transparent
                    ),
                    shadowElevation = if (enabled) 3.dp else 0.dp,
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .offset(x = thumbOffsetXDp)
                        .width(thumbWidthDp)
                        .fillMaxHeight()
                ) {}
            }

            // Etichette di testo delle opzioni
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, title ->
                    val isSelected = index == safeIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = when {
                                !enabled -> colors.textMuted
                                isSelected -> if (colors.isDark) Color.White else colors.accent
                                else -> colors.textSecondary
                            },
                            fontSize = if (options.size > 2) 11.5.sp else 13.sp,
                            lineHeight = 14.sp,
                            fontWeight = if (isSelected && enabled) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// DIALOG CONFIGURAZIONE DELLA HOME
// =========================================================================

@Composable
fun HomeOptionsDialog(
    viewModel: ClosetViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val currentMode = viewModel.homeLayoutMode
    val isPaged = currentMode == HomeLayoutMode.PAGED

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
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
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header con Titolo e tasto Chiusura
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
                                    .background(colors.accentContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesomeMosaic,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Opzioni home",
                                color = colors.textPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder)

                    // 1. SELETTORE MODALITÀ SCHERMATA (Scorrimento continuo vs A pagine)
                    HomeOptionSection(
                        icon = Icons.Outlined.Layers,
                        title = "Modalità schermata"
                    ) {
                        val modeOptions = listOf("Scorrimento continuo", "A pagine")
                        val selectedModeIndex = if (currentMode == HomeLayoutMode.INFINITE_SCROLL) 0 else 1
                        OneUISegmentedSlider(
                            options = modeOptions,
                            selectedIndex = selectedModeIndex,
                            enabled = true,
                            onSelectIndex = { index ->
                                val newMode = if (index == 0) HomeLayoutMode.INFINITE_SCROLL else HomeLayoutMode.PAGED
                                viewModel.updateHomeLayoutMode(newMode)
                            }
                        )
                    }

                    // 2. OPZIONI PERSONALIZZAZIONE PAGINE (Inutilizzabili e scurite se scorrimento continuo)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = if (isPaged) 1f else 0.38f
                            },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // RIGHE MASSIME PER PAGINA (6 o 7 - standard 6)
                        HomeOptionSection(
                            icon = Icons.Outlined.FormatLineSpacing,
                            title = "Righe massime per pagina"
                        ) {
                            val rowOptions = listOf("6 righe", "7 righe")
                            val selectedRowIndex = if (viewModel.homeMaxRowsPerPage == 7) 1 else 0
                            OneUISegmentedSlider(
                                options = rowOptions,
                                selectedIndex = selectedRowIndex,
                                enabled = isPaged,
                                onSelectIndex = { index ->
                                    val rows = if (index == 1) 7 else 6
                                    viewModel.updateHomeMaxRowsPerPage(rows)
                                }
                            )
                        }

                        // VISUALIZZAZIONE INDICATORE PAGINE (Non visibile, Solo in movimento, Sempre visibile)
                        HomeOptionSection(
                            icon = Icons.Outlined.Visibility,
                            title = "Indicatore pagine"
                        ) {
                            val visibilityOptions = listOf("Non visibile", "Solo in\nmovimento", "Sempre\nvisibile")
                            val selectedVisIndex = when (viewModel.homePageIndicatorVisibility) {
                                PageIndicatorVisibility.NEVER -> 0
                                PageIndicatorVisibility.ON_SCROLL -> 1
                                PageIndicatorVisibility.ALWAYS -> 2
                            }
                            OneUISegmentedSlider(
                                options = visibilityOptions,
                                selectedIndex = selectedVisIndex,
                                enabled = isPaged,
                                height = 48.dp,
                                onSelectIndex = { index ->
                                    val vis = when (index) {
                                        0 -> PageIndicatorVisibility.NEVER
                                        2 -> PageIndicatorVisibility.ALWAYS
                                        else -> PageIndicatorVisibility.ON_SCROLL
                                    }
                                    viewModel.updateHomePageIndicatorVisibility(vis)
                                }
                            )
                        }

                        // POSIZIONE INDICATORE (In basso, Destra, Sinistra)
                        HomeOptionSection(
                            icon = Icons.Outlined.ViewSidebar,
                            title = "Posizione indicatore"
                        ) {
                            val positionOptions = listOf("In basso", "Destra", "Sinistra")
                            val selectedPosIndex = when (viewModel.homePageIndicatorPosition) {
                                PageIndicatorPosition.BOTTOM -> 0
                                PageIndicatorPosition.RIGHT -> 1
                                PageIndicatorPosition.LEFT -> 2
                            }
                            OneUISegmentedSlider(
                                options = positionOptions,
                                selectedIndex = selectedPosIndex,
                                enabled = isPaged,
                                onSelectIndex = { index ->
                                    val pos = when (index) {
                                        0 -> PageIndicatorPosition.BOTTOM
                                        1 -> PageIndicatorPosition.RIGHT
                                        else -> PageIndicatorPosition.LEFT
                                    }
                                    viewModel.updateHomePageIndicatorPosition(pos)
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = colors.cardBorder)

                    // Bottone di Chiusura
                    Surface(
                        shape = CircleShape,
                        color = colors.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clickable(onClick = onDismiss)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Fine",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeOptionSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    val colors = LocalOneUIColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        content()
    }
}

// =========================================================================
// INDICATORE VERTICALE DELLE PAGINE A LATO DELLO SCHERMO (STILE ONE UI / iOS)
// =========================================================================

@Composable
fun HomePageIndicator(
    pageCount: Int,
    currentPage: Int,
    visibility: PageIndicatorVisibility,
    position: PageIndicatorPosition,
    isScrollInProgress: Boolean,
    isEditMode: Boolean,
    isDragging: Boolean = false,
    bottomOffset: androidx.compose.ui.unit.Dp = 76.dp,
    onDragStart: () -> Unit = {},
    onDragDelta: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onPageClick: (Int) -> Unit = {}
) {
    if (pageCount <= 1 || visibility == PageIndicatorVisibility.NEVER) return

    val colors = LocalOneUIColors.current
    var isVisibleByScroll by remember { mutableStateOf(false) }

    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            isVisibleByScroll = true
        } else {
            delay(1200)
            isVisibleByScroll = false
        }
    }

    val targetAlpha = when (visibility) {
        PageIndicatorVisibility.NEVER -> 0f
        PageIndicatorVisibility.ALWAYS -> 1f
        PageIndicatorVisibility.ON_SCROLL -> if (isVisibleByScroll || isEditMode || isDragging) 1f else 0f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(280),
        label = "pageIndicatorFade"
    )

    if (animatedAlpha <= 0.01f && !isDragging) return

    when (position) {
        PageIndicatorPosition.BOTTOM -> {
            // INDICATORE IN BASSO (CENTRO DELLO SCHERMO SOPRA L'ISOLA) - STILE SAMSUNG ONE UI HOME
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomOffset)
                    .graphicsLayer { alpha = animatedAlpha },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge pagina fluttuante sopra i pallini durante lo scorrimento rapido
                    AnimatedVisibility(
                        visible = isDragging,
                        enter = fadeIn(tween(150)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut(tween(150)) + scaleOut(spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.islandBackground,
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
                            shadowElevation = 8.dp,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${currentPage + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.accent
                                )
                                Text(
                                    text = "/ $pageCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    // Contenitore pill/punti orizzontale
                    val surfaceScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.12f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "indicatorBottomSurfaceScale"
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDragging) colors.islandBackground else (if (colors.isDark) Color(0x33000000) else Color(0x1F000000)),
                        border = if (isDragging) BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)) else null,
                        tonalElevation = if (isDragging) 4.dp else 0.dp,
                        shadowElevation = if (isDragging) 6.dp else 0.dp,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = surfaceScale
                                scaleY = surfaceScale
                            }
                            .pointerInput(pageCount) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isVisibleByScroll = true
                                        onDragStart()
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragDelta(dragAmount)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragCancel() }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until pageCount) {
                                val isSelected = i == currentPage
                                val pillWidth by animateFloatAsState(
                                    targetValue = if (isSelected) (if (isDragging) 22f else 16f) else 5f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "pillWidthHoriz_$i"
                                )
                                val pillHeight by animateFloatAsState(
                                    targetValue = if (isDragging && isSelected) 6f else 5f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "pillHeightHoriz_$i"
                                )

                                Box(
                                    modifier = Modifier
                                        .width(pillWidth.dp)
                                        .height(pillHeight.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) colors.accent
                                            else colors.textSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.35f)
                                        )
                                        .clickable { onPageClick(i) }
                                )
                            }
                        }
                    }
                }
            }
        }
        PageIndicatorPosition.RIGHT, PageIndicatorPosition.LEFT -> {
            // INDICATORE VERTICALE LATERALE (centrato nello spazio di 16 dp tra i widget e il bordo dello schermo)
            val isRight = position == PageIndicatorPosition.RIGHT
            val sideMargin = 16.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = animatedAlpha },
                contentAlignment = if (isRight) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                // Fascia laterale di 16 dp: l'indicatore è perfettamente centrato in questo spazio
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(sideMargin),
                    contentAlignment = Alignment.Center
                ) {
                    val surfaceScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.15f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "indicatorSurfaceScale"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDragging) colors.islandBackground else (if (colors.isDark) Color(0x33000000) else Color(0x1F000000)),
                        border = if (isDragging) BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)) else null,
                        tonalElevation = if (isDragging) 4.dp else 0.dp,
                        shadowElevation = if (isDragging) 6.dp else 0.dp,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = surfaceScale
                                scaleY = surfaceScale
                            }
                            .pointerInput(pageCount) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isVisibleByScroll = true
                                        onDragStart()
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragDelta(dragAmount)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragCancel() }
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (i in 0 until pageCount) {
                                val isSelected = i == currentPage
                                val pillHeight by animateFloatAsState(
                                    targetValue = if (isSelected) (if (isDragging) 22f else 18f) else 5f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "pillHeight_$i"
                                )
                                val pillWidth by animateFloatAsState(
                                    targetValue = if (isDragging && isSelected) 5f else 3.5f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "pillWidth_$i"
                                )

                                Box(
                                    modifier = Modifier
                                        .width(pillWidth.dp)
                                        .height(pillHeight.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) colors.accent
                                            else colors.textSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.35f)
                                        )
                                        .clickable { onPageClick(i) }
                                )
                            }
                        }
                    }
                }

                // Badge pagina fluttuante posizionato verso l'interno dello schermo durante il drag
                AnimatedVisibility(
                    visible = isDragging,
                    enter = fadeIn(tween(150)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(tween(150)) + scaleOut(spring(stiffness = Spring.StiffnessMediumLow)),
                    modifier = Modifier
                        .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(
                            end = if (isRight) sideMargin + 6.dp else 0.dp,
                            start = if (!isRight) sideMargin + 6.dp else 0.dp
                        )
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.islandBackground,
                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "${currentPage + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.accent
                            )
                            Text(
                                text = "/ $pageCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalPageIndicator(
    pageCount: Int,
    currentPage: Int,
    visibility: PageIndicatorVisibility,
    position: PageIndicatorPosition,
    isScrollInProgress: Boolean,
    isEditMode: Boolean,
    isDragging: Boolean = false,
    bottomOffset: androidx.compose.ui.unit.Dp = 76.dp,
    onDragStart: () -> Unit = {},
    onDragDelta: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onPageClick: (Int) -> Unit = {}
) = HomePageIndicator(
    pageCount, currentPage, visibility, position, isScrollInProgress, isEditMode, isDragging,
    bottomOffset, onDragStart, onDragDelta, onDragEnd, onDragCancel, onPageClick
)
