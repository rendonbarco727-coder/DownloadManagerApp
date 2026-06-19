package com.bmo.downloadmanager.core.downloader.engine

/**
 * Resultado de inspeccionar la URL de origen antes de empezar a descargar.
 *
 * Por qué existe separado de Download/DownloadSegment (modelos de dominio):
 * esto es información transitoria obtenida de un HEAD request, todavía no
 * persistida. Una vez decidido el plan, se traduce a entidades DownloadSegment
 * reales que sí se insertan en Room.
 */
data class RemoteFileInfo(
    val contentLength: Long,
    val acceptsRanges: Boolean,
    val mimeType: String?,
)

/**
 * Plan de segmentación calculado a partir de RemoteFileInfo.
 * segments es la lista de rangos [start, end] inclusive, uno por segmento.
 */
data class SegmentPlan(
    val totalBytes: Long,
    val segments: List<LongRange>,
)

object SegmentPlanner {

    /** Por debajo de este tamaño, no vale la pena paralelizar: 1 solo segmento. */
    const val PARALLEL_THRESHOLD_BYTES = 5L * 1024 * 1024 // 5 MB

    const val DEFAULT_SEGMENT_COUNT = 4

    /**
     * Decide en cuántos segmentos dividir la descarga.
     * Si el servidor no soporta Range o el archivo es chico, devuelve un solo
     * segmento que cubre [0, contentLength - 1].
     * Si contentLength es desconocido (-1, servidor no lo informó), también
     * se fuerza a 1 segmento porque no hay forma de calcular rangos.
     */
    fun plan(
        info: RemoteFileInfo,
        requestedSegmentCount: Int = DEFAULT_SEGMENT_COUNT,
    ): SegmentPlan {
        val total = info.contentLength

        if (!info.acceptsRanges || total <= 0 || total < PARALLEL_THRESHOLD_BYTES) {
            val end = if (total > 0) total - 1 else 0L
            return SegmentPlan(totalBytes = total, segments = listOf(0L..end))
        }

        val segmentCount = requestedSegmentCount.coerceIn(1, 16)
        val baseSize = total / segmentCount
        val ranges = mutableListOf<LongRange>()

        for (i in 0 until segmentCount) {
            val start = i * baseSize
            val end = if (i == segmentCount - 1) total - 1 else (start + baseSize - 1)
            ranges += start..end
        }

        return SegmentPlan(totalBytes = total, segments = ranges)
    }
}
