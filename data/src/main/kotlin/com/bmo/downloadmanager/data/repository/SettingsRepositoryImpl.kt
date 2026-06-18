package com.bmo.downloadmanager.data.repository

import com.bmo.downloadmanager.core.common.result.AppError
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.database.dao.SettingsDao
import com.bmo.downloadmanager.data.mapper.toDomain
import com.bmo.downloadmanager.data.mapper.toEntity
import com.bmo.downloadmanager.domain.model.AppSettings
import com.bmo.downloadmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao
) : SettingsRepository {

    override fun getAllSettings(): Flow<AppResult<List<AppSettings>>> =
        dao.observeAll()
            .map<_, AppResult<List<AppSettings>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(AppError.Database(it))) }

    override suspend fun getSetting(key: String): AppResult<AppSettings?> =
        try {
            val entity = dao.getByKey(key)
            AppResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun setSetting(setting: AppSettings): AppResult<Unit> =
        try {
            dao.upsert(setting.toEntity())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }

    override suspend fun deleteSetting(key: String): AppResult<Unit> =
        try {
            dao.deleteByKey(key)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database(e))
        }
}
