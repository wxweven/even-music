package com.getmusic.hifiti.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.getmusic.hifiti.player.PlayerState
import com.getmusic.hifiti.ui.player.PlayerControls

@Composable
fun DetailScreen(
    threadId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    LaunchedEffect(threadId) {
        viewModel.loadDetail(threadId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("正在解析歌曲信息...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.loadDetail(threadId) }) {
                            Text("重试")
                        }
                    }
                }
            }

            uiState.songDetail != null -> {
                val detail = uiState.songDetail!!
                val currentSong = playerState.currentSong
                val isCurrentSong = currentSong != null &&
                    currentSong.title == detail.songName &&
                    currentSong.artist == detail.artist

                SongDetailContent(
                    uiState = uiState,
                    playerState = playerState,
                    isCurrentSong = isCurrentSong,
                    onPlay = viewModel::play,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onSkipNext = viewModel::skipToNext,
                    onSkipPrevious = viewModel::skipToPrevious,
                    onCyclePlayMode = viewModel::cyclePlayMode,
                    onDownload = viewModel::download,
                    onOpenPlayer = viewModel::openInMusicApp,
                    onToggleFavorite = viewModel::toggleFavorite
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(4.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "返回",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongDetailContent(
    uiState: DetailUiState,
    playerState: PlayerState,
    isCurrentSong: Boolean,
    onPlay: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onCyclePlayMode: () -> Unit,
    onDownload: () -> Unit,
    onOpenPlayer: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val detail = uiState.songDetail ?: return
    val hasLyrics = !detail.lyrics.isNullOrEmpty()
    val pageCount = if (hasLyrics) 2 else 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 24.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> CoverPage(
                    uiState = uiState,
                    playerState = playerState,
                    isCurrentSong = isCurrentSong,
                    currentPage = pagerState.currentPage,
                    pageCount = pageCount,
                    onPlay = onPlay,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onCyclePlayMode = onCyclePlayMode,
                    onDownload = onDownload,
                    onOpenPlayer = onOpenPlayer,
                    onToggleFavorite = onToggleFavorite
                )
                1 -> LyricsPage(lyrics = detail.lyrics ?: "")
            }
        }
    }
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
private fun CoverPage(
    uiState: DetailUiState,
    playerState: PlayerState,
    isCurrentSong: Boolean,
    currentPage: Int,
    pageCount: Int,
    onPlay: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onCyclePlayMode: () -> Unit,
    onDownload: () -> Unit,
    onOpenPlayer: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val detail = uiState.songDetail ?: return
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.downloadProgress,
        label = "download_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (detail.coverUrl.isNotEmpty()) {
            AsyncImage(
                model = detail.coverUrl,
                contentDescription = "专辑封面",
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (pageCount > 1) {
            PagerIndicator(
                pageCount = pageCount,
                currentPage = currentPage
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = detail.songName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                text = " - ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = detail.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActionIconRow(
            uiState = uiState,
            animatedProgress = animatedProgress,
            onToggleFavorite = onToggleFavorite,
            onDownload = onDownload,
            onOpenPlayer = onOpenPlayer
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlayerControls(
            playerState = if (isCurrentSong) playerState else PlayerState(),
            onPlayPause = { if (isCurrentSong) onTogglePlayPause() else onPlay() },
            onSeek = onSeek,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onCyclePlayMode = onCyclePlayMode
        )

        if (uiState.downloadError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.downloadError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ActionIconRow(
    uiState: DetailUiState,
    animatedProgress: Float,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionIconItem(
            icon = {
                Icon(
                    imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (uiState.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            },
            label = if (uiState.isFavorite) "已收藏" else "收藏",
            onClick = onToggleFavorite
        )

        Spacer(modifier = Modifier.width(32.dp))

        when {
            uiState.isDownloading -> {
                ActionIconItem(
                    icon = {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    label = "${(animatedProgress * 100).toInt()}%",
                    onClick = {}
                )
            }
            uiState.downloadCompleted -> {
                ActionIconItem(
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "已下载",
                    onClick = {}
                )
            }
            else -> {
                ActionIconItem(
                    icon = {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "下载",
                    onClick = onDownload
                )
            }
        }

        if (uiState.downloadCompleted) {
            Spacer(modifier = Modifier.width(32.dp))

            ActionIconItem(
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = "打开",
                onClick = onOpenPlayer
            )
        }
    }
}

@Composable
private fun ActionIconItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LyricsPage(lyrics: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "— 歌词 —",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = lyrics,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
