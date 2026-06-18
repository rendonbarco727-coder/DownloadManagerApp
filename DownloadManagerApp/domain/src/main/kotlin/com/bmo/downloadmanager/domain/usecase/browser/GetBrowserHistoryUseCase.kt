package com.bmo.downloadmanager.domain.usecase.browser

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetBrowserHistoryUseCase(
    private val repository: BrowserHistoryRepository
) {
    operator fun invoke(): Flow<AppResult<List<BrowserHistory>>> = repository.getHistory()
}
