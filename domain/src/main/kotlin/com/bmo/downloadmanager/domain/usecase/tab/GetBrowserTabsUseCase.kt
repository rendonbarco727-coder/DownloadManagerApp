package com.bmo.downloadmanager.domain.usecase.tab

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserTab
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import kotlinx.coroutines.flow.Flow

class GetBrowserTabsUseCase(
    private val repository: BrowserTabRepository
) {
    operator fun invoke(): Flow<AppResult<List<BrowserTab>>> = repository.getTabs()
}
