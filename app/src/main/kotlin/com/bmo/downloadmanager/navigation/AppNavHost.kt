package com.bmo.downloadmanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bmo.downloadmanager.feature.browser.BrowserScreen
import com.bmo.downloadmanager.feature.downloads.DownloadsScreen
import com.bmo.downloadmanager.feature.history.HistoryScreen
import com.bmo.downloadmanager.feature.media.MediaScreen
import com.bmo.downloadmanager.feature.settings.SettingsScreen

/**
 * Grafo de navegación raíz. Cada destino es una pantalla completa de su
 * propio feature module; feature-* no se conocen entre sí, solo :app
 * los une aquí.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.DOWNLOADS.route,
        modifier = modifier,
    ) {
        composable(AppDestination.DOWNLOADS.route) { DownloadsScreen() }
        composable(AppDestination.BROWSER.route) { BrowserScreen() }
        composable(AppDestination.MEDIA.route) { MediaScreen() }
        composable(AppDestination.HISTORY.route) { HistoryScreen() }
        composable(AppDestination.SETTINGS.route) { SettingsScreen() }
    }
}
