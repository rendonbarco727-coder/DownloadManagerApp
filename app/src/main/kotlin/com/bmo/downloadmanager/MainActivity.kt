package com.bmo.downloadmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bmo.downloadmanager.core.ui.theme.DownloadManagerTheme
import com.bmo.downloadmanager.navigation.AppDestination
import com.bmo.downloadmanager.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DownloadManagerRoot() }
    }
}

@Composable
private fun DownloadManagerRoot() {
    DownloadManagerTheme {
        Surface {
            val navController = rememberNavController()
            Scaffold(
                bottomBar = { AppBottomBar(navController) }
            ) { padding ->
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    NavigationBar {
        AppDestination.entries.forEach { destination ->
            val selected = currentRoute?.hierarchy
                ?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { Text(destination.emoji, style = MaterialTheme.typography.titleMedium) },
                label = { Text(destination.label) },
            )
        }
    }
}
