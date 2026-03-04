package com.getmusic.hifiti.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.getmusic.hifiti.MusicDownloader
import com.getmusic.hifiti.data.PlayHistoryItem
import com.getmusic.hifiti.data.PlayHistoryManager
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SongInfo(
    val title: String,
    val artist: String,
    val coverUrl: String,
    val audioUrl: String,
    val threadId: String = ""
)

enum class PlaylistSource { FAVORITES, HISTORY, SINGLE }

enum class PlayMode { SEQUENTIAL, SHUFFLE, REPEAT_ONE }

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentSong: SongInfo? = null,
    val playlistSize: Int = 0,
    val currentIndex: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isBuffering: Boolean = false,
    val playlistSource: PlaylistSource = PlaylistSource.SINGLE,
    val playMode: PlayMode = PlayMode.SEQUENTIAL
)

class MusicPlayerManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MusicPlayerManager"

        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionUpdateStarted = false

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val songMap = mutableMapOf<Int, SongInfo>()

    private var pendingSong: SongInfo? = null
    private var pendingPlaylist: Triple<List<SongInfo>, Int, PlaylistSource>? = null

    private val playHistoryManager = PlayHistoryManager(context)
    private val downloader = MusicDownloader(context)

    private var _playlistSource = PlaylistSource.SINGLE
    private var _playMode = loadPlayMode()

    private val playModePrefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)

    fun connect() {
        if (controller != null || controllerFuture != null) return

        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )

            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().also { future ->
                future.addListener({
                    try {
                        controller = future.get()
                        controller?.addListener(playerListener)
                        applyPlayMode(_playMode)
                        startPositionUpdates()

                        pendingPlaylist?.let { (songs, startIndex, source) ->
                            pendingPlaylist = null
                            setPlaylist(songs, startIndex, source)
                        }

                        pendingSong?.let { song ->
                            pendingSong = null
                            play(song)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to connect to PlaybackService", e)
                        controllerFuture = null
                    }
                }, mainHandler::post)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create MediaController", e)
        }
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    fun setPlaylist(songs: List<SongInfo>, startIndex: Int = 0, source: PlaylistSource = PlaylistSource.SINGLE) {
        val ctrl = controller
        if (ctrl == null) {
            pendingPlaylist = Triple(songs, startIndex, source)
            connect()
            return
        }

        _playlistSource = source
        songMap.clear()
        ctrl.clearMediaItems()

        val mediaItems = songs.mapIndexed { index, song ->
            songMap[index] = song
            buildMediaItem(song)
        }
        ctrl.setMediaItems(mediaItems, startIndex, 0L)
        applyPlayMode(_playMode)
        ctrl.play()

        if (songs.isNotEmpty() && startIndex in songs.indices) {
            recordHistory(songs[startIndex])
        }
    }

    fun play(songInfo: SongInfo) {
        val ctrl = controller
        if (ctrl == null) {
            pendingSong = songInfo
            connect()
            return
        }

        _playlistSource = PlaylistSource.SINGLE
        songMap.clear()
        ctrl.clearMediaItems()

        val mediaItem = buildMediaItem(songInfo)
        songMap[0] = songInfo
        ctrl.setMediaItems(listOf(mediaItem), 0, 0L)
        applyPlayMode(_playMode)
        ctrl.play()

        recordHistory(songInfo)
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun skipToNext() {
        val ctrl = controller ?: return
        if (ctrl.hasNextMediaItem()) {
            ctrl.seekToNextMediaItem()
        } else if (ctrl.mediaItemCount > 1) {
            ctrl.seekTo(0, 0)
        }
    }

    fun skipToPrevious() {
        val ctrl = controller ?: return
        if (ctrl.hasPreviousMediaItem()) {
            ctrl.seekToPreviousMediaItem()
        } else if (ctrl.mediaItemCount > 1) {
            ctrl.seekTo(ctrl.mediaItemCount - 1, 0)
        }
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        songMap.clear()
        _playlistSource = PlaylistSource.SINGLE
        _playerState.value = PlayerState(playMode = _playMode)
    }

    fun cyclePlayMode(): PlayMode {
        val next = when (_playMode) {
            PlayMode.SEQUENTIAL -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SEQUENTIAL
        }
        setPlayMode(next)
        return next
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode = mode
        applyPlayMode(mode)
        savePlayMode(mode)
        _playerState.value = _playerState.value.copy(playMode = mode)
    }

    private fun applyPlayMode(mode: PlayMode) {
        val ctrl = controller ?: return
        when (mode) {
            PlayMode.SEQUENTIAL -> {
                ctrl.repeatMode = Player.REPEAT_MODE_ALL
                ctrl.shuffleModeEnabled = false
            }
            PlayMode.SHUFFLE -> {
                ctrl.repeatMode = Player.REPEAT_MODE_ALL
                ctrl.shuffleModeEnabled = true
            }
            PlayMode.REPEAT_ONE -> {
                ctrl.repeatMode = Player.REPEAT_MODE_ONE
                ctrl.shuffleModeEnabled = false
            }
        }
    }

    private fun loadPlayMode(): PlayMode {
        val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        val name = prefs.getString("play_mode", PlayMode.SEQUENTIAL.name) ?: PlayMode.SEQUENTIAL.name
        return try { PlayMode.valueOf(name) } catch (_: Exception) { PlayMode.SEQUENTIAL }
    }

    private fun savePlayMode(mode: PlayMode) {
        playModePrefs.edit().putString("play_mode", mode.name).apply()
    }

    private fun recordHistory(songInfo: SongInfo) {
        if (songInfo.threadId.isEmpty()) return
        playHistoryManager.record(
            PlayHistoryItem(
                threadId = songInfo.threadId,
                title = songInfo.title,
                artist = songInfo.artist,
                coverUrl = songInfo.coverUrl,
                audioUrl = songInfo.audioUrl
            )
        )
    }

    private fun songKey(songInfo: SongInfo): String = "${songInfo.title}|${songInfo.artist}"

    private fun findSongIndex(songKey: String): Int {
        val ctrl = controller ?: return -1
        for (i in 0 until ctrl.mediaItemCount) {
            if (ctrl.getMediaItemAt(i).mediaId == songKey) return i
        }
        return -1
    }

    private fun buildMediaItem(songInfo: SongInfo): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(songInfo.title)
            .setArtist(songInfo.artist)
            .setArtworkUri(if (songInfo.coverUrl.isNotEmpty()) Uri.parse(songInfo.coverUrl) else null)
            .build()

        val playUri = resolvePlayUri(songInfo.audioUrl)

        return MediaItem.Builder()
            .setMediaId(songKey(songInfo))
            .setUri(playUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun resolvePlayUri(audioUrl: String): Uri {
        if (audioUrl.startsWith("content://") || audioUrl.startsWith("file://")) {
            return Uri.parse(audioUrl)
        }
        val downloadedUri = downloader.getDownloadedUri(audioUrl)
        if (downloadedUri != null) {
            return downloadedUri
        }
        return Uri.parse(audioUrl)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateState()
            val ctrl = controller ?: return
            val currentIndex = ctrl.currentMediaItemIndex
            val song = songMap[currentIndex]
            if (song != null && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                recordHistory(song)
            }
        }
    }

    private fun updateState() {
        val ctrl = controller ?: return
        val currentIndex = ctrl.currentMediaItemIndex
        val currentItem = ctrl.currentMediaItem
        val currentSong = songMap[currentIndex]
            ?: currentItem?.let {
                SongInfo(
                    title = it.mediaMetadata.title?.toString() ?: "",
                    artist = it.mediaMetadata.artist?.toString() ?: "",
                    coverUrl = it.mediaMetadata.artworkUri?.toString() ?: "",
                    audioUrl = it.mediaId
                )
            }

        val hasMultiple = ctrl.mediaItemCount > 1
        _playerState.value = PlayerState(
            isPlaying = ctrl.isPlaying,
            currentPosition = ctrl.currentPosition.coerceAtLeast(0),
            duration = ctrl.duration.coerceAtLeast(0),
            currentSong = currentSong,
            playlistSize = ctrl.mediaItemCount,
            currentIndex = currentIndex,
            hasNext = hasMultiple,
            hasPrevious = hasMultiple,
            isBuffering = ctrl.playbackState == Player.STATE_BUFFERING,
            playlistSource = _playlistSource,
            playMode = _playMode
        )
    }

    private fun startPositionUpdates() {
        if (positionUpdateStarted) return
        positionUpdateStarted = true

        scope.launch {
            while (true) {
                delay(500)
                val ctrl = controller ?: continue
                if (ctrl.isPlaying || ctrl.playbackState == Player.STATE_BUFFERING) {
                    _playerState.value = _playerState.value.copy(
                        currentPosition = ctrl.currentPosition.coerceAtLeast(0),
                        duration = ctrl.duration.coerceAtLeast(0),
                        isBuffering = ctrl.playbackState == Player.STATE_BUFFERING
                    )
                }
            }
        }
    }
}
