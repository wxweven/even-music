package com.getmusic.hifiti.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.getmusic.hifiti.BuildConfig
import com.getmusic.hifiti.data.FavoriteSong
import com.getmusic.hifiti.data.FavoritesManager
import com.getmusic.hifiti.ui.update.UpdateDialog
import com.getmusic.hifiti.update.AppUpdater
import com.getmusic.hifiti.update.DownloadState
import com.getmusic.hifiti.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appUpdater = remember { AppUpdater(context) }
    val favoritesManager = remember { FavoritesManager(context) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val downloadState by appUpdater.downloadState.collectAsState()
    var downloadedApkFile by remember { mutableStateOf<java.io.File?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var pendingImportSongs by remember { mutableStateOf<List<FavoriteSong>>(emptyList()) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = favoritesManager.exportToJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            out.write(json.toByteArray())
                        } ?: throw Exception("无法写入文件")
                    }
                    val count = favoritesManager.getAll().size
                    Toast.makeText(context, "已导出 $count 首歌曲", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.use { input ->
                            input.bufferedReader().readText()
                        } ?: throw Exception("无法读取文件")
                    }
                    val songs = favoritesManager.parseFromJson(json)
                    if (songs.isEmpty()) throw Exception("文件中没有歌曲数据")
                    pendingImportSongs = songs
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val checkForUpdate: () -> Unit = {
        if (!isChecking) {
            isChecking = true
            scope.launch {
                val info = appUpdater.checkForUpdate()
                isChecking = false
                if (info != null) {
                    updateInfo = info
                } else {
                    Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val openGitHub: () -> Unit = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wxweven/even-music"))
        context.startActivity(intent)
    }

    if (updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            downloadState = downloadState,
            onConfirm = {
                scope.launch {
                    when (downloadState) {
                        is DownloadState.Downloaded -> {
                            downloadedApkFile?.let { appUpdater.installApk(it) }
                        }
                        is DownloadState.Downloading -> {}
                        else -> {
                            val file = appUpdater.downloadApk(updateInfo!!)
                            if (file != null) {
                                downloadedApkFile = file
                            }
                        }
                    }
                }
            },
            onDismiss = {
                updateInfo = null
                appUpdater.resetState()
            }
        )
    }

    if (pendingImportSongs.isNotEmpty()) {
        ImportStrategyDialog(
            songCount = pendingImportSongs.size,
            onMerge = {
                scope.launch {
                    try {
                        val count = favoritesManager.importSongs(pendingImportSongs, replace = false)
                        pendingImportSongs = emptyList()
                        Toast.makeText(context, "成功导入 $count 首新歌曲", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onReplace = {
                scope.launch {
                    try {
                        val count = favoritesManager.importSongs(pendingImportSongs, replace = true)
                        pendingImportSongs = emptyList()
                        Toast.makeText(context, "成功导入 $count 首歌曲", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { pendingImportSongs = emptyList() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "收藏管理",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("导出收藏") },
                        supportingContent = { Text("将收藏歌曲导出为 JSON 文件") },
                        leadingContent = {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
                            val timestamp = SimpleDateFormat(
                                "yyyyMMdd_HHmmss", Locale.getDefault()
                            ).format(Date())
                            exportLauncher.launch("favorites_$timestamp.json")
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("导入收藏") },
                        supportingContent = { Text("从 JSON 文件导入收藏歌曲") },
                        leadingContent = {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("当前版本") },
                        trailingContent = {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("检查更新") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.clickable(enabled = !isChecking, onClick = checkForUpdate)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("项目地址") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable(onClick = openGitHub)
                    )
                }
            }

            Text(
                text = "HiFiTi Music v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
            )
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
