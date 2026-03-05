package com.getmusic.hifiti.ui.my

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.getmusic.hifiti.data.FavoriteSong
import com.getmusic.hifiti.data.FavoritesManager
import com.getmusic.hifiti.data.PlayHistoryItem
import com.getmusic.hifiti.data.PlayHistoryManager
import com.getmusic.hifiti.player.MusicPlayerManager
import com.getmusic.hifiti.player.PlaylistSource
import com.getmusic.hifiti.player.SongInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
