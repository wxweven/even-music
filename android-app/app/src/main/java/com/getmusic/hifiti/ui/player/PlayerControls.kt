package com.getmusic.hifiti.ui.player

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.getmusic.hifiti.player.PlayMode
import com.getmusic.hifiti.player.PlayerState

@Composable
fun PlayerControls(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onCyclePlayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val currentMs = if (isSeeking) {
                (seekPosition * playerState.duration).toLong()
            } else {
                playerState.currentPosition
            }
            val progress = if (playerState.duration > 0) {
                if (isSeeking) seekPosition
                else playerState.currentPosition.toFloat() / playerState.duration
            } else 0f

            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = {
                    isSeeking = true
                    seekPosition = it
                },
                onValueChangeFinished = {
                    onSeek((seekPosition * playerState.duration).toLong())
                    isSeeking = false
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(playerState.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        onCyclePlayMode()
                        val modeName = when (playerState.playMode) {
                            PlayMode.SEQUENTIAL -> "随机播放"
                            PlayMode.SHUFFLE -> "单曲循环"
                            PlayMode.REPEAT_ONE -> "顺序播放"
                        }
                        Toast.makeText(context, modeName, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = when (playerState.playMode) {
                            PlayMode.SEQUENTIAL -> Icons.Default.Repeat
                            PlayMode.SHUFFLE -> Icons.Default.Shuffle
                            PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                        },
                        contentDescription = "播放模式",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSkipPrevious,
                    enabled = playerState.hasPrevious
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onSkipNext,
                    enabled = playerState.hasNext
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:%02d".format(seconds)
}
