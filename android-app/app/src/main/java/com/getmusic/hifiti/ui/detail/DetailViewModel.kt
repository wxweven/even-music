package com.getmusic.hifiti.ui.detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getmusic.hifiti.MusicDownloader
import com.getmusic.hifiti.data.FavoriteSong
import com.getmusic.hifiti.data.FavoritesManager
import com.getmusic.hifiti.data.HiFiTiApi
import com.getmusic.hifiti.data.SongDetail
import com.getmusic.hifiti.data.SongDetailCache
import com.getmusic.hifiti.player.MusicPlayerManager
import com.getmusic.hifiti.player.PlayerState
import com.getmusic.hifiti.player.SongInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val downloadError: String? = null,
    val isFavorite: Boolean = false
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val api = HiFiTiApi()
    private val downloader = MusicDownloader(application)
    private val favoritesManager = FavoritesManager(application)
    private val detailCache = SongDetailCache(application)
    val playerManager = MusicPlayerManager.getInstance(application)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = playerManager.playerState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerState())

    private var currentThreadId: String = ""

    init {
        viewModelScope.launch {
            playerManager.playerState
                .map { it.currentSong }
                .filterNotNull()
                .distinctUntilChangedBy { it.threadId }
                .collect { song ->
                    if (song.threadId.isNotEmpty() && song.threadId != currentThreadId) {
                        loadDetailForSongTransition(song)
                    }
                }
        }
    }

    fun loadDetail(threadId: String) {
        currentThreadId = threadId

        val cached = detailCache.get(threadId)
        if (cached != null) {
            val audioUrl = cached.audioUrl
            val alreadyDownloaded = audioUrl.isNotEmpty() && downloader.isDownloaded(audioUrl)
            _uiState.value = DetailUiState(
                isLoading = false,
                songDetail = cached,
                downloadCompleted = alreadyDownloaded,
                downloadedUri = if (alreadyDownloaded) downloader.getDownloadedUri(audioUrl) else null,
                isFavorite = favoritesManager.isFavorite(threadId)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            fetchDetail(threadId)
        }
    }

    private fun loadDetailForSongTransition(song: SongInfo) {
        currentThreadId = song.threadId

        val cached = detailCache.get(song.threadId)
        if (cached != null) {
            val audioUrl = cached.audioUrl
            val alreadyDownloaded = audioUrl.isNotEmpty() && downloader.isDownloaded(audioUrl)
            _uiState.value = DetailUiState(
                isLoading = false,
                songDetail = cached,
                downloadCompleted = alreadyDownloaded,
                downloadedUri = if (alreadyDownloaded) downloader.getDownloadedUri(audioUrl) else null,
                isFavorite = favoritesManager.isFavorite(song.threadId)
            )
            return
        }

        val placeholder = SongDetail(
            songName = song.title,
            artist = song.artist,
            audioUrl = song.audioUrl,
            coverUrl = song.coverUrl,
            lyrics = null,
            realAudioUrl = null
        )
        _uiState.value = DetailUiState(
            isLoading = false,
            songDetail = placeholder,
            isFavorite = favoritesManager.isFavorite(song.threadId)
        )
        viewModelScope.launch {
            fetchDetail(song.threadId)
        }
    }

    private suspend fun fetchDetail(threadId: String) {
        try {
            val detail = api.getDetail(threadId)
            detailCache.put(threadId, detail)

            val audioUrl = detail.audioUrl
            val alreadyDownloaded = audioUrl.isNotEmpty() && downloader.isDownloaded(audioUrl)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                songDetail = detail,
                error = null,
                downloadCompleted = alreadyDownloaded,
                downloadedUri = if (alreadyDownloaded) downloader.getDownloadedUri(audioUrl) else null,
                isFavorite = favoritesManager.isFavorite(threadId)
            )
        } catch (e: Exception) {
            if (_uiState.value.songDetail == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun play() {
        val detail = _uiState.value.songDetail ?: return
        val audioUrl = detail.audioUrl
        if (audioUrl.isEmpty()) return

        val localUri = downloader.getDownloadedUri(audioUrl)
        val playUrl = localUri?.toString() ?: audioUrl

        val songInfo = SongInfo(
            title = detail.songName,
            artist = detail.artist,
            coverUrl = detail.coverUrl,
            audioUrl = playUrl,
            threadId = currentThreadId
        )

        playerManager.play(songInfo)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun skipToNext() {
        playerManager.skipToNext()
    }

    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    fun cyclePlayMode() {
        playerManager.cyclePlayMode()
    }

    fun download() {
        val detail = _uiState.value.songDetail ?: return
        val audioUrl = detail.audioUrl
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

    fun toggleFavorite() {
        val detail = _uiState.value.songDetail ?: return
        val audioUrl = detail.audioUrl

        val song = FavoriteSong(
            threadId = currentThreadId,
            title = detail.songName,
            artist = detail.artist,
            coverUrl = detail.coverUrl,
            audioUrl = audioUrl
        )
        val nowFavorite = favoritesManager.toggle(song)
        _uiState.value = _uiState.value.copy(isFavorite = nowFavorite)
    }
}
