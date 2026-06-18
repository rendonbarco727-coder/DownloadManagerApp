package com.bmo.downloadmanager.domain.usecase.settings

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.repository.SettingsRepository

class DeleteSettingUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(key: String): AppResult<Unit> =
        repository.deleteSetting(key)
}
