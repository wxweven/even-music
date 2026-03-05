package com.getmusic.hifiti.ui.my

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getmusic.hifiti.data.AudioUrlCache
import com.getmusic.hifiti.data.FavoriteSong
import com.getmusic.hifiti.data.FavoritesManager
import com.getmusic.hifiti.data.HiFiTiApi
import com.getmusic.hifiti.data.PlayHistoryItem
import com.getmusic.hifiti.data.PlayHistoryManager
import com.getmusic.hifiti.data.SongDetailCache
import com.getmusic.hifiti.player.MusicPlayerManager
import com.getmusic.hifiti.player.PlaylistSource
import com.getmusic.hifiti.player.SongInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesManager = FavoritesManager(application)
    private val playHistoryManager = PlayHistoryManager(application)
    private val playerManager = MusicPlayerManager.getInstance(application)
    private val api = HiFiTiApi()
    private val detailCache = SongDetailCache(application)

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
        if (songs.isEmpty()) return
        viewModelScope.launch {
            val refreshed = songs.mapIndexed { i, song ->
                if (i == 0) song.copy(audioUrl = fetchFreshUrl(song.threadId, song.audioUrl))
                else song
            }
            playerManager.setPlaylist(refreshed, 0, PlaylistSource.FAVORITES)
        }
    }

    fun playFavoriteAt(index: Int) {
        val songs = _favorites.value.map { it.toSongInfo() }
        if (songs.isEmpty() || index !in songs.indices) return
        viewModelScope.launch {
            val refreshed = songs.toMutableList()
            refreshed[index] = refreshed[index].copy(
                audioUrl = fetchFreshUrl(refreshed[index].threadId, refreshed[index].audioUrl)
            )
            playerManager.setPlaylist(refreshed, index, PlaylistSource.FAVORITES)
        }
    }

    fun playAllHistory() {
        val songs = _playHistory.value.map { it.toSongInfo() }
        if (songs.isEmpty()) return
        viewModelScope.launch {
            val refreshed = songs.mapIndexed { i, song ->
                if (i == 0) song.copy(audioUrl = fetchFreshUrl(song.threadId, song.audioUrl))
                else song
            }
            playerManager.setPlaylist(refreshed, 0, PlaylistSource.HISTORY)
        }
    }

    fun playHistoryAt(index: Int) {
        val songs = _playHistory.value.map { it.toSongInfo() }
        if (songs.isEmpty() || index !in songs.indices) return
        viewModelScope.launch {
            val refreshed = songs.toMutableList()
            refreshed[index] = refreshed[index].copy(
                audioUrl = fetchFreshUrl(refreshed[index].threadId, refreshed[index].audioUrl)
            )
            playerManager.setPlaylist(refreshed, index, PlaylistSource.HISTORY)
        }
    }

    /**
     * Get a fresh audio URL for the given threadId.
     * Returns from 10-minute TTL cache if available, otherwise fetches from API.
     */
    private suspend fun fetchFreshUrl(threadId: String, fallbackUrl: String): String {
        if (threadId.isEmpty()) return fallbackUrl

        AudioUrlCache.get(threadId)?.let { return it }

        return try {
            val detail = api.getDetail(threadId)
            detailCache.put(threadId, detail)
            val url = detail.audioUrl.ifEmpty { fallbackUrl }
            AudioUrlCache.put(threadId, url)
            url
        } catch (_: Exception) {
            fallbackUrl
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
