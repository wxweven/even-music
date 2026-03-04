package com.getmusic.hifiti.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.getmusic.hifiti.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
    val forceUpdate: Boolean = false
)

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data object Downloaded : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class AppUpdater(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            BuildConfig.VERSION_CODE
        }
    }

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(BuildConfig.UPDATE_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val json = JSONObject(response.body?.string() ?: return@withContext null)
                val remoteVersionCode = json.getInt("versionCode")
                val currentVersionCode = getCurrentVersionCode()

                if (remoteVersionCode > currentVersionCode) {
                    UpdateInfo(
                        versionCode = remoteVersionCode,
                        versionName = json.getString("versionName"),
                        apkUrl = json.getString("apkUrl"),
                        changelog = json.optString("changelog", ""),
                        forceUpdate = json.optBoolean("forceUpdate", false)
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadApk(updateInfo: UpdateInfo): File? = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Downloading(0)

            val apkDir = File(context.cacheDir, "apk")
            if (apkDir.exists()) {
                apkDir.listFiles()?.forEach { it.delete() }
            } else {
                apkDir.mkdirs()
            }

            val apkFile = File(apkDir, "hifiti-${updateInfo.versionName}.apk")

            val request = Request.Builder()
                .url(updateInfo.apkUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _downloadState.value = DownloadState.Error("下载失败: HTTP ${response.code}")
                    return@withContext null
                }

                val body = response.body ?: run {
                    _downloadState.value = DownloadState.Error("下载失败: 空响应")
                    return@withContext null
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                apkFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = (downloadedBytes * 100 / totalBytes).toInt()
                                _downloadState.value = DownloadState.Downloading(progress)
                            }
                        }
                    }
                }

                _downloadState.value = DownloadState.Downloaded
                apkFile
            }
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error("下载失败: ${e.message}")
            null
        }
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }
}
