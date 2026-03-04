package com.getmusic.hifiti.ui.my

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.getmusic.hifiti.data.FavoriteSong
import com.getmusic.hifiti.data.PlayHistoryItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyScreen(
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: MyViewModel = viewModel()
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsState()
    val playHistory by viewModel.playHistory.collectAsState()
    val pendingImportSongs by viewModel.pendingImportSongs.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportFavorites(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.readImportFile(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    LaunchedEffect(Unit) {
        viewModel.exportResult.collect { result ->
            result.onSuccess { count ->
                Toast.makeText(context, "已导出 $count 首歌曲", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.importResult.collect { result ->
            result.onSuccess { count ->
                Toast.makeText(context, "成功导入 $count 首新歌曲", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空播放历史") },
            text = { Text("确定要清空所有播放历史吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (pendingImportSongs.isNotEmpty()) {
        ImportStrategyDialog(
            songCount = pendingImportSongs.size,
            onMerge = { viewModel.confirmImport(replace = false) },
            onReplace = { viewModel.confirmImport(replace = true) },
            onDismiss = { viewModel.cancelImport() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    if (pagerState.currentPage == 0) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("导出收藏") },
                                    leadingIcon = {
                                        Icon(Icons.Default.FileUpload, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        val timestamp = SimpleDateFormat(
                                            "yyyyMMdd_HHmmss", Locale.getDefault()
                                        ).format(Date())
                                        exportLauncher.launch("favorites_$timestamp.json")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("导入收藏") },
                                    leadingIcon = {
                                        Icon(Icons.Default.FileDownload, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        importLauncher.launch(arrayOf("application/json", "*/*"))
                                    }
                                )
                            }
                        }
                    }
                    if (pagerState.currentPage == 1 && playHistory.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "清空播放历史"
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("收藏歌曲") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("播放历史") }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> FavoritesTab(
                        favorites = favorites,
                        onPlayAll = {
                            viewModel.playAllFavorites()
                            favorites.firstOrNull()?.let { onNavigateToDetail(it.threadId) }
                        },
                        onPlayAt = { index ->
                            viewModel.playFavoriteAt(index)
                            favorites.getOrNull(index)?.let { onNavigateToDetail(it.threadId) }
                        },
                        onRemove = viewModel::removeFavorite
                    )
                    1 -> HistoryTab(
                        history = playHistory,
                        onPlayAll = {
                            viewModel.playAllHistory()
                            playHistory.firstOrNull()?.let { onNavigateToDetail(it.threadId) }
                        },
                        onPlayAt = { index ->
                            viewModel.playHistoryAt(index)
                            playHistory.getOrNull(index)?.let { onNavigateToDetail(it.threadId) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportStrategyDialog(
    songCount: Int,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入收藏") },
        text = {
            Text("检测到 $songCount 首歌曲，请选择导入方式：")
        },
        confirmButton = {
            TextButton(onClick = onMerge) {
                Text("合并")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(onClick = onReplace) {
                    Text("覆盖")
                }
            }
        }
    )
}

@Composable
private fun PlayAllBar(
    count: Int,
    onPlayAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalButton(
            onClick = onPlayAll,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("播放全部")
        }
        Text(
            text = "共 $count 首",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FavoritesTab(
    favorites: List<FavoriteSong>,
    onPlayAll: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onRemove: (String) -> Unit
) {
    if (favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Default.FavoriteBorder,
            message = "还没有收藏歌曲\n去首页搜索喜欢的歌曲吧"
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { PlayAllBar(count = favorites.size, onPlayAll = onPlayAll) }
            itemsIndexed(favorites, key = { _, song -> song.threadId }) { index, song ->
                SongListItem(
                    title = song.title,
                    artist = song.artist,
                    coverUrl = song.coverUrl,
                    onClick = { onPlayAt(index) },
                    trailing = {
                        IconButton(onClick = { onRemove(song.threadId) }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "取消收藏",
                                tint = Color.Red
                            )
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun HistoryTab(
    history: List<PlayHistoryItem>,
    onPlayAll: () -> Unit,
    onPlayAt: (Int) -> Unit
) {
    if (history.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            message = "还没有播放记录"
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { PlayAllBar(count = history.size, onPlayAll = onPlayAll) }
            itemsIndexed(history, key = { _, item -> item.threadId }) { index, item ->
                SongListItem(
                    title = item.title,
                    artist = item.artist,
                    coverUrl = item.coverUrl,
                    onClick = { onPlayAt(index) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun SongListItem(
    title: String,
    artist: String,
    coverUrl: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingContent = trailing,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
