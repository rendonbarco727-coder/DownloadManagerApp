package com.bmo.downloadmanager.domain.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserTab
import kotlinx.coroutines.flow.Flow

interface BrowserTabRepository {
    fun getTabs(): Flow<AppResult<List<BrowserTab>>>
    suspend fun getActiveTab(): AppResult<BrowserTab?>
    suspend fun insertTab(tab: BrowserTab): AppResult<Long>
    suspend fun updateTab(tab: BrowserTab): AppResult<Unit>
    suspend fun deleteTab(id: Long): AppResult<Unit>
    suspend fun setActiveTab(id: Long): AppResult<Unit>
}
