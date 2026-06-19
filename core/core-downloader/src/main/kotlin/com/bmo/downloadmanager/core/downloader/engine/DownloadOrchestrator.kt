package com.bmo.downloadmanager.core.downloader.engine

import com.bmo.downloadmanager.core.common.dispatcher.DispatcherProvider
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.enums.DownloadStatus
import com.bmo.downloadmanager.domain.enums.SegmentStatus
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado agregado de una descarga en curso, expuesto al Service para que
 * actualice la notificación sin tener que recalcular sumas de segmentos en
 * cada tick.
 */
data class DownloadProgress(
    val downloadId: Long,
    val fileName: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val status: DownloadStatus,
)

/**
 * Coordina el ciclo de vida completo de cada descarga activa.
 *
 * Por qué un SupervisorJob por descarga (no uno global): si un segmento
 * falla con una excepción no controlada, no debe cancelar los demás
 * segmentos de la MISMA descarga (cada hijo bajo SupervisorJob falla de
 * forma aislada), pero tampoco debe afectar a OTRAS descargas corriendo en
 * paralelo. Cada Download obtiene su propio SupervisorJob raíz; dentro de
 * ese Job, los N coroutines de segmentos son hijos directos.
 *
 * pause/cancel funcionan cancelando ese Job raíz: cancelar el padre cancela
 * automáticamente todos los hijos (los segmentos en curso), que es lo que
 * SegmentDownloader interpreta como señal de pausa (ver su manejo de
 * coroutineContext.isActive).
 *
 * Esta clase es @Singleton porque debe sobrevivir mientras el Service esté
 * vivo y mantener el mapa de Jobs activos consistente entre llamadas de
 * distintos componentes (Service, futuros entry points).
 */
