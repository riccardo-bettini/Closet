package com.example.closet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closet.ui.updater.AppUpdateManager
import com.example.closet.ui.updater.GitHubReleaseInfo
import com.example.closet.ui.updater.UpdateUiState
import kotlinx.coroutines.launch

@Composable
fun AppUpdateSection(
    currentVersionName: String,
    autoCheckUpdatesOnStartup: Boolean = true,
    onToggleAutoCheckUpdates: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalOneUIColors.current

    var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }

    fun checkUpdates() {
        updateState = UpdateUiState.Checking
        coroutineScope.launch {
            val result = AppUpdateManager.checkForUpdate(currentVersionName)
            result.onSuccess { release ->
                updateState = if (release != null) {
                    UpdateUiState.UpdateAvailable(release)
                } else {
                    UpdateUiState.UpToDate
                }
            }.onFailure { err ->
                updateState = UpdateUiState.Error(err.localizedMessage ?: "Errore durante la verifica")
            }
        }
    }

    fun startDownload(release: GitHubReleaseInfo) {
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
                // Prova ad avviare automaticamente l'installer
                AppUpdateManager.startInstallApk(context, apkFile)
            }.onFailure { err ->
                updateState = UpdateUiState.Error(err.localizedMessage ?: "Download fallito")
            }
        }
    }

    // Header sezione
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.SystemUpdate,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Aggiornamento Software",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary
        )
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground,
        border = BorderStroke(0.5.dp, colors.cardBorder),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Riga superiore: Versione installata + Pulsante "Verifica"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Versione corrente",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = "v$currentVersionName",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                FilledTonalButton(
                    onClick = { checkUpdates() },
                    enabled = updateState !is UpdateUiState.Checking && updateState !is UpdateUiState.Downloading,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.accent.copy(alpha = 0.15f),
                        contentColor = colors.accent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (updateState is UpdateUiState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.accent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifica...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cerca aggiornamenti", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            HorizontalDivider(
                color = colors.cardBorder.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // Opzione: Cerca aggiornamenti all'avvio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleAutoCheckUpdates(!autoCheckUpdatesOnStartup) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Cerca aggiornamenti all'avvio",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Controlla automaticamente la presenza di nuove versioni all'apertura dell'app",
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        lineHeight = 16.sp
                    )
                }

                Switch(
                    checked = autoCheckUpdatesOnStartup,
                    onCheckedChange = { onToggleAutoCheckUpdates(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceSecondary
                    ),
                    modifier = Modifier.scale(0.88f)
                )
            }

            // STATO: App già aggiornata
            AnimatedVisibility(visible = updateState is UpdateUiState.UpToDate) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, Color(0xFF2E7D32).copy(alpha = 0.25f)),
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
                            text = "L'app è aggiornata all'ultima versione disponibile.",
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // STATO: Errore
            if (updateState is UpdateUiState.Error) {
                val errorMsg = (updateState as UpdateUiState.Error).message
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFE55353).copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, Color(0xFFE55353).copy(alpha = 0.25f)),
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
                            text = errorMsg,
                            fontSize = 12.sp,
                            color = colors.textPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // STATO: Nuova versione trovata
            if (updateState is UpdateUiState.UpdateAvailable) {
                val release = (updateState as UpdateUiState.UpdateAvailable).release
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accent.copy(alpha = 0.08f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nuova versione: ${release.tagName}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        if (release.apkSizeBytes > 0) {
                            Text(
                                text = "%.1f MB".format(release.apkSizeBytes / (1024f * 1024f)),
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    if (release.releaseNotes.isNotBlank()) {
                        Text(
                            text = release.releaseNotes,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 4
                        )
                    }

                    Button(
                        onClick = { startDownload(release) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scarica e Aggiorna",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // STATO: Download in corso
            if (updateState is UpdateUiState.Downloading) {
                val dState = updateState as UpdateUiState.Downloading
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accent.copy(alpha = 0.08f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Download dell'aggiornamento...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${(dState.progress * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }

                    LinearProgressIndicator(
                        progress = { dState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colors.accent,
                        trackColor = colors.accent.copy(alpha = 0.2f)
                    )

                    if (dState.totalMb > 0) {
                        Text(
                            text = "%.1f MB / %.1f MB".format(dState.downloadedMb, dState.totalMb),
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // STATO: Pronto per l'installazione
            if (updateState is UpdateUiState.ReadyToInstall) {
                val apkFile = (updateState as UpdateUiState.ReadyToInstall).apkFile
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Download completato con successo!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Se la schermata di installazione non si è aperta da sola, tocca il pulsante qui sotto.",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )

                    Button(
                        onClick = { AppUpdateManager.startInstallApk(context, apkFile) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Installa Aggiornamento",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
