package com.example.closet.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.example.closet.ui.viewmodel.ClosetViewModel
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetPhotosScreen(
    viewModel: ClosetViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOneUIColors.current
    val closets = viewModel.availableClosets
    val screenAccent = if (viewModel.useDistinctFunctionColors) Color(0xFF53B5A2) else colors.accent

    var selectedClosetForAction by remember { mutableStateOf<String?>(null) }
    var photoViewerCloset by remember { mutableStateOf<String?>(null) }
    var showFullCamera by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showFullCamera = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null && selectedClosetForAction != null) {
            val closet = selectedClosetForAction!!
            selectedClosetForAction = null
            coroutineScope.launch {
                val savedUri = viewModel.savePermanentImage(uri)
                viewModel.setClosetPhoto(closet, savedUri)
            }
        }
    }

    BackHandler(enabled = photoViewerCloset != null) {
        photoViewerCloset = null
    }

    if (showFullCamera && selectedClosetForAction != null) {
        FullFormatCameraDialog(
            onDismiss = { showFullCamera = false },
            onPhotoCaptured = { uri ->
                val closet = selectedClosetForAction!!
                showFullCamera = false
                selectedClosetForAction = null
                coroutineScope.launch {
                    val savedUri = viewModel.savePermanentImage(uri)
                    viewModel.setClosetPhoto(closet, savedUri)
                }
            }
        )
    }

    if (photoViewerCloset != null) {
        val targetCloset = photoViewerCloset!!
        val photoUri = viewModel.closetPhotos[targetCloset]
        if (photoUri != null) {
            ClosetPhotoViewerDialog(
                closetName = targetCloset,
                photoUri = photoUri,
                onExit = { photoViewerCloset = null },
                onDelete = {
                    viewModel.setClosetPhoto(targetCloset, null)
                    photoViewerCloset = null
                }
            )
        } else {
            photoViewerCloset = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 100.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            tint = screenAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Foto armadio",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )

                // Spazio segnaposto riservato al tasto Info (40.dp) per allineamento e bilanciamento perfetto
                Spacer(modifier = Modifier.width(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(closets, key = { it }) { closetName ->
                    val photoUri = viewModel.closetPhotos[closetName]

                    ClosetPhotoGridCard(
                        closetName = closetName,
                        photoUri = photoUri,
                        onClick = {
                            if (photoUri != null) {
                                photoViewerCloset = closetName
                            } else {
                                selectedClosetForAction = closetName
                            }
                        }
                    )
                }
            }
        }
    }

    if (selectedClosetForAction != null && !showFullCamera) {
        val activeCloset = selectedClosetForAction!!

        ModalBottomSheet(
            onDismissRequest = { selectedClosetForAction = null },
            containerColor = colors.dialogBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(colors.textSecondary.copy(alpha = 0.3f), CircleShape)
                )
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = activeCloset,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Aggiungi una foto panoramica per questo armadio",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                showFullCamera = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.accentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Scatta foto", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Interfaccia fotocamera completa", color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Scegli dalla galleria", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Carica un'immagine salvata", color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClosetPhotoGridCard(
    closetName: String,
    photoUri: Uri?,
    onClick: () -> Unit
) {
    val colors = LocalOneUIColors.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, if (photoUri != null) colors.cardBorder else colors.accent.copy(alpha = 0.4f)),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                if (photoUri != null) {
                    Image(
                        painter = rememberOptimizedPainter(photoUri, 220),
                        contentDescription = closetName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.accentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                        }
                        Text("Aggiungi foto", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = closetName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = if (photoUri != null) "Tocca per visualizzare" else "Nessuna foto",
                    fontSize = 11.sp,
                    color = if (photoUri != null) colors.accent else colors.textMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// --- VISUALIZZATORE FOTO A SCHERMO INTERO ---

@Composable
fun ClosetPhotoViewerDialog(
    closetName: String,
    photoUri: Uri,
    onExit: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090A0D))
        ) {
            Image(
                painter = rememberAsyncImagePainter(photoUri),
                contentDescription = closetName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Esci",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = closetName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.size(42.dp))
            }

            Surface(
                shape = CircleShape,
                color = Color(0xFF181B22).copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 28.dp)
                    .height(58.dp)
                    .wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .height(42.dp)
                            .clickable { onExit() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Esci",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Esci",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE55353).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFFE55353).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .height(42.dp)
                            .clickable { showConfirmDeleteDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Elimina foto",
                                tint = Color(0xFFE55353),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Elimina foto",
                                color = Color(0xFFE55353),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (showConfirmDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDeleteDialog = false },
                containerColor = Color(0xFF1E222A),
                shape = RoundedCornerShape(26.dp),
                title = {
                    Text("Eliminare la foto?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("La foto panoramica di \"$closetName\" verrà rimossa in modo permanente.", color = Color(0xFF8F949F), fontSize = 14.sp)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDeleteDialog = false
                            onDelete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE55353)),
                        shape = CircleShape
                    ) {
                        Text("Elimina", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDeleteDialog = false }) {
                        Text("Annulla", color = Color.White)
                    }
                }
            )
        }
    }
}

