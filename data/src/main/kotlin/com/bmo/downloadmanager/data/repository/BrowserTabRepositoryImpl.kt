package com.bmo.downloadmanager.data.repository

import com.bmo.downloadmanager.core.common.result.AppError
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.database.dao.BrowserTabDao
import com.bmo.downloadmanager.data.mapper.toDomain
import com.bmo.downloadmanager.data.mapper.toEntity
import com.bmo.downloadmanager.domain.model.BrowserTab
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BrowserTabRepositoryImpl @Inject constructor(
    private val dao: BrowserTabDao
) : BrowserTabRepository {

    override fun getTabs(): Flow<AppResult<List<BrowserTab>>> =
        dao.observeAll()
            .map<_, AppResult<List<BrowserTab>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(AppError.Database(it))) }

    override suspend fun getActiveTab(): AppResult<BrowserTab?> =
        try {
            val entity = dao.getActiveTab()
            AppResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun insertTab(tab: BrowserTab): AppResult<Long> =
        try {
            val id = dao.insert(tab.toEntity())
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun updateTab(tab: BrowserTab): AppResult<Unit> =
        try {
            dao.update(tab.toEntity())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun deleteTab(id: Long): AppResult<Unit> =
        try {
            dao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun setActiveTab(id: Long): AppResult<Unit> =
        try {
            dao.setActiveTab(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }
}
