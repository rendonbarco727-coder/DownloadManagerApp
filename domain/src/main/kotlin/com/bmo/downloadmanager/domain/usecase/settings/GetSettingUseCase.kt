package com.bmo.downloadmanager.domain.usecase.settings

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.AppSettings
import com.bmo.downloadmanager.domain.repository.SettingsRepository

class GetSettingUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(key: String): AppResult<AppSettings?> =
        repository.getSetting(key)
}
