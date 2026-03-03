package com.getmusic.hifiti.ui.detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getmusic.hifiti.MusicDownloader
import com.getmusic.hifiti.data.HiFiTiApi
import com.getmusic.hifiti.data.SongDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val songDetail: SongDetail? = null,
    val error: String? = null,
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false,
    val downloadCompleted: Boolean = false,
    val downloadedUri: Uri? = null,
    val downloadedPath: String? = null,
    val downloadError: String? = null
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val api = HiFiTiApi()
    private val downloader = MusicDownloader(application)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadDetail(threadId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)

            try {
                val detail = api.getDetail(threadId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    songDetail = detail
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun download() {
        val detail = _uiState.value.songDetail ?: return
        val audioUrl = detail.realAudioUrl ?: detail.audioUrl
        if (audioUrl.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                downloadError = null
            )

            val result = downloader.download(
                audioUrl = audioUrl,
                artist = detail.artist,
                songName = detail.songName
            ) { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }

            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadCompleted = true,
                    downloadedUri = result.uri,
                    downloadedPath = result.filePath
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadError = result.error
                )
            }
        }
    }

    fun openInMusicApp() {
        val state = _uiState.value
        try {
            when {
                state.downloadedUri != null -> downloader.openInMusicApp(state.downloadedUri)
                state.downloadedPath != null -> downloader.openInMusicApp(state.downloadedPath)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                downloadError = "无法打开音乐播放器: ${e.message}"
            )
        }
    }
}
