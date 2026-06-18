package com.bmo.downloadmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmo.downloadmanager.core.database.entity.DownloadEntity
import com.bmo.downloadmanager.core.database.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeById(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY created_at DESC")
    fun observeByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query(
        """
        UPDATE downloads
        SET downloaded_bytes = :downloadedBytes, status = :status
        WHERE id = :id
        """
    )
    suspend fun updateProgress(id: Long, downloadedBytes: Long, status: DownloadStatus)

    @Query(
        """
        UPDATE downloads
        SET status = :status, completed_at = :completedAt, error_message = :errorMessage
        WHERE id = :id
        """
    )
    suspend fun updateTerminalState(
        id: Long,
        status: DownloadStatus,
        completedAt: Long?,
        errorMessage: String?
    )

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)
}