// --- FOTOCAMERA AVANZATA (UI SAMSUNG CAMERA STYLE) ---

@Composable
fun FullFormatCameraDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val colors = LocalOneUIColors.current

        var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
        var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
        var isCapturing by remember { mutableStateOf(false) }

        var currentZoomRatio by remember { mutableFloatStateOf(1f) }
        var minZoomRatio by remember { mutableFloatStateOf(1f) }
        var maxZoomRatio by remember { mutableFloatStateOf(8f) }

        var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
        var focusPoint by remember { mutableStateOf<Offset?>(null) }
        var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

        DisposableEffect(lifecycleOwner) {
            onDispose {
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    cameraProvider.unbindAll()
                } catch (_: Exception) {}
            }
        }

        LaunchedEffect(lensFacing, previewViewRef) {
            val pView = previewViewRef ?: return@LaunchedEffect
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(pView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(flashMode)
                        .build()
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                    imageCapture = capture
                    cameraControl = camera.cameraControl

                    camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                        zoomState?.let {
                            currentZoomRatio = it.zoomRatio
                            minZoomRatio = it.minZoomRatio
                            maxZoomRatio = it.maxZoomRatio
                        }
                    }
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(context))
        }

        LaunchedEffect(focusPoint) {
            if (focusPoint != null) {
                delay(1800)
                focusPoint = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { previewViewRef = it }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            cameraControl?.let { control ->
                                val targetZoom = (currentZoomRatio * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                                control.setZoomRatio(targetZoom)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val pView = previewViewRef ?: return@detectTapGestures
                            val factory = pView.meteringPointFactory
                            val point = factory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            cameraControl?.startFocusAndMetering(action)
                            focusPoint = offset
                        }
                    }
            )

            if (focusPoint != null) {
                val pt = focusPoint!!
                Box(
                    modifier = Modifier
                        .offset(x = with(LocalDensity.current) { (pt.x - 28.dp.toPx()).toDp() }, y = with(LocalDensity.current) { (pt.y - 28.dp.toPx()).toDp() })
                        .size(56.dp)
                        .border(2.dp, colors.accent, CircleShape)
                )
            }

            if (isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.65f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.clickable {
                            val nextFlash = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            flashMode = nextFlash
                            imageCapture?.flashMode = nextFlash
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when (flashMode) {
                                    ImageCapture.FLASH_MODE_ON -> "⚡ ON"
                                    ImageCapture.FLASH_MODE_AUTO -> "⚡ AUTO"
                                    else -> "⚡ OFF"
                                } ,
                                color = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White.copy(alpha = 0.7f) else colors.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "FULL",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val zoomOptions = if (minZoomRatio <= 0.7f) {
                                listOf(0.6f, 1.0f, 2.0f)
                            } else {
                                listOf(1.0f, 2.0f, 3.0f)
                            }

                            zoomOptions.forEach { z ->
                                val isSupported = z >= minZoomRatio && z <= maxZoomRatio
                                val isCurrent = kotlin.math.abs(currentZoomRatio - z) < 0.25f
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) colors.accent else Color.Transparent)
                                        .clickable(enabled = isSupported) { cameraControl?.setZoomRatio(z) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (z == 0.6f) ".6" else "${z.toInt()}",
                                        color = if (isCurrent) Color.White else if (isSupported) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.35f),
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.size(52.dp))

                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .clickable(enabled = !isCapturing) {
                                    val capture = imageCapture ?: return@clickable
                                    isCapturing = true

                                    capture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                try {
                                                    val buffer: ByteBuffer = image.planes[0].buffer
                                                    val bytes = ByteArray(buffer.remaining())
                                                    buffer.get(bytes)
                                                    val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                                                     val rotationDegrees = image.imageInfo.rotationDegrees
                                                    val matrix = Matrix().apply {
                                                        if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
                                                        if (lensFacing == CameraSelector.LENS_FACING_FRONT) postScale(-1f, 1f)
                                                    }
                                                    val orientedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)

                                                    val tempFile = File.createTempFile("closet_full_", ".jpg", context.cacheDir)
                                                    val outStream = FileOutputStream(tempFile)
                                                    orientedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, outStream)
                                                    outStream.flush()
                                                    outStream.close()

                                                    onPhotoCaptured(Uri.fromFile(tempFile))
                                                } catch (_: Exception) {
                                                    isCapturing = false
                                                } finally {
                                                    image.close()
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isCapturing = false
                                            }
                                        }
                                    )
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isCapturing) colors.accent else Color.White)
                            )
                        }

                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Outlined.FlipCameraAndroid, contentDescription = "Cambia fotocamera", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
        }
    }
}
