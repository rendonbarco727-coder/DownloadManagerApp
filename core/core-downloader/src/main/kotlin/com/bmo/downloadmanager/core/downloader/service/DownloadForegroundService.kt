package com.bmo.downloadmanager.core.downloader.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.bmo.downloadmanager.core.downloader.engine.DownloadOrchestrator
import com.bmo.downloadmanager.core.downloader.notification.DownloadNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Service en foreground que ejecuta y supervisa descargas activas.
 *
 * Comunicación: por Intent con un extra ACTION (START/PAUSE/RESUME/CANCEL)
 * y EXTRA_DOWNLOAD_ID. No es un Bound Service: el progreso se observa por
 * Room (Flow) desde la UI, no llamando directamente a este Service. Esto
 * mantiene al Service como un componente desacoplado que solo necesita
 * recibir órdenes, no devolver datos de vuelta de forma síncrona.
 *
 * Ciclo de vida: arranca en foreground en el primer START y se detiene solo
 * (stopSelf) cuando DownloadOrchestrator ya no tiene ninguna descarga
 * activa. Se vuelve a evaluar esa condición tras cada pause/cancel y en
 * cada tick del refresco de notificación.
 */
@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject
    lateinit var orchestrator: DownloadOrchestrator

    @Inject
    lateinit var notificationHelper: DownloadNotificationHelper

    // SupervisorJob propio del Service: de aquí cuelgan los Jobs raíz de
    // cada descarga que arranca DownloadOrchestrator.start(). Si el Service
    // es destruido, cancelar este scope cancela cualquier descarga que
    // quedara corriendo (no debería pasar en condiciones normales, ya que
    // el Service solo se detiene cuando no hay descargas activas).
    private val serviceScope: CoroutineScope = MainScope()
    private var notificationTickerJob: Job? = null

    private lateinit var tempDir: File

    override fun onCreate() {
        super.onCreate()
        tempDir = File(getExternalFilesDir(null), "downloads_tmp")
        tempDir.mkdirs()
        notificationHelper.ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(EXTRA_ACTION)
        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L

        when (action) {
            ACTION_START, ACTION_RESUME -> {
                if (downloadId <= 0) return START_NOT_STICKY
                startForeground(
                    DownloadNotificationHelper.NOTIFICATION_ID,
                    notificationHelper.buildAggregated(orchestrator.allProgress()),
                )
                orchestrator.start(downloadId, serviceScope, tempDir)
                ensureNotificationTicker()
            }

            ACTION_PAUSE -> {
                serviceScope.launch {
                    orchestrator.pause(downloadId)
                    stopIfIdle()
                }
            }

            ACTION_CANCEL -> {
                serviceScope.launch {
                    orchestrator.cancel(downloadId)
                    stopIfIdle()
                }
            }
        }

        // START_STICKY: si el sistema mata el proceso por presión de
        // memoria, Android intenta recrear el Service sin reenviar el
        // último Intent (intent llegaría null en onStartCommand). Las
        // descargas que quedaron a medias se retoman manualmente desde la
        // UI (resume), no automáticamente al recrear el Service.
        return START_STICKY
    }

    private fun ensureNotificationTicker() {
        if (notificationTickerJob?.isActive == true) return

        notificationTickerJob = serviceScope.launch {
            while (orchestrator.hasActiveDownloads()) {
                val notification = notificationHelper.buildAggregated(orchestrator.allProgress())
                startForeground(DownloadNotificationHelper.NOTIFICATION_ID, notification)
                delay(NOTIFICATION_REFRESH_INTERVAL_MS)
            }
            stopIfIdle()
        }
    }

    private fun stopIfIdle() {
        if (!orchestrator.hasActiveDownloads()) {
            notificationTickerJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        notificationTickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_CANCEL = "action_cancel"

        private const val NOTIFICATION_REFRESH_INTERVAL_MS = 1000L

        fun buildIntent(context: Context, action: String, downloadId: Long): Intent =
            Intent(context, DownloadForegroundService::class.java).apply {
                putExtra(EXTRA_ACTION, action)
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
    }
}
