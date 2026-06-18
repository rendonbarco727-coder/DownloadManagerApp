package com.bmo.downloadmanager.domain.usecase.download

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.repository.DownloadRepository

class UpdateDownloadUseCase(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(download: Download): AppResult<Unit> =
        repository.updateDownload(download)
}
