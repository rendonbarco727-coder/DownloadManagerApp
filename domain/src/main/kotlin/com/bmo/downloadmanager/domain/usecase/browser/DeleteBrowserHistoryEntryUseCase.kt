package com.bmo.downloadmanager.domain.usecase.browser

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository

class DeleteBrowserHistoryEntryUseCase(
    private val repository: BrowserHistoryRepository
) {
    suspend operator fun invoke(id: Long): AppResult<Unit> =
        repository.deleteHistoryEntry(id)
}
