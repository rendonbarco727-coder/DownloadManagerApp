package com.bmo.downloadmanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un rango de bytes de una descarga (ver comentario de diseño en
 * DownloadEntity sobre cómo esto unifica el caso simple y multi-segmento).
 *
 * onDelete = CASCADE: al borrar una DownloadEntity, sus segmentos se
 * eliminan automáticamente. Esto es deliberado: un segmento huérfano sin
 * descarga padre no tiene significado en este modelo, a diferencia de
 * BrowserHistory que sí debe sobrevivir aunque se cierre una pestaña.
 */
@Entity(
    tableName = "download_segments",
    foreignKeys = [
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["download_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["download_id"])]
)
data class DownloadSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "download_id")
    val downloadId: Long,

    @ColumnInfo(name = "segment_index")
    val segmentIndex: Int,

    @ColumnInfo(name = "range_start")
    val rangeStart: Long,

    @ColumnInfo(name = "range_end")
    val rangeEnd: Long,

    @ColumnInfo(name = "bytes_downloaded")
    val bytesDownloaded: Long = 0,

    @ColumnInfo(name = "segment_url")
    val segmentUrl: String,

    @ColumnInfo(name = "status")
    val status: SegmentStatus = SegmentStatus.PENDING,

    @ColumnInfo(name = "temp_file_path")
    val tempFilePath: String? = null
)

enum class SegmentStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}