@Singleton
class DownloadOrchestrator @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val remoteFileInspector: RemoteFileInspector,
    private val segmentDownloader: SegmentDownloader,
    private val dispatcherProvider: DispatcherProvider,
) {

    // Job raíz de cada descarga activa, indexado por downloadId. Cancelar
    // este Job es la forma de pausar o cancelar esa descarga específica.
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    // Último progreso conocido de cada descarga activa, para que el Service
    // pueda leerlo sin golpear Room en cada tick de notificación.
    private val progressState = ConcurrentHashMap<Long, DownloadProgress>()

    fun isActive(downloadId: Long): Boolean = activeJobs.containsKey(downloadId)

    fun currentProgress(downloadId: Long): DownloadProgress? = progressState[downloadId]

    fun allProgress(): List<DownloadProgress> = progressState.values.toList()

    fun hasActiveDownloads(): Boolean = activeJobs.isNotEmpty()

    /**
     * Arranca (o reanuda) la descarga de [downloadId] dentro de [scope].
     * scope es el CoroutineScope del Service llamante (normalmente atado a
     * su propio SupervisorJob de Service), del cual este método deriva un
     * Job hijo dedicado a esta descarga.
     *
     * No hace nada si la descarga ya está activa (evita doble-arranque si
     * el Service recibe START repetido para el mismo id).
     */
    fun start(downloadId: Long, scope: CoroutineScope, tempDir: File) {
        if (activeJobs.containsKey(downloadId)) return

        val downloadJob = scope.launch(dispatcherProvider.io + SupervisorJob()) {
            runDownload(downloadId, tempDir)
        }

        activeJobs[downloadId] = downloadJob

        downloadJob.invokeOnCompletion {
            activeJobs.remove(downloadId)
        }
    }

    /**
     * Pausa la descarga cancelando su Job raíz. Los segmentos en curso
     * persisten su progreso parcial como PAUSED antes de terminar (ver
     * SegmentDownloader). No borra temporales.
     */
    suspend fun pause(downloadId: Long) {
        activeJobs[downloadId]?.cancelAndJoin()
        activeJobs.remove(downloadId)

        val result = downloadRepository.getDownloadById(downloadId)
        if (result is AppResult.Success) {
            downloadRepository.updateDownload(result.data.copy(status = DownloadStatus.PAUSED))
        }
    }

    /** Resume es simplemente start(): los segmentos ya saben retomar desde su bytesDownloaded. */
    fun resume(downloadId: Long, scope: CoroutineScope, tempDir: File) {
        start(downloadId, scope, tempDir)
    }

    /**
     * Cancela la descarga, borra sus temporales y marca status = CANCELLED.
     */
    suspend fun cancel(downloadId: Long) {
        activeJobs[downloadId]?.cancelAndJoin()
        activeJobs.remove(downloadId)
        progressState.remove(downloadId)

        val segments = downloadRepository.getSegmentsForDownload(downloadId).firstOrNullSuccess()
        segments?.let { SegmentMerger.deleteTempFiles(it) }

        val downloadResult = downloadRepository.getDownloadById(downloadId)
        if (downloadResult is AppResult.Success) {
            downloadRepository.updateDownload(
                downloadResult.data.copy(status = DownloadStatus.CANCELLED)
            )
        }
    }

    private suspend fun runDownload(downloadId: Long, tempDir: File) {
        val downloadResult = downloadRepository.getDownloadById(downloadId)
        if (downloadResult !is AppResult.Success) {
            return
        }
        val download = downloadResult.data

        try {
            downloadRepository.updateDownload(download.copy(status = DownloadStatus.DOWNLOADING))
            publishProgress(download)

            val existingSegments = downloadRepository.getSegmentsForDownload(downloadId).firstOrNullSuccess()
            val segments = if (existingSegments.isNullOrEmpty()) {
                planAndPersistSegments(download)
            } else {
                existingSegments
            }

            downloadSegmentsInParallel(download, segments, tempDir)

            val finalSegments = downloadRepository.getSegmentsForDownload(downloadId).firstOrNullSuccess()
                ?: emptyList()

            val allCompleted = finalSegments.isNotEmpty() && finalSegments.all { it.status == SegmentStatus.COMPLETED }

            if (allCompleted) {
                finishDownload(download, finalSegments)
            } else {
                // No todos completaron: o fue pausado (cancelación cooperativa)
                // o algún segmento falló. Se distingue por si hay algún FAILED.
                val anyFailed = finalSegments.any { it.status == SegmentStatus.FAILED }
                val newStatus = if (anyFailed) DownloadStatus.FAILED else DownloadStatus.PAUSED
                downloadRepository.updateDownload(download.copy(status = newStatus))
            }
        } catch (e: CancellationException) {
            // Pausa cooperativa normal: el estado por segmento ya quedó
            // persistido como PAUSED dentro de SegmentDownloader. No se
            // sobreescribe aquí para no pisar un estado más específico.
            throw e
        } catch (e: Exception) {
            downloadRepository.updateDownload(
                download.copy(status = DownloadStatus.FAILED, errorMessage = e.message)
            )
        } finally {
            progressState.remove(downloadId)
        }
    }

    private suspend fun planAndPersistSegments(download: Download): List<DownloadSegment> {
        val info = remoteFileInspector.inspect(
            url = download.sourceUrl,
            refererHeader = download.refererHeader,
            userAgentHeader = download.userAgentHeader,
        )

        val plan = SegmentPlanner.plan(info, requestedSegmentCount = download.totalSegments)

        downloadRepository.updateDownload(
            download.copy(totalBytes = plan.totalBytes, totalSegments = plan.segments.size)
        )

        val segments = plan.segments.mapIndexed { index, range ->
            DownloadSegment(
                downloadId = download.id,
                segmentIndex = index,
                rangeStart = range.first,
                rangeEnd = range.last,
                segmentUrl = download.sourceUrl,
                status = SegmentStatus.PENDING,
            )
        }

        segments.forEach { downloadRepository.insertSegment(it) }

        return downloadRepository.getSegmentsForDownload(download.id).firstOrNullSuccess() ?: segments
    }

    private suspend fun downloadSegmentsInParallel(
        download: Download,
        segments: List<DownloadSegment>,
        tempDir: File,
    ) = coroutineScope {
        val pending = segments.filter { it.status != SegmentStatus.COMPLETED }

        val deferreds = pending.map { segment ->
            async {
                runCatching {
                    segmentDownloader.download(
                        segment = segment,
                        sourceUrl = segment.segmentUrl,
                        refererHeader = download.refererHeader,
                        userAgentHeader = download.userAgentHeader,
                        tempDir = tempDir,
                    )
                }

                // Tras cada segmento (éxito, falla o pausa), refresca el
                // progreso agregado visible al Service.
                publishProgressFromRepository(download)
            }
        }

        deferreds.awaitAll()
    }

    private suspend fun finishDownload(download: Download, segments: List<DownloadSegment>) {
        SegmentMerger.merge(segments, download.destinationUri)

        downloadRepository.updateDownload(
            download.copy(
                status = DownloadStatus.COMPLETED,
                downloadedBytes = download.totalBytes,
                completedAt = System.currentTimeMillis(),
            )
        )

        progressState[download.id] = DownloadProgress(
            downloadId = download.id,
            fileName = download.fileName,
            downloadedBytes = download.totalBytes,
            totalBytes = download.totalBytes,
            status = DownloadStatus.COMPLETED,
        )
    }

    private suspend fun publishProgressFromRepository(download: Download) {
        val segments = downloadRepository.getSegmentsForDownload(download.id).firstOrNullSuccess() ?: return
        val downloadedBytes = segments.sumOf { it.bytesDownloaded }

        progressState[download.id] = DownloadProgress(
            downloadId = download.id,
            fileName = download.fileName,
            downloadedBytes = downloadedBytes,
            totalBytes = download.totalBytes,
            status = DownloadStatus.DOWNLOADING,
        )

        downloadRepository.updateDownload(download.copy(downloadedBytes = downloadedBytes))
    }

    private fun publishProgress(download: Download) {
        progressState[download.id] = DownloadProgress(
            downloadId = download.id,
            fileName = download.fileName,
            downloadedBytes = download.downloadedBytes,
            totalBytes = download.totalBytes,
            status = download.status,
        )
    }
}

/**
 * Helper para tomar el primer valor exitoso (o el primer error, lo que
 * llegue primero) de un Flow<AppResult<T>> sin suscribirse indefinidamente.
 * Usado en operaciones puntuales (no observación continua) donde el
 * repository solo expone Flow.
 */
private suspend fun <T> Flow<AppResult<T>>.firstOrNullSuccess(): T? {
    var result: T? = null
    runCatching {
        first { emission ->
            if (emission is AppResult.Success) {
                result = emission.data
                true
            } else {
                emission is AppResult.Error
            }
        }
    }
    return result
}
