package com.bmo.downloadmanager.domain.usecase.browser

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository

class InsertBrowserHistoryUseCase(
    private val repository: BrowserHistoryRepository
) {
    suspend operator fun invoke(entry: BrowserHistory): AppResult<Unit> =
        repository.insertOrUpdateHistory(entry)
}
