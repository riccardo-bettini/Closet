package com.example.closet.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClothingGridCard(
    item: ClothingItem,
    isEditMode: Boolean,
    isInsideFolder: Boolean = false,
    isSelected: Boolean = false,
    selectedQuantity: Int = item.quantity,
    isDragging: Boolean = false,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {},
    onIncrementSelected: () -> Unit = {},
    onDecrementSelected: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit = {},
    onEditClick: () -> Unit
) {
    val colors = LocalOneUIColors.current

    val subtitle = remember(item.folderName, item.tags, isInsideFolder, item.isLaundryInTransit) {
        if (item.isLaundryInTransit) {
            "In arrivo (Panni)"
        } else {
            val folderPrefix = if (!isInsideFolder && item.folderName != null) "📁 ${item.folderName}" else null
            val tagsText = if (item.tags.isNotEmpty()) item.tags.joinToString(" • ") { it } else null
            listOfNotNull(folderPrefix, tagsText).ifEmpty { listOf("Nessun tag") }.joinToString(" • ")
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(
            width = if (isSelected || isDragging) 2.dp else if (isEditMode) 1.dp else 0.5.dp,
            color = if (isSelected || isDragging) colors.accent else if (item.isLaundryInTransit) colors.accent.copy(alpha = 0.5f) else colors.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (item.isLaundryInTransit) Modifier.alpha(0.38f) else Modifier)
            .then(
                if (isEditMode && !item.isLaundryInTransit) {
                    Modifier.clickable(onClick = onToggleSelect)
                } else if (!isEditMode && !item.isLaundryInTransit) {
                    Modifier.pointerInput(item.id) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { }
                        )
                    }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUri != null) {
                    Image(
                        painter = rememberOptimizedPainter(item.imageUri, 190),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!item.emoji.isNullOrBlank()) {
                    Text(normalizeEmoji(item.emoji) ?: item.emoji.orEmpty(), fontSize = 42.sp)
                } else {
                    Icon(Icons.Default.Checkroom, contentDescription = null, tint = colors.accent, modifier = Modifier.size(40.dp))
                }

                if (item.isLaundryInTransit) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.accent)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("IN TRANSITO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isEditMode && !item.isLaundryInTransit,
                    enter = fadeIn(tween(220)) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.accent else Color.Black.copy(alpha = 0.45f))
                            .border(1.5.dp, if (isSelected) colors.accent else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Pallino contatore in alto a destra durante la modalità gestione elementi: diventa selettore quantità se l'elemento è selezionato
                androidx.compose.animation.AnimatedVisibility(
                    visible = isEditMode && item.isCountable && !item.isLaundryInTransit,
                    enter = fadeIn(tween(220)) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    AnimatedContent(
                        targetState = isSelected,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.85f))
                                .togetherWith(fadeOut(tween(130)) + scaleOut(targetScale = 0.85f))
                        },
                        label = "counterTopEndMorph"
                    ) { selected ->
                        if (selected) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.85f),
                                border = BorderStroke(1.2.dp, colors.accent),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (selectedQuantity > 1) colors.accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f))
                                            .clickable(enabled = selectedQuantity > 1) { onDecrementSelected() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "-",
                                            color = if (selectedQuantity > 1) Color.White else Color.White.copy(alpha = 0.35f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Text(
                                        text = if (selectedQuantity < item.quantity) "$selectedQuantity/${item.quantity}" else "$selectedQuantity",
                                        color = colors.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (selectedQuantity < item.quantity) colors.accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f))
                                            .clickable(enabled = selectedQuantity < item.quantity) { onIncrementSelected() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+",
                                            color = if (selectedQuantity < item.quantity) Color.White else Color.White.copy(alpha = 0.35f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .height(24.dp)
                                    .widthIn(min = 24.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = "${item.quantity}",
                                        color = colors.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                if (item.isCountable && !isEditMode && !item.isLaundryInTransit) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { onDecrement() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                            Text(
                                text = "${item.quantity}",
                                color = colors.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { onIncrement() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isEditMode && !item.isLaundryInTransit,
                    enter = fadeIn(tween(220)) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accent,
                        modifier = Modifier.clickable { onEditClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("Modifica", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (item.isLaundryInTransit) colors.accent else colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
fun ClothingListCard(
    item: ClothingItem,
    isEditMode: Boolean,
    isInsideFolder: Boolean = false,
    isSelected: Boolean = false,
    selectedQuantity: Int = item.quantity,
    isDragging: Boolean = false,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {},
    onIncrementSelected: () -> Unit = {},
    onDecrementSelected: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit = {},
    onEditClick: () -> Unit
) {
    val colors = LocalOneUIColors.current

    val subtitle = remember(item.folderName, item.tags, isInsideFolder, item.isLaundryInTransit) {
        if (item.isLaundryInTransit) {
            "In arrivo (Panni)"
        } else {
            val folderPrefix = if (!isInsideFolder && item.folderName != null) "📁 ${item.folderName}" else null
            val tagsText = if (item.tags.isNotEmpty()) item.tags.joinToString(" ") { it } else null
            listOfNotNull(folderPrefix, tagsText).ifEmpty { listOf("Nessun tag") }.joinToString(" • ")
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        border = BorderStroke(
            width = if (isSelected || isDragging) 2.dp else if (isEditMode) 1.dp else 0.5.dp,
            color = if (isSelected || isDragging) colors.accent else if (item.isLaundryInTransit) colors.accent.copy(alpha = 0.5f) else colors.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (item.isLaundryInTransit) Modifier.alpha(0.38f) else Modifier)
            .then(
                if (isEditMode && !item.isLaundryInTransit) {
                    Modifier.clickable(onClick = onToggleSelect)
                } else if (!isEditMode && !item.isLaundryInTransit) {
                    Modifier.pointerInput(item.id) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { }
                        )
                    }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isEditMode && !item.isLaundryInTransit,
                    enter = fadeIn(tween(220)) + expandHorizontally(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = fadeOut(tween(160)) + shrinkHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .border(1.5.dp, if (isSelected) colors.accent else colors.textSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.imageUri != null) {
                            Image(
                                painter = rememberOptimizedPainter(item.imageUri, 112),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!item.emoji.isNullOrBlank()) {
                            Text(normalizeEmoji(item.emoji) ?: item.emoji.orEmpty(), fontSize = 28.sp)
                        } else {
                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEditMode && item.isCountable && !item.isLaundryInTransit,
                        enter = fadeIn(tween(220)) + scaleIn(
                            initialScale = 0.7f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) colors.accent else Color.Black.copy(alpha = 0.82f),
                            border = BorderStroke(1.2.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .height(20.dp)
                                .widthIn(min = 20.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 5.dp)
                            ) {
                                Text(
                                    text = if (isSelected) {
                                        if (selectedQuantity < item.quantity) "$selectedQuantity/${item.quantity}" else "$selectedQuantity"
                                    } else {
                                        "${item.quantity}"
                                    },
                                    color = if (isSelected) Color.White else colors.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = if (item.isLaundryInTransit) colors.accent else colors.textSecondary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (item.isCountable && !isEditMode && !item.isLaundryInTransit) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceSecondary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(onClick = onDecrement, modifier = Modifier.size(26.dp)) {
                                Text("-", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                            Text(
                                text = "${item.quantity}",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(onClick = onIncrement, modifier = Modifier.size(26.dp)) {
                                Text("+", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isEditMode && !item.isLaundryInTransit,
                    enter = fadeIn(tween(220)) + scaleIn(
                        initialScale = 0.75f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.75f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelected && item.isCountable) {
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(onClick = onDecrementSelected, modifier = Modifier.size(26.dp)) {
                                        Text("-", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                    Text(
                                        text = if (selectedQuantity < item.quantity) "$selectedQuantity/${item.quantity}" else "$selectedQuantity",
                                        color = colors.accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(onClick = onIncrementSelected, modifier = Modifier.size(26.dp)) {
                                        Text("+", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = colors.accent,
                            modifier = Modifier
                                .height(34.dp)
                                .clickable(onClick = onEditClick)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Modifica", tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Modifica", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderGridCard(
    folderName: String,
    itemCount: Int,
    coverUri: Uri?,
    coverEmoji: String? = null,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { }
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                if (coverUri != null) {
                    Image(
                        painter = rememberOptimizedPainter(coverUri, 190),
                        contentDescription = folderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!coverEmoji.isNullOrBlank()) {
                    Text(normalizeEmoji(coverEmoji) ?: coverEmoji.orEmpty(), fontSize = 42.sp)
                } else {
                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.accent, modifier = Modifier.size(44.dp))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.accent, modifier = Modifier.size(12.dp))
                        Text("Cartella", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isEditMode,
                    enter = fadeIn(tween(220)) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accent,
                        modifier = Modifier.clickable { onEditClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("Modifica", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = folderName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$itemCount capi",
                    fontSize = 11.sp,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun FolderListCard(
    folderName: String,
    itemCount: Int,
    coverUri: Uri?,
    coverEmoji: String? = null,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUri != null) {
                        Image(
                            painter = rememberOptimizedPainter(coverUri, 112),
                            contentDescription = folderName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!coverEmoji.isNullOrBlank()) {
                        Text(normalizeEmoji(coverEmoji) ?: coverEmoji.orEmpty(), fontSize = 28.sp)
                    } else {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
                    }
                }

                Column {
                    Text(
                        text = folderName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = "$itemCount capi (Cartella)",
                        fontSize = 12.sp,
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn(tween(220)) + scaleIn(
                    initialScale = 0.75f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.75f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.accent,
                    modifier = Modifier
                        .height(34.dp)
                        .clickable(onClick = onEditClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modifica", tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Modifica", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
