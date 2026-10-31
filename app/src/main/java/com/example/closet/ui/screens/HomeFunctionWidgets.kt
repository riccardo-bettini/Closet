package com.example.closet.ui.screens

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// =========================================================================
// WIDGET FUNZIONI (SCORCIATOIE, FOTO, SPOSTAMENTO, PANNI, VIAGGIO, REGISTRO)
// =========================================================================

@Composable
fun FunctionShortcutWidget1x1(
    icon: ImageVector,
    tint: Color,
    label: String
) {
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
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FunctionQuickActionsWidget1x4(
    useDistinctColors: Boolean,
    isEditMode: Boolean,
    onSelectFunction: (FunctionSubScreen) -> Unit
) {
    val colors = LocalOneUIColors.current
    val items = listOf(
        Triple(FunctionSubScreen.CALENDAR, Icons.Outlined.CalendarMonth, if (useDistinctColors) Color(0xFF388AF6) else colors.accent),
        Triple(FunctionSubScreen.MOVE, Icons.Outlined.SyncAlt, if (useDistinctColors) Color(0xFF10B981) else colors.accent),
        Triple(FunctionSubScreen.LAUNDRY, Icons.Outlined.LocalLaundryService, if (useDistinctColors) Color(0xFFEF4444) else colors.accent),
        Triple(FunctionSubScreen.TRIP, Icons.Outlined.Work, if (useDistinctColors) Color(0xFF8B5CF6) else colors.accent)
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (subScreen, icon, tint) ->
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (colors.isDark) 0.20f else 0.14f))
                    .then(
                        if (!isEditMode) {
                            Modifier.clickable { onSelectFunction(subScreen) }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun FunctionQuickActionsWidget2x2(
    useDistinctColors: Boolean,
    isEditMode: Boolean,
    onSelectFunction: (FunctionSubScreen) -> Unit
) {
    val colors = LocalOneUIColors.current
    val actions = listOf(
        Triple(FunctionSubScreen.CALENDAR, Icons.Outlined.CalendarMonth, if (useDistinctColors) Color(0xFF388AF6) else colors.accent),
        Triple(FunctionSubScreen.MOVE, Icons.Outlined.SyncAlt, if (useDistinctColors) Color(0xFF10B981) else colors.accent),
        Triple(FunctionSubScreen.LAUNDRY, Icons.Outlined.LocalLaundryService, if (useDistinctColors) Color(0xFFEF4444) else colors.accent),
        Triple(FunctionSubScreen.TRIP, Icons.Outlined.Work, if (useDistinctColors) Color(0xFF8B5CF6) else colors.accent)
    )

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
                text = "Funzioni",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Icon(
                imageVector = Icons.Default.DashboardCustomize,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(14.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowItems.forEach { (subScreen, icon, tint) ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
                                .then(
                                    if (!isEditMode) {
                                        Modifier.clickable { onSelectFunction(subScreen) }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tint,
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
fun FunctionPhotoWidget2x2(
    viewModel: ClosetViewModel,
    targetCloset: String?,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val closetName = targetCloset ?: viewModel.availableClosets.firstOrNull() ?: "Armadio"
    val photoUri: Uri? = remember(viewModel.clothingItems.toList(), closetName) {
        viewModel.clothingItems.firstOrNull { it.closetName == closetName && it.imageUri != null }?.imageUri
    }
    val tint = if (useDistinctColors) Color(0xFF53B5A2) else colors.accent
    val bgTint = if (useDistinctColors) tint.copy(alpha = if (colors.isDark) 0.22f else 0.14f) else colors.accentContainer

    Box(modifier = Modifier.fillMaxSize()) {
        if (photoUri != null) {
            Image(
                painter = rememberOptimizedPainter(photoUri, targetSizeDp = 220),
                contentDescription = closetName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = closetName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = "Panoramica",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column {
                    Text(
                        text = closetName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = "Panoramica foto",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun FunctionPhotoWidget2x4(
    viewModel: ClosetViewModel,
    targetCloset: String?,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val closetName = targetCloset ?: viewModel.availableClosets.firstOrNull() ?: "Armadio"
    val photoUri: Uri? = remember(viewModel.clothingItems.toList(), closetName) {
        viewModel.clothingItems.firstOrNull { it.closetName == closetName && it.imageUri != null }?.imageUri
    }
    val count = viewModel.clothingItems.count { it.closetName == closetName }
    val tint = if (useDistinctColors) Color(0xFF53B5A2) else colors.accent

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                Image(
                    painter = rememberOptimizedPainter(photoUri, targetSizeDp = 240),
                    contentDescription = closetName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "FOTO ARMADIO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
                Text(
                    text = closetName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Column {
                Text(
                    text = "$count capi presenti",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Text(
                    text = if (photoUri != null) "Tocca per aprire" else "Tocca per gestire",
                    fontSize = 11.sp,
                    color = colors.accent
                )
            }
        }
    }
}

@Composable
fun FunctionMoveWidget1x2(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFF10B981) else colors.accent

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
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SyncAlt,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sposta Capi",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = "${viewModel.availableClosets.size} armadi",
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FunctionMoveWidget2x2(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFF10B981) else colors.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SyncAlt,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(21.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Spostamento",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
            Text(
                text = "Trasferisci capi tra ${viewModel.availableClosets.size} armadi",
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FunctionLaundryWidget1x1(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFFEF4444) else colors.accent

    FunctionShortcutWidget1x1(
        icon = Icons.Outlined.LocalLaundryService,
        tint = tint,
        label = "Panni"
    )
}

@Composable
fun FunctionLaundryWidget1x2(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFFEF4444) else colors.accent

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
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalLaundryService,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Panni sporchi",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = "Lavaggio & Timer",
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FunctionLaundryWidget2x2(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFFEF4444) else colors.accent

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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalLaundryService,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = colors.surfaceSecondary
            ) {
                Text(
                    text = "Pronto",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Column {
            Text(
                text = "Lavanderia",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
            Text(
                text = "Cesto panni & timer",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun FunctionLaundryWidget2x4(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFFEF4444) else colors.accent

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    imageVector = Icons.Outlined.LocalLaundryService,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "LAVANDERIA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
            }

            Text(
                text = "Gestione Panni",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
            Text(
                text = "Tocca per gestire il cesto e avviare il timer",
                fontSize = 11.5.sp,
                color = colors.textSecondary
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = tint.copy(alpha = if (colors.isDark) 0.20f else 0.12f),
            modifier = Modifier.size(width = 104.dp, height = 74.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Timer",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = tint
                )
            }
        }
    }
}

@Composable
fun FunctionTripWidget1x2(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFF8B5CF6) else colors.accent

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
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Work,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Valigia",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Organizza viaggio",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun FunctionTripWidget2x2(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFF8B5CF6) else colors.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Work,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column {
            Text(
                text = "Viaggio",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
            Text(
                text = "Prepara la valigia",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun FunctionTripWidget2x4(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFF8B5CF6) else colors.accent

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    imageVector = Icons.Outlined.FlightTakeoff,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ORGANIZZA VIAGGIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
            }
            Text(
                text = "Valigia & Partenze",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
            Text(
                text = "Seleziona i capi da portare con te",
                fontSize = 11.5.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun FunctionTransfersWidget1x2(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFFF97316) else colors.accent

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
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Trasferimenti",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = "Registro movimenti",
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FunctionTransfersWidget2x2(
    viewModel: ClosetViewModel,
    useDistinctColors: Boolean = viewModel.useDistinctFunctionColors
) {
    val colors = LocalOneUIColors.current
    val tint = if (useDistinctColors) Color(0xFFF97316) else colors.accent

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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = colors.surfaceSecondary
            ) {
                Text(
                    text = "Attivo",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Column {
            Text(
                text = "Trasferimenti",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
            Text(
                text = "Panni, Valigia & Spostamenti",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

// =========================================================================
// WIDGET UTILITÃ€ (TORCIA & BUSSOLA)
// =========================================================================

@Composable
fun UtilityTorchWidget1x1(isEditMode: Boolean) {
    val colors = LocalOneUIColors.current
    val (isTorchOn, toggleTorch) = rememberTorchState()
    val activeColor = Color(0xFFEAB308)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isEditMode) Modifier.clickable { toggleTorch() } else Modifier
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (isTorchOn) activeColor else colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOff,
                contentDescription = "Torcia",
                tint = if (isTorchOn) Color(0xFF1A1600) else colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun UtilityTorchWidget1x2(isEditMode: Boolean) {
    val colors = LocalOneUIColors.current
    val (isTorchOn, toggleTorch) = rememberTorchState()
    val activeColor = Color(0xFFEAB308)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isTorchOn) activeColor.copy(alpha = 0.14f) else Color.Transparent)
            .then(
                if (!isEditMode) Modifier.clickable { toggleTorch() } else Modifier
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isTorchOn) activeColor else colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOff,
                contentDescription = "Torcia",
                tint = if (isTorchOn) Color(0xFF1A1600) else colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column {
            Text(
                text = "Torcia",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = if (isTorchOn) "Accesa" else "Spenta",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isTorchOn) activeColor else colors.textSecondary
            )
        }
    }
}

@Composable
fun UtilityTorchWidget2x2(isEditMode: Boolean) {
    val colors = LocalOneUIColors.current
    val (isTorchOn, toggleTorch) = rememberTorchState()
    val activeColor = Color(0xFFEAB308)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isTorchOn) activeColor.copy(alpha = 0.14f) else Color.Transparent)
            .then(
                if (!isEditMode) Modifier.clickable { toggleTorch() } else Modifier
            )
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isTorchOn) activeColor else colors.surfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Outlined.FlashlightOff,
                    contentDescription = "Torcia",
                    tint = if (isTorchOn) Color(0xFF1A1600) else colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isTorchOn) activeColor else colors.surfaceSecondary
            ) {
                Text(
                    text = if (isTorchOn) "ON" else "OFF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isTorchOn) Color(0xFF1A1600) else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Column {
            Text(
                text = "Torcia LED",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
            Text(
                text = if (isTorchOn) "Tocca per spegnere" else "Tocca per illuminare",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

// =========================================================================
// MINI BUSSOLA GRAFICA DI PRECISIONE (WIDGET 1X1 E 1X2)
// =========================================================================

// =========================================================================
// MINI BUSSOLA GRAFICA DI PRECISIONE (WIDGET 1X1 E 1X2)
// =========================================================================

@Composable
fun CompassDialMini(
    reading: CompassReading,
    modifier: Modifier = Modifier,
    showCenterText: Boolean = true
) {
    val colors = LocalOneUIColors.current
    val (shortDir, _) = getCardinalLabel(reading.azimuthDegrees)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1. Base circolare del quadrante (identica all'orologio analogico)
            drawCircle(
                color = colors.surfaceSecondary,
                radius = radius,
                center = center
            )

            // 2. Tacca fissa di riferimento a ore 12 (direzione frontale del telefono)
            val topTickWidth = 2.dp.toPx()
            val topTickLength = 4.5.dp.toPx()
            drawLine(
                color = colors.accent,
                start = Offset(center.x, center.y - radius + 1.dp.toPx()),
                end = Offset(center.x, center.y - radius + 1.dp.toPx() + topTickLength),
                strokeWidth = topTickWidth,
                cap = StrokeCap.Round
            )

            // 3. Rosa dei venti e ago magnetico (ruotano con -azimuthDegrees per puntare al Nord fisico)
            rotate(degrees = -reading.azimuthDegrees, pivot = center) {
                // Tacche perimetrali ogni 30 gradi
                for (deg in 0 until 360 step 30) {
                    val isNorth = deg == 0
                    val isCardinal = deg % 90 == 0
                    val tickLen = when {
                        isNorth -> 6.5.dp.toPx()
                        isCardinal -> 5.dp.toPx()
                        else -> 3.dp.toPx()
                    }
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val startR = radius - 3.dp.toPx()
                    val endR = startR - tickLen
                    val start = Offset(
                        center.x + (startR * cos(rad)).toFloat(),
                        center.y + (startR * sin(rad)).toFloat()
                    )
                    val end = Offset(
                        center.x + (endR * cos(rad)).toFloat(),
                        center.y + (endR * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = when {
                            isNorth -> Color(0xFFE55353)
                            isCardinal -> colors.textPrimary
                            else -> colors.textMuted.copy(alpha = 0.45f)
                        },
                        start = start,
                        end = end,
                        strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Piccolo triangolo indicatore del Nord a 0 gradi
                val nPipTipY = center.y - radius + 10.5.dp.toPx()
                val nPipBaseY = nPipTipY + 3.5.dp.toPx()
                val nPipHalfW = 2.2.dp.toPx()
                val northPip = Path().apply {
                    moveTo(center.x, nPipTipY)
                    lineTo(center.x - nPipHalfW, nPipBaseY)
                    lineTo(center.x + nPipHalfW, nPipBaseY)
                    close()
                }
                drawPath(northPip, Color(0xFFE55353))

                // Ago magnetico sfaccettato 3D a doppia punta
                val needleNorthLength = radius * 0.74f
                val needleSouthLength = radius * 0.58f
                val needleHalfWidth = radius * 0.13f

                // Freccia Nord - Meta sinistra (rosso vivido)
                val northLeftPath = Path().apply {
                    moveTo(center.x, center.y - needleNorthLength)
                    lineTo(center.x - needleHalfWidth, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(northLeftPath, Color(0xFFEF4444))

                // Freccia Nord - Meta destra (rosso scuro per sfaccettatura 3D)
                val northRightPath = Path().apply {
                    moveTo(center.x, center.y - needleNorthLength)
                    lineTo(center.x + needleHalfWidth, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(northRightPath, Color(0xFFDC2626))

                // Freccia Sud - Meta sinistra (argento / grigio chiaro)
                val southLeftPath = Path().apply {
                    moveTo(center.x, center.y + needleSouthLength)
                    lineTo(center.x - needleHalfWidth, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(southLeftPath, if (colors.isDark) Color(0xFFCBD5E1) else Color(0xFF94A3B8))

                // Freccia Sud - Meta destra (ardesia per sfaccettatura 3D)
                val southRightPath = Path().apply {
                    moveTo(center.x, center.y + needleSouthLength)
                    lineTo(center.x + needleHalfWidth, center.y)
                    lineTo(center.x, center.y)
                    close()
                }
                drawPath(southRightPath, if (colors.isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            }

            // 4. Mozzo centrale
            val hubRadius = if (showCenterText) radius * 0.36f else radius * 0.18f
            drawCircle(
                color = if (showCenterText) {
                    if (colors.isDark) Color(0xFF191D26) else Color(0xFFFFFFFF)
                } else {
                    colors.surfaceSecondary
                },
                radius = hubRadius,
                center = center
            )
            drawCircle(
                color = colors.cardBorder,
                radius = hubRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            if (!showCenterText) {
                // Rubino centrale di perno se non c'e testo
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = radius * 0.08f,
                    center = center
                )
            }
        }

        // Direzione cardinale centrale fissa (senza gradi, pulita e minimale)
        if (showCenterText) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = shortDir,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (shortDir == "N") Color(0xFFE55353) else colors.accent,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}

@Composable
fun UtilityCompassWidget1x1() {
    val reading = rememberCompassReading()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        CompassDialMini(
            reading = reading,
            modifier = Modifier.fillMaxSize(),
            showCenterText = true
        )
    }
}

@Composable
fun UtilityCompassWidget1x2() {
    val colors = LocalOneUIColors.current
    val reading = rememberCompassReading()
    val (shortDir, fullDir) = getCardinalLabel(reading.azimuthDegrees)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mini Bussola grafica ad alta precisione
        CompassDialMini(
            reading = reading,
            modifier = Modifier.size(54.dp),
            showCenterText = false
        )

        // Dettagli e orientamento testuale One UI
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${reading.azimuthDegrees.roundToInt()}\u00B0",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (shortDir == "N") Color(0xFFE55353).copy(alpha = 0.15f) else colors.accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = shortDir,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (shortDir == "N") Color(0xFFE55353) else colors.accent,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = fullDir,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
@Composable
fun UtilityCompassWidget2x2() {
    val colors = LocalOneUIColors.current
    val reading = rememberCompassReading()
    val (shortDir, _) = getCardinalLabel(reading.azimuthDegrees)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = colors.surfaceSecondary,
                radius = radius,
                center = center
            )

            rotate(degrees = -reading.azimuthDegrees, pivot = center) {
                for (deg in 0 until 360 step 15) {
                    val isCardinal = deg % 90 == 0
                    val tickLen = if (isCardinal) 10.dp.toPx() else 5.dp.toPx()
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val startR = radius - 4.dp.toPx()
                    val endR = startR - tickLen
                    val start = Offset(
                        center.x + (startR * cos(rad)).toFloat(),
                        center.y + (startR * sin(rad)).toFloat()
                    )
                    val end = Offset(
                        center.x + (endR * cos(rad)).toFloat(),
                        center.y + (endR * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = if (deg == 0) Color(0xFFEF4444)
                        else if (isCardinal) colors.textPrimary
                        else colors.textMuted.copy(alpha = 0.5f),
                        start = start,
                        end = end,
                        strokeWidth = if (isCardinal) 2.2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = shortDir,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = if (shortDir == "N") Color(0xFFEF4444) else colors.accent,
                lineHeight = 20.sp
            )
            Text(
                text = "${reading.azimuthDegrees.roundToInt()}Â°",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
fun UtilityCompassWidget2x4() {
    val colors = LocalOneUIColors.current
    val reading = rememberCompassReading()
    val (shortDir, fullDir) = getCardinalLabel(reading.azimuthDegrees)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
        ) {
            UtilityCompassWidget2x2()
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "BUSSOLA",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent
            )
            Text(
                text = "${reading.azimuthDegrees.roundToInt()}Â° $shortDir",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                lineHeight = 28.sp
            )
            Text(
                text = fullDir,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Text(
                text = "Incl. ${reading.pitchDegrees.roundToInt()}° • Roll ${reading.rollDegrees.roundToInt()}°",
                fontSize = 10.5.sp,
                color = colors.textMuted
            )
        }
    }
}
