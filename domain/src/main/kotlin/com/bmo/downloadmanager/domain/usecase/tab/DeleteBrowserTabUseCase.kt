package com.bmo.downloadmanager.domain.usecase.tab

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository

class DeleteBrowserTabUseCase(
    private val repository: BrowserTabRepository
) {
    suspend operator fun invoke(id: Long): AppResult<Unit> =
        repository.deleteTab(id)
}
