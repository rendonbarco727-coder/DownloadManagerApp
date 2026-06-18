package com.bmo.downloadmanager.feature.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bmo.downloadmanager.domain.enums.DownloadStatus
import com.bmo.downloadmanager.domain.model.Download

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Descargas") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val state = uiState) {
                is DownloadsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DownloadsUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                is DownloadsUiState.Success -> {
                    if (state.downloads.isEmpty()) {
                        Text(
                            text = "No hay descargas",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = state.downloads,
                                key = { it.id },
                            ) { download ->
                                DownloadItem(
                                    download = download,
                                    onDelete = { viewModel.deleteDownload(download.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    download: Download,
    onDelete: () -> Unit,
) {
    val progress = if (download.totalBytes > 0) {
        download.downloadedBytes.toFloat() / download.totalBytes.toFloat()
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = download.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            StatusChip(status = download.status)

            if (download.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: DownloadStatus) {
    val label = when (status) {
        DownloadStatus.PENDING     -> "Pendiente"
        DownloadStatus.QUEUED      -> "En cola"
        DownloadStatus.DOWNLOADING -> "Descargando"
        DownloadStatus.PAUSED      -> "Pausado"
        DownloadStatus.COMPLETED   -> "Completado"
        DownloadStatus.FAILED      -> "Error"
        DownloadStatus.CANCELLED   -> "Cancelado"
    }
    val color = when (status) {
        DownloadStatus.PENDING     -> MaterialTheme.colorScheme.outline
        DownloadStatus.QUEUED      -> MaterialTheme.colorScheme.secondary
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED      -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED   -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED      -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED   -> MaterialTheme.colorScheme.outline
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color,
        ),
    )
}
