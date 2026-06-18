package com.bmo.downloadmanager.domain.usecase.download

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class GetDownloadsUseCase(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<AppResult<List<Download>>> = repository.getDownloads()
}
