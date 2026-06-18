package com.bmo.downloadmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmo.downloadmanager.core.database.entity.DownloadSegmentEntity
import com.bmo.downloadmanager.core.database.entity.SegmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadSegmentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(segment: DownloadSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(segments: List<DownloadSegmentEntity>): List<Long>

    @Update
    suspend fun update(segment: DownloadSegmentEntity)

    @Query("SELECT * FROM download_segments WHERE download_id = :downloadId ORDER BY segment_index ASC")
    suspend fun getSegmentsForDownload(downloadId: Long): List<DownloadSegmentEntity>

    @Query("SELECT * FROM download_segments WHERE download_id = :downloadId ORDER BY segment_index ASC")
    fun observeSegmentsForDownload(downloadId: Long): Flow<List<DownloadSegmentEntity>>

    @Query(
        """
        UPDATE download_segments
        SET bytes_downloaded = :bytesDownloaded, status = :status
        WHERE id = :id
        """
    )
    suspend fun updateProgress(id: Long, bytesDownloaded: Long, status: SegmentStatus)

    /**
     * Suma de bytes descargados across todos los segmentos de una
     * descarga. Esta query es la fuente de verdad para el progreso real
     * mostrado en UI; DownloadEntity.downloadedBytes es una copia
     * cacheada que se actualiza desde este valor, no al revés.
     */
    @Query("SELECT COALESCE(SUM(bytes_downloaded), 0) FROM download_segments WHERE download_id = :downloadId")
    suspend fun getTotalDownloadedBytes(downloadId: Long): Long

    @Query("SELECT COUNT(*) FROM download_segments WHERE download_id = :downloadId AND status = :status")
    suspend fun countByStatus(downloadId: Long, status: SegmentStatus): Int

    @Query("DELETE FROM download_segments WHERE download_id = :downloadId")
    suspend fun deleteAllForDownload(downloadId: Long)
}
