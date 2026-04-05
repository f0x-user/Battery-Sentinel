package com.flamefox.batterysentinel.presentation.update

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

enum class UpdateStatus { CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, DOWNLOADING, DOWNLOAD_COMPLETE, ERROR }

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.CHECKING,
    val currentVersion: String = "",
    val latestVersion: String = "",
    val downloadProgress: Int = 0,
    val errorMessage: String = ""
) {
    val isVisible: Boolean get() = status == UpdateStatus.UPDATE_AVAILABLE
            || status == UpdateStatus.DOWNLOADING
            || status == UpdateStatus.DOWNLOAD_COMPLETE
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var apkFile: File? = null
    private var pendingDownloadUrl: String = ""

    init {
        val ctx = getApplication<Application>()
        val currentVersion = ctx.packageManager
            .getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0L))
            .versionName ?: "0"
        _uiState.update { it.copy(currentVersion = currentVersion) }
        checkForUpdate(currentVersion)
    }

    private fun checkForUpdate(currentVersion: String) {
        viewModelScope.launch {
            val result = fetchLatestRelease()
            if (result == null) {
                _uiState.update { it.copy(status = UpdateStatus.UP_TO_DATE) }
                return@launch
            }
            val (latestVersion, downloadUrl) = result
            pendingDownloadUrl = downloadUrl
            if (isNewer(latestVersion, currentVersion)) {
                _uiState.update { it.copy(
                    status = UpdateStatus.UPDATE_AVAILABLE,
                    latestVersion = latestVersion
                )}
            } else {
                _uiState.update { it.copy(status = UpdateStatus.UP_TO_DATE) }
            }
        }
    }

    fun startDownload() {
        val url = pendingDownloadUrl.ifEmpty { return }
        viewModelScope.launch {
            _uiState.update { it.copy(status = UpdateStatus.DOWNLOADING, downloadProgress = 0) }
            downloadApk(url)
        }
    }

    fun installApk(context: Context) {
        val file = apkFile ?: return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun dismiss() {
        _uiState.update { it.copy(status = UpdateStatus.UP_TO_DATE) }
    }

    private suspend fun fetchLatestRelease(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/f0x-user/Battery-Sentinel/releases/latest")
                .openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "BatterySentinel-Android")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.connect()

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            val tagName = json.getString("tag_name").removePrefix("v")
            val assets = json.getJSONArray("assets")
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (downloadUrl.isEmpty()) null else Pair(tagName, downloadUrl)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun downloadApk(url: String) = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "BatterySentinel-Android")
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.connect()

            val total = conn.contentLength.toLong()
            val outputFile = File(getApplication<Application>().cacheDir, "update.apk")
            val output = FileOutputStream(outputFile)
            val input = conn.inputStream
            val buffer = ByteArray(8192)
            var downloaded = 0L

            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                if (total > 0) {
                    val progress = (downloaded * 100 / total).toInt()
                    _uiState.update { it.copy(downloadProgress = progress) }
                }
            }

            output.flush()
            output.close()
            input.close()
            conn.disconnect()

            apkFile = outputFile
            _uiState.update { it.copy(status = UpdateStatus.DOWNLOAD_COMPLETE, downloadProgress = 100) }
        } catch (e: Exception) {
            _uiState.update { it.copy(
                status = UpdateStatus.ERROR,
                errorMessage = e.message ?: "Download failed"
            )}
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(l.size, c.size)
        for (i in 0 until len) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
