package com.bmo.downloadmanager.core.downloader.engine

import com.bmo.downloadmanager.domain.model.DownloadSegment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Concatena los archivos temporales de los segmentos en destinationPath,
 * respetando el orden de segmentIndex, y borra los temporales al finalizar.
 *
 * destinationUri se trata como path de archivo plano (no SAF/content://) por
 * decisión explícita: SAF queda para una iteración futura. Esto simplifica
 * el merge a FileOutputStream directo sobre File(destinationUri).
 */
object SegmentMerger {

    private const val COPY_BUFFER_SIZE = 64 * 1024

    @Throws(IOException::class)
    fun merge(segments: List<DownloadSegment>, destinationPath: String) {
        val ordered = segments.sortedBy { it.segmentIndex }

        val missing = ordered.filter { it.tempFilePath == null || !File(it.tempFilePath).exists() }
        if (missing.isNotEmpty()) {
            throw IOException(
                "No se puede mergear: faltan archivos temporales para segmentos ${missing.map { it.segmentIndex }}"
            )
        }

        val destinationFile = File(destinationPath)
        destinationFile.parentFile?.mkdirs()

        FileOutputStream(destinationFile).use { output ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            for (segment in ordered) {
                val tempFile = File(segment.tempFilePath!!)
                tempFile.inputStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                }
            }
        }

        deleteTempFiles(ordered)
    }

    fun deleteTempFiles(segments: List<DownloadSegment>) {
        for (segment in segments) {
            segment.tempFilePath?.let { path ->
                runCatching { File(path).delete() }
            }
        }
    }
}
