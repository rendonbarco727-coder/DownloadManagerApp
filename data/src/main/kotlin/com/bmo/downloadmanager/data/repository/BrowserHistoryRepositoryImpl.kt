package com.bmo.downloadmanager.data.repository

import com.bmo.downloadmanager.core.common.result.AppError
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.database.dao.BrowserHistoryDao
import com.bmo.downloadmanager.data.mapper.toDomain
import com.bmo.downloadmanager.data.mapper.toEntity
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BrowserHistoryRepositoryImpl @Inject constructor(
    private val dao: BrowserHistoryDao
) : BrowserHistoryRepository {

    override fun getHistory(): Flow<AppResult<List<BrowserHistory>>> =
        dao.observeRecent()
            .map<_, AppResult<List<BrowserHistory>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(AppError.Database(it))) }

    override suspend fun insertOrUpdateHistory(entry: BrowserHistory): AppResult<Unit> =
        try {
            dao.recordVisit(
                url = entry.url,
                title = entry.title,
                favicon = entry.favicon,
                visitedAt = entry.lastVisitedAt
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun deleteHistoryEntry(id: Long): AppResult<Unit> =
        try {
            dao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun clearAllHistory(): AppResult<Unit> =
        try {
            dao.clearAll()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override fun searchHistory(query: String): Flow<AppResult<List<BrowserHistory>>> =
        dao.search(query)
            .map<_, AppResult<List<BrowserHistory>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(AppError.Database(it))) }
}
