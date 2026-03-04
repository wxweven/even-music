package com.getmusic.hifiti

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class MusicDownloader(private val context: Context) {

    private val prefs = context.getSharedPreferences("download_cache", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    data class DownloadResult(
        val success: Boolean,
        val filePath: String? = null,
        val uri: Uri? = null,
        val error: String? = null
    )

    /**
     * Download audio file to public Music/HiFiTi/ directory.
     * On Android 10+ uses MediaStore; on older versions uses direct file write.
     */
    suspend fun download(
        audioUrl: String,
        artist: String,
        songName: String,
        onProgress: (Float) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            val ext = detectExtension(audioUrl)
            val filename = sanitizeFilename("$artist - $songName$ext")

            val request = Request.Builder()
                .url(audioUrl)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult(
                        success = false,
                        error = "HTTP ${response.code}"
                    )
                }

                val body = response.body
                    ?: return@withContext DownloadResult(success = false, error = "空响应")

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                val (outputStream, resultPath, resultUri) = openOutputStream(filename)

                outputStream.use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                onProgress(downloadedBytes.toFloat() / totalBytes)
                            }
                        }
                    }
                }

                onProgress(1f)

                val resultUriString = resultUri?.toString()
                    ?: resultPath?.let { Uri.fromFile(File(it)).toString() }
                if (resultUriString != null) {
                    prefs.edit().putString(audioUrl, resultUriString).apply()
                }

                DownloadResult(
                    success = true,
                    filePath = resultPath,
                    uri = resultUri
                )
            }
        } catch (e: Exception) {
            DownloadResult(success = false, error = e.message ?: "下载失败")
        }
    }

    private data class OutputTarget(
        val stream: OutputStream,
        val path: String?,
        val uri: Uri?
    )

    private fun openOutputStream(filename: String): OutputTarget {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.MIME_TYPE, guessMimeType(filename))
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MUSIC}/HiFiTi"
                )
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw RuntimeException("无法创建 MediaStore 条目")

            val stream = resolver.openOutputStream(uri)
                ?: throw RuntimeException("无法打开输出流")

            // Mark as not pending after write completes — caller must close stream first
            // We'll update IS_PENDING in a wrapper
            return OutputTarget(
                stream = PendingOutputStream(stream) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                },
                path = null,
                uri = uri
            )
        } else {
            val musicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "HiFiTi"
            )
            musicDir.mkdirs()
            val file = File(musicDir, filename)
            return OutputTarget(
                stream = file.outputStream(),
                path = file.absolutePath,
                uri = Uri.fromFile(file)
            )
        }
    }

    fun openInMusicApp(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "选择音乐播放器").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openInMusicApp(filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        openInMusicApp(uri)
    }

    private fun detectExtension(url: String): String {
        val lower = url.lowercase()
        for (ext in listOf(".flac", ".m4a", ".wav", ".aac", ".ogg", ".mp3")) {
            if (ext in lower) return ext
        }
        return ".mp3"
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("""[/\\:*?"<>|]"""), "-").trim()
    }

    private fun guessMimeType(filename: String): String {
        return when {
            filename.endsWith(".flac") -> "audio/flac"
            filename.endsWith(".m4a") -> "audio/mp4"
            filename.endsWith(".wav") -> "audio/wav"
            filename.endsWith(".ogg") -> "audio/ogg"
            else -> "audio/mpeg"
        }
    }

    fun getDownloadedUri(audioUrl: String): Uri? {
        val uriString = prefs.getString(audioUrl, null) ?: return null
        return Uri.parse(uriString)
    }

    fun isDownloaded(audioUrl: String): Boolean {
        return prefs.contains(audioUrl)
    }

    /**
     * OutputStream wrapper that runs a callback when closed,
     * used to mark MediaStore entry as not pending.
     */
    private class PendingOutputStream(
        private val delegate: OutputStream,
        private val onClose: () -> Unit
    ) : OutputStream() {
        override fun write(b: Int) = delegate.write(b)
        override fun write(b: ByteArray) = delegate.write(b)
        override fun write(b: ByteArray, off: Int, len: Int) = delegate.write(b, off, len)
        override fun flush() = delegate.flush()
        override fun close() {
            delegate.close()
            onClose()
        }
    }
}
