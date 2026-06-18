package com.bmo.downloadmanager.data.repository

import com.bmo.downloadmanager.core.common.result.AppError
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.database.dao.DownloadDao
import com.bmo.downloadmanager.core.database.dao.DownloadSegmentDao
import com.bmo.downloadmanager.data.mapper.toDomain
import com.bmo.downloadmanager.data.mapper.toEntity
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val segmentDao: DownloadSegmentDao
) : DownloadRepository {

    override fun getDownloads(): Flow<AppResult<List<Download>>> =
        downloadDao.observeAll()
            .map<_, AppResult<List<Download>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(AppError.Database(it))) }

    override suspend fun getDownloadById(id: Long): AppResult<Download> =
        try {
            val entity = downloadDao.getById(id)
            if (entity != null) AppResult.Success(entity.toDomain())
            else AppResult.Error(AppError.NotFound("download:$id"))
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun insertDownload(download: Download): AppResult<Long> =
        try {
            val id = downloadDao.insert(download.toEntity())
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun updateDownload(download: Download): AppResult<Unit> =
        try {
            downloadDao.update(download.toEntity())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun deleteDownload(id: Long): AppResult<Unit> =
        try {
            downloadDao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override fun getSegmentsForDownload(downloadId: Long): Flow<AppResult<List<DownloadSegment>>> =
        segmentDao.observeSegmentsForDownload(downloadId)
            .map<_, AppResult<List<DownloadSegment>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(AppError.Database(it))) }

    override suspend fun insertSegment(segment: DownloadSegment): AppResult<Long> =
        try {
            val id = segmentDao.insert(segment.toEntity())
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun updateSegment(segment: DownloadSegment): AppResult<Unit> =
        try {
            segmentDao.update(segment.toEntity())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }
}
