package com.bmo.downloadmanager.core.downloader.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bmo.downloadmanager.core.downloader.engine.DownloadProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Construye la notificación agregada del ForegroundService.
 *
 * Decisión: una sola notificación para todas las descargas activas, no una
 * por descarga. Si hay una activa, muestra su nombre y porcentaje. Si hay
 * varias, muestra un resumen ("3 descargas activas") con el progreso
 * combinado. Esto se revisita más adelante si se necesita una notificación
 * expandible por descarga (no es parte de esta iteración).
 */
@Singleton
class DownloadNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        const val CHANNEL_ID = "download_progress_channel"
        const val NOTIFICATION_ID = 1001
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Descargas",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Progreso de descargas en curso"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildAggregated(progresses: List<DownloadProgress>): Notification {
        ensureChannel()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        when {
            progresses.isEmpty() -> {
                builder
                    .setContentTitle("Descargas")
                    .setContentText("Preparando...")
            }

            progresses.size == 1 -> {
                val p = progresses.first()
                val percent = percentOf(p)
                builder
                    .setContentTitle(p.fileName)
                    .setContentText("$percent%")
                    .setProgress(100, percent, p.totalBytes <= 0)
            }

            else -> {
                val totalDownloaded = progresses.sumOf { it.downloadedBytes }
                val totalSize = progresses.sumOf { it.totalBytes.coerceAtLeast(0) }
                val percent = if (totalSize > 0) {
                    ((totalDownloaded.toDouble() / totalSize.toDouble()) * 100).toInt().coerceIn(0, 100)
                } else 0

                builder
                    .setContentTitle("${progresses.size} descargas activas")
                    .setContentText("$percent% completado en total")
                    .setProgress(100, percent, totalSize <= 0)
            }
        }

        return builder.build()
    }

    private fun percentOf(p: DownloadProgress): Int {
        if (p.totalBytes <= 0) return 0
        return ((p.downloadedBytes.toDouble() / p.totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
    }
}
