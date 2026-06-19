package com.bmo.downloadmanager.core.downloader.manager

import android.content.Context
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.enums.DownloadStatus
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import com.bmo.downloadmanager.core.downloader.service.DownloadForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point público del motor de descarga, pensado para inyectarse vía
 * Hilt en ViewModels de feature-downloads (botones pausar/cancelar) y en
 * feature-browser (al interceptar una URL de descarga detectada en el
 * WebView).
 *
 * Esta clase NO ejecuta descargas directamente: persiste el Download en
 * Room y delega la ejecución real al DownloadForegroundService vía Intent.
 * Esto mantiene la regla de Android de que el trabajo de larga duración
 * vive en un Service, no en un objeto inyectado en un ViewModel que muere
 * con su Activity.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository,
) {

    suspend fun enqueue(download: Download): Long {
        val toInsert = download.copy(status = DownloadStatus.QUEUED)
        val result = downloadRepository.insertDownload(toInsert)

        val id = when (result) {
            is AppResult.Success -> result.data
            else -> return -1L
        }

        sendCommand(DownloadForegroundService.ACTION_START, id)
        return id
    }

    fun pause(downloadId: Long) {
        sendCommand(DownloadForegroundService.ACTION_PAUSE, downloadId)
    }

    fun resume(downloadId: Long) {
        sendCommand(DownloadForegroundService.ACTION_RESUME, downloadId)
    }

    fun cancel(downloadId: Long) {
        sendCommand(DownloadForegroundService.ACTION_CANCEL, downloadId)
    }

    private fun sendCommand(action: String, downloadId: Long) {
        val intent = DownloadForegroundService.buildIntent(context, action, downloadId)
        context.startForegroundService(intent)
    }
}
