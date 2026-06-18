package com.bmo.downloadmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity única de la app (patrón single-activity + Navigation Compose).
 * Por ahora solo confirma que el árbol de compilación end-to-end funciona:
 * app -> core-ui -> Compose, y que Hilt inyecta correctamente en una Activity.
 * La navegación real entre feature-* se conecta en un módulo posterior.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DownloadManagerRoot()
        }
    }
}

@Composable
private fun DownloadManagerRoot() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text(text = "Download Manager — Módulo 1 en construcción")
        }
    }
}
