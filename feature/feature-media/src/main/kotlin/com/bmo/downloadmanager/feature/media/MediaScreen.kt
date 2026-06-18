package com.bmo.downloadmanager.feature.media

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bmo.downloadmanager.domain.enums.DownloadStatus
import com.bmo.downloadmanager.domain.model.Download

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val libraryState    by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val activeState     by viewModel.activeDownloadsUiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Biblioteca", "En progreso")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Media") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick  = { selectedTabIndex = index },
                        text     = { Text(title) },
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> LibraryTab(state = libraryState)
                1 -> ActiveTab(state = activeState)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab: Biblioteca (archivos COMPLETED)
// ---------------------------------------------------------------------------

@Composable
private fun LibraryTab(state: MediaLibraryUiState) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is MediaLibraryUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is MediaLibraryUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is MediaLibraryUiState.Success -> {
                if (state.files.isEmpty()) {
                    Text(
                        text = "No hay archivos completados",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.files, key = { it.id }) { download ->
                            MediaFileItem(
                                download = download,
                                onClick  = {
                                    val uri    = Uri.parse(download.destinationUri)
                                    val mime   = download.mimeType ?: "*/*"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mime)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(intent)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaFileItem(
    download: Download,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = download.fileName,
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = download.mimeType ?: "archivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text  = formatBytes(download.totalBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tab: En progreso (DOWNLOADING / PAUSED / QUEUED)
// ---------------------------------------------------------------------------

@Composable
private fun ActiveTab(state: ActiveDownloadsUiState) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is ActiveDownloadsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ActiveDownloadsUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is ActiveDownloadsUiState.Success -> {
                if (state.downloads.isEmpty()) {
                    Text(
                        text = "No hay descargas activas",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.downloads, key = { it.id }) { download ->
                            ActiveDownloadItem(download = download)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadItem(download: Download) {
    val progress = if (download.totalBytes > 0L) {
        (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text     = download.fileName,
                style    = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatusLabel(status = download.status)
                Text(
                    text  = "${(progress * 100).toInt()}% · ${formatBytes(download.downloadedBytes)} / ${formatBytes(download.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            if (download.totalSegments > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "${download.totalSegments} segmentos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusLabel(status: DownloadStatus) {
    val (label, color) = when (status) {
        DownloadStatus.PENDING     -> "Pendiente"   to MaterialTheme.colorScheme.outline
        DownloadStatus.QUEUED      -> "En cola"     to MaterialTheme.colorScheme.secondary
        DownloadStatus.DOWNLOADING -> "Descargando" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED      -> "Pausado"     to MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED   -> "Completado"  to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED      -> "Error"       to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED   -> "Cancelado"   to MaterialTheme.colorScheme.outline
    }
    SuggestionChip(
        onClick = {},
        label   = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor     = color,
        ),
    )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun formatBytes(bytes: Long): String = when {
    bytes < 0L          -> "?"
    bytes < 1_024L      -> "$bytes B"
    bytes < 1_048_576L  -> "${"%.1f".format(bytes / 1_024f)} KB"
    bytes < 1_073_741_824L -> "${"%.1f".format(bytes / 1_048_576f)} MB"
    else                -> "${"%.2f".format(bytes / 1_073_741_824f)} GB"
}
