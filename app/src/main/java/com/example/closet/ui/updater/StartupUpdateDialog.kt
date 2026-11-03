package com.example.closet.ui.updater

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.closet.ui.screens.LocalOneUIColors
import kotlinx.coroutines.launch

@Composable
fun StartupUpdateDialog(
    release: GitHubReleaseInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalOneUIColors.current

    var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }

    fun startDownload() {
        updateState = UpdateUiState.Downloading(0f, 0f, 0f)
        coroutineScope.launch {
            val result = AppUpdateManager.downloadApk(
                context = context,
                release = release,
                onProgress = { progress, downloaded, total ->
                    updateState = UpdateUiState.Downloading(progress, downloaded, total)
                }
            )
            result.onSuccess { apkFile ->
                updateState = UpdateUiState.ReadyToInstall(apkFile)
                AppUpdateManager.startInstallApk(context, apkFile)
            }.onFailure { err ->
                updateState = UpdateUiState.Error(err.localizedMessage ?: "Download fallito")
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (updateState !is UpdateUiState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = updateState !is UpdateUiState.Downloading,
            dismissOnClickOutside = updateState !is UpdateUiState.Downloading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.dialogBackground,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Icona + Titolo + Tag versione
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Aggiornamento disponibile",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = release.tagName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accent
                            )
                            if (release.apkSizeBytes > 0) {
                                Text(
                                    text = "•   %.1f MB".format(release.apkSizeBytes / (1024f * 1024f)),
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }

                // Note di rilascio se presenti
                if (release.releaseNotes.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Novità:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.cardBackground.copy(alpha = 0.6f))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = release.releaseNotes,
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Stato Download / Pronto / Errore
                when (val state = updateState) {
                    is UpdateUiState.Idle -> {
                        Text(
                            text = "È pronta una nuova versione dell'app Closet. Vuoi scaricarla e installarla ora?",
                            fontSize = 13.5.sp,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                    is UpdateUiState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Download in corso...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${(state.progress * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = colors.accent,
                                trackColor = colors.accent.copy(alpha = 0.2f)
                            )
                            if (state.totalMb > 0) {
                                Text(
                                    text = "%.1f MB / %.1f MB".format(state.downloadedMb, state.totalMb),
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Download completato! Se l'installazione non si apre automaticamente, tocca Installa.",
                                    fontSize = 12.sp,
                                    color = colors.textPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    is UpdateUiState.Error -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE55353).copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFE55353),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        fontSize = 12.sp,
                                        color = colors.textPrimary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(release.apkDownloadUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.accent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scarica dal browser",
                                    fontSize = 12.sp,
                                    color = colors.accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    else -> {}
                }

                // Pulsanti Azione in fondo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (updateState !is UpdateUiState.Downloading) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = if (updateState is UpdateUiState.ReadyToInstall) "Chiudi" else "Più tardi",
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    when (val state = updateState) {
                        is UpdateUiState.Idle -> {
                            Button(
                                onClick = { startDownload() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Scarica ora", fontWeight = FontWeight.Bold)
                            }
                        }
                        is UpdateUiState.ReadyToInstall -> {
                            Button(
                                onClick = { AppUpdateManager.startInstallApk(context, state.apkFile) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Installa", fontWeight = FontWeight.Bold)
                            }
                        }
                        is UpdateUiState.Error -> {
                            Button(
                                onClick = { startDownload() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(text = "Riprova", fontWeight = FontWeight.Bold)
                            }
                        }
                        is UpdateUiState.Downloading -> {
                            // Nessun bottone confermativo durante il download
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
