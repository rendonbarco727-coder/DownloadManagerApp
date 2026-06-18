package com.bmo.downloadmanager.domain.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAllSettings(): Flow<AppResult<List<AppSettings>>>
    suspend fun getSetting(key: String): AppResult<AppSettings?>
    suspend fun setSetting(setting: AppSettings): AppResult<Unit>
    suspend fun deleteSetting(key: String): AppResult<Unit>
}
