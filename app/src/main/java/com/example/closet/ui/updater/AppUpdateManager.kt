package com.example.closet.ui.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long
)

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    object UpToDate : UpdateUiState()
    data class UpdateAvailable(val release: GitHubReleaseInfo) : UpdateUiState()
    data class Downloading(val progress: Float, val downloadedMb: Float, val totalMb: Float) : UpdateUiState()
    data class ReadyToInstall(val apkFile: File) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

object AppUpdateManager {
    private const val GITHUB_OWNER = "riccardo-bettini"
    private const val GITHUB_REPO = "Closet"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * Controlla l'ultima release su GitHub.
     * Restituisce ReleaseInfo se disponibile, oppure null se la versione è già la più recente.
     */
    suspend fun checkForUpdate(currentVersionName: String): Result<GitHubReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Closet-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                // Nessuna release ancora pubblicata su GitHub
                return@withContext Result.success(null)
            }
            if (responseCode != 200) {
                return@withContext Result.failure(Exception("GitHub API error: HTTP $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)
            val rawTagName = json.optString("tag_name", "").trim()
            val cleanRemoteVersion = rawTagName.removePrefix("v").removePrefix("V").trim()
            val cleanCurrentVersion = currentVersionName.removePrefix("v").removePrefix("V").trim()

            val releaseNotes = json.optString("body", "Nessuna nota di rilascio fornita.")
            val assets = json.optJSONArray("assets")

            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isNullOrEmpty() || apkName.isNullOrEmpty()) {
                // Trovata release ma senza file APK allegato
                return@withContext Result.failure(Exception("Nessun file APK allegato all'ultima release ($rawTagName)."))
            }

            val isNewer = isVersionNewer(remoteVersion = cleanRemoteVersion, currentVersion = cleanCurrentVersion)

            if (isNewer) {
                Result.success(
                    GitHubReleaseInfo(
                        tagName = rawTagName,
                        releaseNotes = releaseNotes,
                        apkDownloadUrl = apkUrl,
                        apkFileName = apkName,
                        apkSizeBytes = apkSize
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scarica l'APK nella cartella cache sicura dell'app con segnalazione del progresso.
     */
    suspend fun downloadApk(
        context: Context,
        release: GitHubReleaseInfo,
        onProgress: (progress: Float, downloadedMb: Float, totalMb: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }
            // Puliamo eventuali download precedenti per non sprecare spazio
            updatesDir.listFiles()?.forEach { it.delete() }

            val destinationFile = File(updatesDir, release.apkFileName)

            val url = URL(release.apkDownloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Closet-App")
            }

            // Gestione redirect (GitHub Releases rimandano a AWS S3)
            var currentConnection = connection
            var status = currentConnection.responseCode
            var redirects = 0
            while ((status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) && redirects < 5) {
                val newUrl = currentConnection.getHeaderField("Location")
                currentConnection.disconnect()
                currentConnection = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Closet-App")
                }
                status = currentConnection.responseCode
                redirects++
            }

            if (status != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("Download fallito: HTTP $status"))
            }

            val totalBytes = currentConnection.contentLengthLong.takeIf { it > 0 } ?: release.apkSizeBytes
            val inputStream = currentConnection.inputStream
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = (totalBytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            val downloadedMb = totalBytesRead / (1024f * 1024f)
                            val totalMb = totalBytes / (1024f * 1024f)
                            onProgress(progress, downloadedMb, totalMb)
                        }
                    }
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Avvia l'installer di sistema Android passando l'APK tramite FileProvider.
     */
    fun startInstallApk(context: Context, apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Confronto semantico per versioni numeriche tipo "1.3.01" vs "1.3.02" o "1.4.0".
     */
    private fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.split(".").mapNotNull { it.trim().toIntOrNull() }
            val currentParts = currentVersion.split(".").mapNotNull { it.trim().toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            // Se il formato non è puramente numerico, controlla string difference
            return remoteVersion != currentVersion
        }
    }
}
