package com.bmo.downloadmanager.data.mapper

import com.bmo.downloadmanager.core.database.entity.BrowserHistoryEntity
import com.bmo.downloadmanager.core.database.entity.BrowserTabEntity
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.model.BrowserTab

fun BrowserHistoryEntity.toDomain(): BrowserHistory = BrowserHistory(
    id = id,
    url = url,
    title = title,
    favicon = favicon,
    visitCount = visitCount,
    lastVisitedAt = lastVisitedAt
)

fun BrowserHistory.toEntity(): BrowserHistoryEntity = BrowserHistoryEntity(
    id = id,
    url = url,
    title = title,
    favicon = favicon,
    lastVisitedAt = lastVisitedAt,
    visitCount = visitCount
)

fun BrowserTabEntity.toDomain(): BrowserTab = BrowserTab(
    id = id,
    currentUrl = currentUrl,
    title = title,
    favicon = favicon,
    tabOrder = tabOrder,
    isActive = isActive,
    webViewState = webViewState,
    isSessionRestored = isSessionRestored,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt
)

fun BrowserTab.toEntity(): BrowserTabEntity = BrowserTabEntity(
    id = id,
    currentUrl = currentUrl,
    title = title,
    favicon = favicon,
    tabOrder = tabOrder,
    isActive = isActive,
    webViewState = webViewState,
    isSessionRestored = isSessionRestored,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt
)
