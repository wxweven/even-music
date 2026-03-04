package com.getmusic.hifiti.ui.my

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getmusic.hifiti.data.FavoriteSong
import com.getmusic.hifiti.data.FavoritesManager
import com.getmusic.hifiti.data.PlayHistoryItem
import com.getmusic.hifiti.data.PlayHistoryManager
import com.getmusic.hifiti.player.MusicPlayerManager
import com.getmusic.hifiti.player.PlaylistSource
import com.getmusic.hifiti.player.SongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesManager = FavoritesManager(application)
    private val playHistoryManager = PlayHistoryManager(application)
    private val playerManager = MusicPlayerManager.getInstance(application)

    private val _favorites = MutableStateFlow<List<FavoriteSong>>(emptyList())
    val favorites: StateFlow<List<FavoriteSong>> = _favorites.asStateFlow()

    private val _playHistory = MutableStateFlow<List<PlayHistoryItem>>(emptyList())
    val playHistory: StateFlow<List<PlayHistoryItem>> = _playHistory.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        _favorites.value = favoritesManager.getAll()
        _playHistory.value = playHistoryManager.getAll()
    }

    fun refreshFavorites() {
        _favorites.value = favoritesManager.getAll()
    }

    fun refreshHistory() {
        _playHistory.value = playHistoryManager.getAll()
    }

    fun removeFavorite(threadId: String) {
        favoritesManager.remove(threadId)
        _favorites.value = favoritesManager.getAll()
    }

    fun clearHistory() {
        playHistoryManager.clear()
        _playHistory.value = emptyList()
    }

    fun playAllFavorites() {
        val songs = _favorites.value.map { it.toSongInfo() }
        if (songs.isNotEmpty()) {
            playerManager.setPlaylist(songs, 0, PlaylistSource.FAVORITES)
        }
    }

    fun playFavoriteAt(index: Int) {
        val songs = _favorites.value.map { it.toSongInfo() }
        if (songs.isNotEmpty() && index in songs.indices) {
            playerManager.setPlaylist(songs, index, PlaylistSource.FAVORITES)
        }
    }

    fun playAllHistory() {
        val songs = _playHistory.value.map { it.toSongInfo() }
        if (songs.isNotEmpty()) {
            playerManager.setPlaylist(songs, 0, PlaylistSource.HISTORY)
        }
    }

    fun playHistoryAt(index: Int) {
        val songs = _playHistory.value.map { it.toSongInfo() }
        if (songs.isNotEmpty() && index in songs.indices) {
            playerManager.setPlaylist(songs, index, PlaylistSource.HISTORY)
        }
    }

    private val _exportResult = MutableSharedFlow<Result<Int>>()
    val exportResult: SharedFlow<Result<Int>> = _exportResult.asSharedFlow()

    private val _importResult = MutableSharedFlow<Result<Int>>()
    val importResult: SharedFlow<Result<Int>> = _importResult.asSharedFlow()

    private val _pendingImportSongs = MutableStateFlow<List<FavoriteSong>>(emptyList())
    val pendingImportSongs: StateFlow<List<FavoriteSong>> = _pendingImportSongs.asStateFlow()

    fun exportFavorites(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = favoritesManager.exportToJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    } ?: throw Exception("无法写入文件")
                }
                _exportResult.emit(Result.success(_favorites.value.size))
            } catch (e: Exception) {
                _exportResult.emit(Result.failure(e))
            }
        }
    }

    fun readImportFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    } ?: throw Exception("无法读取文件")
                }
                val songs = favoritesManager.parseFromJson(json)
                if (songs.isEmpty()) throw Exception("文件中没有歌曲数据")
                _pendingImportSongs.value = songs
            } catch (e: Exception) {
                _importResult.emit(Result.failure(e))
            }
        }
    }

    fun confirmImport(replace: Boolean) {
        viewModelScope.launch {
            try {
                val songs = _pendingImportSongs.value
                if (songs.isEmpty()) return@launch
                val count = favoritesManager.importSongs(songs, replace)
                refreshFavorites()
                _pendingImportSongs.value = emptyList()
                _importResult.emit(Result.success(count))
            } catch (e: Exception) {
                _importResult.emit(Result.failure(e))
            }
        }
    }

    fun cancelImport() {
        _pendingImportSongs.value = emptyList()
    }
}

private fun FavoriteSong.toSongInfo() = SongInfo(
    title = title,
    artist = artist,
    coverUrl = coverUrl,
    audioUrl = audioUrl,
    threadId = threadId
)

private fun PlayHistoryItem.toSongInfo() = SongInfo(
    title = title,
    artist = artist,
    coverUrl = coverUrl,
    audioUrl = audioUrl,
    threadId = threadId
)
