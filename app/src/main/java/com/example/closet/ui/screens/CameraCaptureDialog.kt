package com.example.closet.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

fun cropToSquare(context: Context, sourceUri: Uri): Uri {
    return try {
        val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return sourceUri
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val exifStream = context.contentResolver.openInputStream(sourceUri)
        val exif = exifStream?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL
        exifStream?.close()

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotatedBitmap != bitmap) bitmap.recycle()

        val edge = minOf(rotatedBitmap.width, rotatedBitmap.height)
        val xOffset = (rotatedBitmap.width - edge) / 2
        val yOffset = (rotatedBitmap.height - edge) / 2

        val rawSquare = Bitmap.createBitmap(rotatedBitmap, xOffset, yOffset, edge, edge)
        if (rawSquare != rotatedBitmap) rotatedBitmap.recycle()

        val maxEdge = 1080
        val finalSquare = if (edge > maxEdge) {
            Bitmap.createScaledBitmap(rawSquare, maxEdge, maxEdge, true).also {
                if (it != rawSquare) rawSquare.recycle()
            }
        } else {
            rawSquare
        }

        val squareFile = File.createTempFile("cropped_1_1_", ".jpg", context.cacheDir)
        val outStream = FileOutputStream(squareFile)
        finalSquare.compress(Bitmap.CompressFormat.JPEG, 88, outStream)
        outStream.flush()
        outStream.close()

        FileProvider.getUriForFile(context, "${context.packageName}.provider", squareFile)
    } catch (_: Exception) {
        sourceUri
    }
}

@Composable
fun OneUICameraCaptureDialog(
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

        var currentZoomRatio by remember { mutableFloatStateOf(1f) }
        var minZoomRatio by remember { mutableFloatStateOf(1f) }
        var maxZoomRatio by remember { mutableFloatStateOf(8f) }
        var isCapturing by remember { mutableStateOf(false) }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                    }
                    Text("Scatto 1:1", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(2.dp, colors.accent, RoundedCornerShape(26.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                cameraControl?.let { control ->
                                    val targetZoom = (currentZoomRatio * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                                    control.setZoomRatio(targetZoom)
                                }
                            }
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }.also { previewViewRef = it }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
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
                            listOf(0.6f, 1.0f, 2.0f).forEach { zoom ->
                                val isSupported = zoom >= minZoomRatio && zoom <= maxZoomRatio
                                val isCurrent = kotlin.math.abs(currentZoomRatio - zoom) < 0.25f
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) colors.accent else Color.Transparent)
                                        .clickable(enabled = isSupported) { cameraControl?.setZoomRatio(zoom) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (zoom == 0.6f) ".6" else "${zoom.toInt()}",
                                        color = if (isCurrent) Color.White else if (isSupported) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f),
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
                            .padding(horizontal = 36.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Outlined.FlipCameraAndroid, contentDescription = "Cambia fotocamera", tint = Color.White, modifier = Modifier.size(26.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(80.dp)
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
                                                    if (orientedBitmap != rawBitmap) rawBitmap.recycle()

                                                    val edge = minOf(orientedBitmap.width, orientedBitmap.height)
                                                    val xOffset = (orientedBitmap.width - edge) / 2
                                                    val yOffset = (orientedBitmap.height - edge) / 2
                                                    val squareBitmap = Bitmap.createBitmap(orientedBitmap, xOffset, yOffset, edge, edge)
                                                    if (squareBitmap != orientedBitmap) orientedBitmap.recycle()

                                                    val maxEdge = 1080
                                                    val finalSquare = if (edge > maxEdge) {
                                                        Bitmap.createScaledBitmap(squareBitmap, maxEdge, maxEdge, true).also {
                                                            if (it != squareBitmap) squareBitmap.recycle()
                                                        }
                                                    } else {
                                                        squareBitmap
                                                    }

                                                    val tempFile = File.createTempFile("photo_1_1_", ".jpg", context.cacheDir)
                                                    val outStream = FileOutputStream(tempFile)
                                                    finalSquare.compress(Bitmap.CompressFormat.JPEG, 88, outStream)
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

                        Spacer(modifier = Modifier.size(52.dp))
                    }
                }
            }
        }
    }
}
