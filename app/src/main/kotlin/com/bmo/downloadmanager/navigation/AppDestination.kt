package com.bmo.downloadmanager.navigation

/**
 * Cada item de la bottom navigation. `route` se usa tal cual como ruta
 * de NavHost. El icono se representa como emoji para evitar dependencia
 * de material-icons que no está en el classpath de :app.
 */
enum class AppDestination(
    val route: String,
    val label: String,
    val emoji: String,
) {
    DOWNLOADS(route = "downloads", label = "Descargas", emoji = "⬇"),
    BROWSER(route = "browser",    label = "Navegador", emoji = "🌐"),
    MEDIA(route = "media",        label = "Media",     emoji = "▶"),
    HISTORY(route = "history",    label = "Historial", emoji = "🕑"),
    SETTINGS(route = "settings",  label = "Ajustes",   emoji = "⚙"),
}
