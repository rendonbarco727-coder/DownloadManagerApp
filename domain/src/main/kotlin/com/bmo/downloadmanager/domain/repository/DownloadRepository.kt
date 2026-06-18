package com.bmo.downloadmanager.domain.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloads(): Flow<AppResult<List<Download>>>
    suspend fun getDownloadById(id: Long): AppResult<Download>
    suspend fun insertDownload(download: Download): AppResult<Long>
    suspend fun updateDownload(download: Download): AppResult<Unit>
    suspend fun deleteDownload(id: Long): AppResult<Unit>
    fun getSegmentsForDownload(downloadId: Long): Flow<AppResult<List<DownloadSegment>>>
    suspend fun insertSegment(segment: DownloadSegment): AppResult<Long>
    suspend fun updateSegment(segment: DownloadSegment): AppResult<Unit>
}
