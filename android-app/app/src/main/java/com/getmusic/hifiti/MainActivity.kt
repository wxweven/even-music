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
import androidx.compose.material3.Scaffold
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.getmusic.hifiti.player.MusicPlayerManager
import com.getmusic.hifiti.ui.detail.DetailScreen
import com.getmusic.hifiti.ui.favorites.FavoritesScreen
import com.getmusic.hifiti.ui.player.MiniPlayerBar
import com.getmusic.hifiti.ui.search.SearchScreen
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

    Scaffold(
        bottomBar = {
            MiniPlayerBar(
                playerState = playerState,
                onPlayPause = { playerManager.togglePlayPause() },
                onClose = { playerManager.stop() },
                onClick = {
                    val threadId = playerState.currentSong?.threadId
                    if (!threadId.isNullOrEmpty()) {
                        navController.navigate("detail/$threadId") {
                            launchSingleTop = true
                        }
                    }
                }
            )
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
                        },
                        onNavigateToFavorites = {
                            navController.navigate("favorites")
                        }
                    )
                }

                composable("favorites") {
                    FavoritesScreen(
                        onBack = { navController.popBackStack() },
                        onItemClick = { threadId ->
                            navController.navigate("detail/$threadId")
                        }
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
