package com.bmo.downloadmanager.domain.usecase.settings

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.AppSettings
import com.bmo.downloadmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetAllSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppResult<List<AppSettings>>> =
        repository.getAllSettings()
}
