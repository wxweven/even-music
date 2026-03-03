package com.getmusic.hifiti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.getmusic.hifiti.ui.detail.DetailScreen
import com.getmusic.hifiti.ui.search.SearchScreen
import com.getmusic.hifiti.ui.theme.HiFiTiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HiFiTiTheme {
                HiFiTiApp()
            }
        }
    }
}

@Composable
fun HiFiTiApp() {
    val navController = rememberNavController()

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
