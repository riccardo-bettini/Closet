package com.example.closet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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

                        // POSIZIONE INDICATORE (Sinistra, Destra)
                        HomeOptionSection(
                            icon = Icons.Outlined.ViewSidebar,
                            title = "Posizione indicatore"
                        ) {
                            val positionOptions = listOf("Sinistra", "Destra")
                            val selectedPosIndex = if (viewModel.homePageIndicatorPosition == PageIndicatorPosition.LEFT) 0 else 1
                            OneUISegmentedSlider(
                                options = positionOptions,
                                selectedIndex = selectedPosIndex,
                                enabled = isPaged,
                                onSelectIndex = { index ->
                                    val pos = if (index == 0) PageIndicatorPosition.LEFT else PageIndicatorPosition.RIGHT
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
fun VerticalPageIndicator(
    pageCount: Int,
    currentPage: Int,
    visibility: PageIndicatorVisibility,
    position: PageIndicatorPosition,
    isScrollInProgress: Boolean,
    isEditMode: Boolean,
    onPageClick: (Int) -> Unit = {}
) {
    if (pageCount <= 1 || visibility == PageIndicatorVisibility.NEVER) return

    val colors = LocalOneUIColors.current
    var isVisibleByScroll by remember { mutableStateOf(false) }

    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            isVisibleByScroll = true
        } else {
            delay(1200)
            isVisibleByScroll = false
        }
    }

    val targetAlpha = when (visibility) {
        PageIndicatorVisibility.NEVER -> 0f
        PageIndicatorVisibility.ALWAYS -> 1f
        PageIndicatorVisibility.ON_SCROLL -> if (isVisibleByScroll || isEditMode) 1f else 0f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(280),
        label = "pageIndicatorFade"
    )

    if (animatedAlpha <= 0.01f) return

    val alignment = if (position == PageIndicatorPosition.LEFT) Alignment.CenterStart else Alignment.CenterEnd
    val paddingModifier = if (position == PageIndicatorPosition.LEFT) {
        Modifier.padding(start = 5.dp)
    } else {
        Modifier.padding(end = 5.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(paddingModifier)
            .graphicsLayer { alpha = animatedAlpha },
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (colors.isDark) Color(0x33000000) else Color(0x1F000000),
            modifier = Modifier.wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(4.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 0 until pageCount) {
                    val isSelected = i == currentPage
                    val pillHeight by animateFloatAsState(
                        targetValue = if (isSelected) 18f else 6f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "pillHeight_$i"
                    )

                    Box(
                        modifier = Modifier
                            .width(4.dp)
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
