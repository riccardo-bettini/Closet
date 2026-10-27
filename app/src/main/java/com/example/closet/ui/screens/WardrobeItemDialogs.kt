@file:OptIn(ExperimentalLayoutApi::class)

package com.example.closet.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.closet.ui.viewmodel.ClosetFolder
import com.example.closet.ui.viewmodel.ClosetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmojiPickerDialog(
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    val colors = LocalOneUIColors.current
    val emojis = remember {
        listOf(
            "ðŸ‘•", "ðŸ‘”", "ðŸ‘—", "ðŸ§¥", "ðŸ©²", "ðŸ©³", "ðŸ‘–", "ðŸ‘š", "ðŸ¥¼", "ðŸ¥»", "ðŸ‘˜", "ðŸŽ½", "ðŸ¦º", "ðŸ¥‹", "ðŸ¤µ", "ðŸ‘°", "ðŸ‘™", "ðŸ©±",
            "ðŸ‘Ÿ", "ðŸ‘ž", "ðŸ¥¾", "ðŸ‘¡", "ðŸ‘ ", "ðŸ‘¢", "ðŸ¥¿", "ðŸ©°", "ðŸ©´", "ðŸ§¦", "â›¸ï¸", "ðŸŽ¿",
            "ðŸ§¢", "ðŸ‘’", "ðŸŽ©", "ðŸŽ“", "ðŸ‘‘", "â›‘ï¸", "ðŸª–", "ðŸ§£", "ðŸ§¤", "ðŸ•¶ï¸", "ðŸ‘“", "ðŸŒ‚", "â˜‚ï¸", "ðŸ‘œ", "ðŸŽ’", "ðŸ‘›", "ðŸ’¼", "ðŸ§³",
            "ðŸ’", "ðŸ’Ž", "âŒš", "ðŸ•°ï¸", "ðŸª¡", "ðŸ§µ", "ðŸ§¶", "ðŸ§·", "ðŸ·ï¸", "ðŸŽ€",
            "â˜€ï¸", "â„ï¸", "ðŸŒ§ï¸", "â›…", "ðŸŒŠ", "ðŸ‚", "ðŸŒ¸", "âš¡", "âœ¨", "ðŸ”¥", "â­", "ðŸ–¤", "ðŸ¤", "ðŸ§¸", "ðŸ’¯", "ðŸŽ¯", "ðŸš€", "ðŸ†"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = colors.dialogBackground,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scegli Icona / Emoji",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    items(emojis, key = { it }) { emoji ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceSecondary,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    onEmojiSelected(emoji)
                                    onDismiss()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 26.sp)
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Chiudi", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// Dialog con Morphing per la Modifica della Cartella
@Composable
fun FolderEditDialog(
    folder: ClosetFolder,
    onDismiss: () -> Unit,
    onCameraActiveChange: (Boolean) -> Unit,
    onSave: (ClosetFolder) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current
    var name by remember { mutableStateOf(folder.folderName) }
    var coverUri by remember { mutableStateOf<Uri?>(folder.coverImageUri) }
    var coverEmoji by remember { mutableStateOf(folder.coverEmoji ?: "") }
    var showCustomCamera by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "folderEditMorphScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "folderEditMorphAlpha"
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showCustomCamera = true
            onCameraActiveChange(true)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            val square = cropToSquare(context, uri)
            coverUri = square
            coverEmoji = ""
        }
    }

    if (showCustomCamera) {
        OneUICameraCaptureDialog(
            onDismiss = {
                showCustomCamera = false
                onCameraActiveChange(false)
            },
            onPhotoCaptured = { uri ->
                coverUri = uri
                coverEmoji = ""
                showCustomCamera = false
                onCameraActiveChange(false)
            }
        )
    }

    if (showEmojiPicker) {
        EmojiPickerDialog(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { selectedEmoji ->
                coverEmoji = selectedEmoji
                coverUri = null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.dialogBackground,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Modifica Cartella",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surfaceSecondary)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverUri != null) {
                            Image(painter = rememberOptimizedPainter(coverUri, 172), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else if (coverEmoji.isNotBlank()) {
                            Text(coverEmoji, fontSize = 38.sp)
                        } else {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.accent, modifier = Modifier.size(36.dp))
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable {
                                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            showCustomCamera = true
                                            onCameraActiveChange(true)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Camera", tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Galleria", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { showEmojiPicker = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emoji", tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (coverUri != null || coverEmoji.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE55353).copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, Color(0xFFE55353).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clickable { coverUri = null; coverEmoji = "" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFE55353), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rimuovi icona", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE55353))
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (name.isEmpty()) { Text("Nome cartella...", color = colors.textSecondary, fontSize = 14.sp) }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = ComposeTextStyle(fontSize = 14.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(colors.accent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Annulla", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            val clean = name.trim()
                            if (clean.isNotBlank()) {
                                onSave(folder.copy(folderName = clean, coverImageUri = coverUri, coverEmoji = coverEmoji.ifBlank { null }))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Salva", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dialog con Morphing per la Creazione della Cartella
@Composable
fun FolderCreateDialog(
    selectedCloset: String,
    selectedItemIds: MutableList<String>,
    onDismiss: () -> Unit,
    onCameraActiveChange: (Boolean) -> Unit,
    onSave: (String, Uri?, String?) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current
    var name by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var coverEmoji by remember { mutableStateOf("") }
    var showCustomCamera by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "folderCreateMorphScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "folderCreateMorphAlpha"
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showCustomCamera = true
            onCameraActiveChange(true)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            val square = cropToSquare(context, uri)
            coverUri = square
            coverEmoji = ""
        }
    }

    if (showCustomCamera) {
        OneUICameraCaptureDialog(
            onDismiss = {
                showCustomCamera = false
                onCameraActiveChange(false)
            },
            onPhotoCaptured = { uri ->
                coverUri = uri
                coverEmoji = ""
                showCustomCamera = false
                onCameraActiveChange(false)
            }
        )
    }

    if (showEmojiPicker) {
        EmojiPickerDialog(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { selectedEmoji ->
                coverEmoji = selectedEmoji
                coverUri = null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.dialogBackground,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Nuova Cartella",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surfaceSecondary)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverUri != null) {
                            Image(painter = rememberOptimizedPainter(coverUri, 172), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else if (coverEmoji.isNotBlank()) {
                            Text(coverEmoji, fontSize = 38.sp)
                        } else {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = colors.accent, modifier = Modifier.size(36.dp))
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable {
                                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            showCustomCamera = true
                                            onCameraActiveChange(true)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Camera", tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Galleria", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { showEmojiPicker = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emoji", tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (coverUri != null || coverEmoji.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE55353).copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, Color(0xFFE55353).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clickable { coverUri = null; coverEmoji = "" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFE55353), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rimuovi icona", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE55353))
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (name.isEmpty()) { Text("Nome cartella (es. Felpe)", color = colors.textSecondary, fontSize = 14.sp) }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = ComposeTextStyle(fontSize = 14.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(colors.accent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedItemIds.isNotEmpty()) {
                    Text("${selectedItemIds.size} capi selezionati verranno raggruppati qui.", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Annulla", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            val clean = name.trim()
                            if (clean.isNotBlank()) {
                                onSave(clean, coverUri, coverEmoji.ifBlank { null })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Crea", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dialog con Morphing per la Creazione e Modifica dei Capi
@Composable
fun AddOrEditClothingDialog(
    viewModel: ClosetViewModel,
    availableClosets: List<String>,
    initialCloset: String,
    initialFolder: String? = null,
    itemToEdit: ClothingItem? = null,
    onDismiss: () -> Unit,
    onCameraActiveChange: (Boolean) -> Unit = {},
    onSave: (ClothingItem) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "clothingMorphScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "clothingMorphAlpha"
    )

    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var selectedTags by remember { mutableStateOf(itemToEdit?.tags?.toSet() ?: emptySet()) }
    var selectedCloset by remember { mutableStateOf(itemToEdit?.closetName ?: initialCloset) }
    var selectedFolder by remember { mutableStateOf<String?>(itemToEdit?.folderName ?: initialFolder) }
    var showClosetDropdown by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(itemToEdit?.imageUri) }
    var emojiInput by remember { mutableStateOf(itemToEdit?.emoji ?: "") }

    var isCountable by remember { mutableStateOf(itemToEdit?.isCountable ?: false) }
    var quantity by remember { mutableIntStateOf(itemToEdit?.quantity ?: 1) }

    var showCustomCamera by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showCustomCamera = true
            onCameraActiveChange(true)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val squareUri = cropToSquare(context, uri)
                selectedImageUri = viewModel.savePermanentImage(squareUri)
                emojiInput = ""
            }
        }
    }

    if (showCustomCamera) {
        OneUICameraCaptureDialog(
            onDismiss = {
                showCustomCamera = false
                onCameraActiveChange(false)
            },
            onPhotoCaptured = { uri ->
                coroutineScope.launch {
                    selectedImageUri = viewModel.savePermanentImage(uri)
                    emojiInput = ""
                    showCustomCamera = false
                    onCameraActiveChange(false)
                }
            }
        )
    }

    if (showEmojiPicker) {
        EmojiPickerDialog(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { selectedEmoji ->
                emojiInput = selectedEmoji
                selectedImageUri = null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.dialogBackground,
            border = BorderStroke(0.5.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (itemToEdit != null) "Modifica Capo" else "Nuovo Capo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tipo di elemento", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceSecondary)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isCountable) colors.accent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isCountable = false }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text("Singolo", color = if (!isCountable) Color.White else colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCountable) colors.accent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isCountable = true }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text("Con Contatore", color = if (isCountable) Color.White else colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isCountable) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surfaceSecondary)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("QuantitÃ  disponibile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                Text("Pezzi registrati", fontSize = 11.sp, color = colors.textSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(colors.cardBackground)
                                ) { Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center) }

                                Text(text = "$quantity", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.accent, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)

                                IconButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(colors.cardBackground)
                                ) { Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center) }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surfaceSecondary)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            Image(painter = rememberOptimizedPainter(selectedImageUri, 172), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else if (emojiInput.isNotBlank()) {
                            Text(emojiInput, fontSize = 38.sp)
                        } else {
                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(36.dp))
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable {
                                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            showCustomCamera = true
                                            onCameraActiveChange(true)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Camera", tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Galleria", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { showEmojiPicker = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emoji", tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (selectedImageUri != null || emojiInput.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE55353).copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, Color(0xFFE55353).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clickable { selectedImageUri = null; emojiInput = "" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFE55353), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rimuovi icona", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE55353))
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceSecondary,
                    border = BorderStroke(1.dp, colors.cardBorder.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        if (name.isEmpty()) {
                            Text(
                                text = "Nome capo (es. T-shirt nera)",
                                color = colors.textSecondary.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            textStyle = ComposeTextStyle(
                                fontSize = 14.sp,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Destinazione Armadio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { showClosetDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
                                    }
                                    Text(selectedCloset, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.textSecondary)
                            }
                        }
                        DropdownMenu(
                            expanded = showClosetDropdown,
                            onDismissRequest = { showClosetDropdown = false },
                            modifier = Modifier.background(colors.dialogBackground),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            availableClosets.forEach { closet ->
                                DropdownMenuItem(
                                    text = { Text(closet, color = if (closet == selectedCloset) colors.accent else colors.textPrimary) },
                                    trailingIcon = { if (closet == selectedCloset) Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp)) },
                                    onClick = { selectedCloset = closet; selectedFolder = null; showClosetDropdown = false }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tag associati", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        Text("${selectedTags.size} selezionati", fontSize = 11.sp, color = colors.textSecondary)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        viewModel.availableTags.forEach { tag ->
                            val isSelected = selectedTags.contains(tag)
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) colors.accent else colors.surfaceSecondary,
                                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.cardBorder),
                                modifier = Modifier.clickable { selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text(tag, color = if (isSelected) Color.White else colors.textPrimary, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Annulla", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            onSave(
                                ClothingItem(
                                    id = itemToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = name.ifBlank { if (isCountable) "Stock Capi" else "Nuovo capo" },
                                    tags = selectedTags.toList(),
                                    closetName = selectedCloset,
                                    folderName = selectedFolder,
                                    isCountable = isCountable,
                                    quantity = if (isCountable) quantity.coerceAtLeast(1) else 1,
                                    color = Color.Transparent,
                                    imageUri = selectedImageUri,
                                    emoji = emojiInput.ifBlank { null }
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Salva", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Popup dedicato al singolo elemento con animazione fluida di apertura e chiusura
@Composable
fun ClothingDetailDialog(
    item: ClothingItem,
    onDismiss: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditClick: () -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var isVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun triggerClose(afterClose: () -> Unit) {
        if (isClosing) return
        isClosing = true
        isVisible = false
        coroutineScope.launch {
            delay(190)
            afterClose()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = if (isVisible) {
            spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            tween(durationMillis = 185)
        },
        label = "detailDialogScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (isVisible) 210 else 180),
        label = "detailDialogAlpha"
    )

    val offsetYPx by animateFloatAsState(
        targetValue = if (isVisible) 0f else with(density) { 22.dp.toPx() },
        animationSpec = if (isVisible) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            tween(durationMillis = 185)
        },
        label = "detailDialogOffsetY"
    )

    Dialog(
        onDismissRequest = { triggerClose(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    triggerClose(onDismiss)
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = colors.dialogBackground,
                border = BorderStroke(0.8.dp, colors.cardBorder),
                tonalElevation = 10.dp,
                shadowElevation = 28.dp,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .heightIn(max = 700.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        translationY = offsetYPx
                    }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Consuma il click per non chiudere toccando la card */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    // Immagine o Emoji in grande a schermo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surfaceSecondary)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.imageUri != null) {
                            Image(
                                painter = rememberOptimizedPainter(item.imageUri, 380),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!item.emoji.isNullOrBlank()) {
                            Text(
                                text = item.emoji,
                                fontSize = 96.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(88.dp)
                            )
                        }
                    }

                    // Nome dell'elemento e posizione (Armadio / Cartella)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                            lineHeight = 27.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceSecondary,
                                border = BorderStroke(0.8.dp, colors.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = item.closetName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            if (!item.folderName.isNullOrBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.surfaceSecondary,
                                    border = BorderStroke(0.8.dp, colors.cardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = item.folderName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sezione Contatore (solo se il capo è un elemento contatore)
                    if (item.isCountable) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(0.8.dp, colors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Quantità disponibile",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.cardBackground,
                                        border = BorderStroke(1.dp, colors.cardBorder),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { onDecrement() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "-",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${item.quantity}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.accent,
                                        modifier = Modifier.widthIn(min = 22.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = colors.cardBackground,
                                        border = BorderStroke(1.dp, colors.cardBorder),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { onIncrement() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "+",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sezione Tag associati
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tag associati",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                            if (item.tags.isNotEmpty()) {
                                Text(
                                    text = "${item.tags.size} tag",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (item.tags.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceSecondary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nessun tag associato a questo elemento",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item.tags.forEach { tag ->
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.accentContainer,
                                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Label,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = tag,
                                                color = colors.accent,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Pulsanti di azione in basso: Chiudi e Modifica
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { triggerClose(onDismiss) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Chiudi",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = { triggerClose(onEditClick) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Modifica",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}