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

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentSong: SongInfo? = null,
    val playlistSize: Int = 0,
    val currentIndex: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isBuffering: Boolean = false
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
                        startPositionUpdates()

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

    fun play(songInfo: SongInfo) {
        val ctrl = controller
        if (ctrl == null) {
            pendingSong = songInfo
            connect()
            return
        }

        val key = songKey(songInfo)
        val existingIndex = findSongIndex(key)
        if (existingIndex >= 0) {
            songMap[existingIndex] = songInfo
            ctrl.replaceMediaItem(existingIndex, buildMediaItem(songInfo))
            if (ctrl.currentMediaItemIndex != existingIndex) {
                ctrl.seekTo(existingIndex, 0)
            }
            ctrl.play()
            return
        }

        val mediaItem = buildMediaItem(songInfo)
        val insertIndex = ctrl.mediaItemCount
        songMap[insertIndex] = songInfo
        ctrl.addMediaItem(mediaItem)
        ctrl.seekTo(insertIndex, 0)
        ctrl.play()
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
        if (ctrl.hasNextMediaItem()) ctrl.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        val ctrl = controller ?: return
        if (ctrl.hasPreviousMediaItem()) ctrl.seekToPreviousMediaItem()
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        songMap.clear()
        _playerState.value = PlayerState()
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

        return MediaItem.Builder()
            .setMediaId(songKey(songInfo))
            .setUri(songInfo.audioUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
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

        _playerState.value = PlayerState(
            isPlaying = ctrl.isPlaying,
            currentPosition = ctrl.currentPosition.coerceAtLeast(0),
            duration = ctrl.duration.coerceAtLeast(0),
            currentSong = currentSong,
            playlistSize = ctrl.mediaItemCount,
            currentIndex = currentIndex,
            hasNext = ctrl.hasNextMediaItem(),
            hasPrevious = ctrl.hasPreviousMediaItem(),
            isBuffering = ctrl.playbackState == Player.STATE_BUFFERING
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
