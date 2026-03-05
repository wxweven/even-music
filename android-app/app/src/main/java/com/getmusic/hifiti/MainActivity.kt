package com.getmusic.hifiti

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.getmusic.hifiti.player.MusicPlayerManager
import com.getmusic.hifiti.ui.detail.DetailScreen
import com.getmusic.hifiti.ui.my.MyScreen
import com.getmusic.hifiti.ui.search.SearchScreen
import com.getmusic.hifiti.ui.settings.SettingsScreen
import com.getmusic.hifiti.ui.theme.HiFiTiTheme
import com.getmusic.hifiti.ui.update.UpdateDialog
import com.getmusic.hifiti.update.AppUpdater
import com.getmusic.hifiti.update.DownloadState
import com.getmusic.hifiti.update.UpdateInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        val playerManager = MusicPlayerManager.getInstance(applicationContext)

        setContent {
            HiFiTiTheme {
                HiFiTiApp(playerManager)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun HiFiTiApp(playerManager: MusicPlayerManager) {
    val navController = rememberNavController()
    val playerState by playerManager.playerState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appUpdater = remember { AppUpdater(context) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val downloadState by appUpdater.downloadState.collectAsState()
    var downloadedApkFile by remember { mutableStateOf<java.io.File?>(null) }

    LaunchedEffect(Unit) {
        updateInfo = appUpdater.checkForUpdate()
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

    val isOnTabRoute = currentRoute == "search" || currentRoute == "my"
    val hasPlayingSong = playerState.currentSong != null

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "search",
                    onClick = {
                        navController.navigate("search") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = isOnTabRoute
                            }
                            launchSingleTop = true
                            restoreState = isOnTabRoute
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = currentRoute == "detail/{threadId}",
                    enabled = hasPlayingSong,
                    onClick = {
                        val threadId = playerState.currentSong?.threadId
                        if (!threadId.isNullOrEmpty()) {
                            navController.navigate("detail/$threadId") {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = {
                        Icon(Icons.Default.MusicNote, contentDescription = "播放")
                    },
                    label = { Text("播放") }
                )
                NavigationBarItem(
                    selected = currentRoute == "my",
                    onClick = {
                        navController.navigate("my") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = isOnTabRoute
                            }
                            launchSingleTop = true
                            restoreState = isOnTabRoute
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                    label = { Text("我的") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = "search"
            ) {
                composable("search") {
                    SearchScreen(
                        onItemClick = { item ->
                            navController.navigate("detail/${item.threadId}")
                        }
                    )
                }

                composable("my") {
                    MyScreen(
                        onNavigateToDetail = { threadId ->
                            navController.navigate("detail/$threadId") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "detail/{threadId}",
                    arguments = listOf(navArgument("threadId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val threadId = backStackEntry.arguments?.getString("threadId") ?: return@composable
                    DetailScreen(
                        threadId = threadId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
