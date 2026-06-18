package com.bmo.downloadmanager.domain.usecase.tab

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserTab
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository

class InsertBrowserTabUseCase(
    private val repository: BrowserTabRepository
) {
    suspend operator fun invoke(tab: BrowserTab): AppResult<Long> =
        repository.insertTab(tab)
}
