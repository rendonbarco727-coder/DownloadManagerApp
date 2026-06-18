package com.bmo.downloadmanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una descarga lógica, sin importar si se resuelve como un único stream
 * HTTP o como múltiples segmentos en paralelo. La diferencia entre
 * "descarga simple" y "multi-segmento" vive enteramente en
 * DownloadSegmentEntity: una descarga simple tiene exactamente una fila
 * de segmento (índice 0, rango de bytes completo); una multi-segmento
 * tiene N filas. Ningún campo de aquí cambia de significado según el caso.
 *
 * totalSegments existe como métrica derivada cacheada (evita un COUNT en
 * cada consulta de progreso) pero la fuente de verdad de "cuántos
 * segmentos hay" es siempre la tabla DownloadSegments.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "source_url")
    val sourceUrl: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "destination_uri")
    val destinationUri: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String?,

    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long,

    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0,

    @ColumnInfo(name = "total_segments")
    val totalSegments: Int = 1,

    @ColumnInfo(name = "status")
    val status: DownloadStatus = DownloadStatus.PENDING,

    @ColumnInfo(name = "referer_header")
    val refererHeader: String? = null,

    @ColumnInfo(name = "user_agent_header")
    val userAgentHeader: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null
)

/**
 * Estado del ciclo de vida de una descarga. Persistido como String (ver
 * Converters.kt) en lugar de Int ordinal para que la base de datos sea
 * legible/depurable directamente con herramientas externas y para que
 * reordenar este enum en el futuro no corrompa datos ya guardados.
 */
enum class DownloadStatus {
    PENDING,
    RESOLVING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
