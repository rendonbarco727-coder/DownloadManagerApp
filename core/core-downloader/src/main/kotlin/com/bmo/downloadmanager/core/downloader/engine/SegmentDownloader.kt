package com.bmo.downloadmanager.core.downloader.engine

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.enums.SegmentStatus
import com.bmo.downloadmanager.domain.model.DownloadSegment
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Descarga un único DownloadSegment hacia su archivo temporal.
 *
 * Throttle de progreso: actualizar Room en cada chunk leído del stream (cada
 * pocos KB) generaría cientos de writes por segundo con 4 segmentos en
 * paralelo. En su lugar, se acumula el progreso en memoria y se vuelca a
 * Room cada PROGRESS_UPDATE_INTERVAL_MS o al completar/fallar el segmento,
 * lo que ocurra primero.
 *
 * Reanudación: si tempFilePath ya existe y tiene bytesDownloaded > 0 (porque
 * el segmento fue pausado), la descarga continúa desde
 * rangeStart + bytesDownloaded en lugar de pedir el rango completo otra vez.
 * El archivo temporal se abre en modo append (RandomAccessFile + seek) en
 * vez de sobreescribirse.
 */
class SegmentDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val downloadRepository: DownloadRepository,
) {

    companion object {
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private const val BUFFER_SIZE = 8 * 1024
    }

    /**
     * Ejecuta la descarga de [segment] de forma bloqueante dentro de la
     * corrutina llamante (se espera que el caller ya esté en Dispatchers.IO).
     * Lanza CancellationException si la corrutina es cancelada externamente
     * (pausa/cancelación), lo cual es el mecanismo normal de pausa: no hay
     * un flag de "pause" explícito, se cancela el Job del segmento.
     */
    @Throws(IOException::class)
    suspend fun download(
        segment: DownloadSegment,
        sourceUrl: String,
        refererHeader: String?,
        userAgentHeader: String?,
        tempDir: File,
    ) {
        val tempFile = File(segment.tempFilePath ?: defaultTempFilePath(tempDir, segment))
        if (!tempFile.exists()) {
            tempFile.parentFile?.mkdirs()
            tempFile.createNewFile()
        }

        val alreadyDownloaded = segment.bytesDownloaded
        val resumeFrom = segment.rangeStart + alreadyDownloaded

        // Si ya se descargó todo el rango (por ejemplo tras un crash justo
        // al terminar), no hay nada más que hacer.
        if (resumeFrom > segment.rangeEnd) {
            markStatus(segment, SegmentStatus.COMPLETED, alreadyDownloaded, tempFile.absolutePath)
            return
        }

        markStatus(segment, SegmentStatus.DOWNLOADING, alreadyDownloaded, tempFile.absolutePath)

        val request = buildRangeRequest(sourceUrl, resumeFrom, segment.rangeEnd, refererHeader, userAgentHeader)

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                markStatus(segment, SegmentStatus.FAILED, alreadyDownloaded, tempFile.absolutePath)
                throw IOException("Segmento ${segment.segmentIndex} falló con código ${response.code}")
            }

            val body = response.body ?: throw IOException("Cuerpo vacío para segmento ${segment.segmentIndex}")

            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.seek(alreadyDownloaded)

                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var totalRead = alreadyDownloaded
                    var lastUpdateAt = System.currentTimeMillis()

                    while (coroutineContext.isActive) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        raf.write(buffer, 0, read)
                        totalRead += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateAt >= PROGRESS_UPDATE_INTERVAL_MS) {
                            markStatus(segment, SegmentStatus.DOWNLOADING, totalRead, tempFile.absolutePath)
                            lastUpdateAt = now
                        }
                    }

                    // Si la corrutina fue cancelada (pausa/cancel), se persiste el
                    // último progreso conocido como PAUSED en vez de COMPLETED.
                    if (!coroutineContext.isActive) {
                        markStatus(segment, SegmentStatus.PAUSED, totalRead, tempFile.absolutePath)
                        return
                    }

                    markStatus(segment, SegmentStatus.COMPLETED, totalRead, tempFile.absolutePath)
                }
            }
        }
    }

    private suspend fun markStatus(
        segment: DownloadSegment,
        status: SegmentStatus,
        bytesDownloaded: Long,
        tempFilePath: String,
    ) {
        val updated = segment.copy(
            status = status,
            bytesDownloaded = bytesDownloaded,
            tempFilePath = tempFilePath,
        )
        val result = downloadRepository.updateSegment(updated)
        // No se propaga el error de persistencia hacia arriba: si Room falla
        // al guardar el progreso intermedio, la descarga del segmento debe
        // continuar igual; el próximo update tiene otra oportunidad de
        // persistir. Solo importa loguearlo para diagnóstico.
        if (result is AppResult.Error) {
            // Punto de extensión: conectar a un logger real del proyecto si existe.
        }
    }

    private fun buildRangeRequest(
        url: String,
        start: Long,
        end: Long,
        refererHeader: String?,
        userAgentHeader: String?,
    ): Request {
        var builder = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$start-$end")

        refererHeader?.let { builder = builder.addHeader("Referer", it) }
        userAgentHeader?.let { builder = builder.addHeader("User-Agent", it) }

        return builder.build()
    }

    private fun defaultTempFilePath(tempDir: File, segment: DownloadSegment): String =
        File(tempDir, "download_${segment.downloadId}_segment_${segment.segmentIndex}.part").absolutePath
}
