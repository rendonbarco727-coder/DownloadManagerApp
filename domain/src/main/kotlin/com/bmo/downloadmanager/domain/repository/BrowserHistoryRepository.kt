package com.bmo.downloadmanager.domain.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserHistory
import kotlinx.coroutines.flow.Flow

interface BrowserHistoryRepository {
    fun getHistory(): Flow<AppResult<List<BrowserHistory>>>
    suspend fun insertOrUpdateHistory(entry: BrowserHistory): AppResult<Unit>
    suspend fun deleteHistoryEntry(id: Long): AppResult<Unit>
    suspend fun clearAllHistory(): AppResult<Unit>
    fun searchHistory(query: String): Flow<AppResult<List<BrowserHistory>>>
}
