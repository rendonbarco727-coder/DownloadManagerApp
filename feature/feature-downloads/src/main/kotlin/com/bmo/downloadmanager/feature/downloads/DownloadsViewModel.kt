package com.bmo.downloadmanager.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.downloader.manager.DownloadManager
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.usecase.download.DeleteDownloadUseCase
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DownloadsUiState {
    data object Loading : DownloadsUiState
    data class Success(val downloads: List<Download>) : DownloadsUiState
    data class Error(val message: String) : DownloadsUiState
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = getDownloadsUseCase()
        .map<AppResult<List<Download>>, DownloadsUiState> { result ->
            when (result) {
                is AppResult.Success -> DownloadsUiState.Success(result.data)
                is AppResult.Error   -> DownloadsUiState.Error(result.error.toString())
                is AppResult.Loading -> DownloadsUiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DownloadsUiState.Loading,
        )

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            deleteDownloadUseCase(id)
        }
    }

    // pause/resume/cancel son fire-and-forget hacia el Service vía Intent;
    // no se espera un resultado aquí porque la UI ya observa el cambio de
    // status a través de getDownloadsUseCase() (Flow sobre Room), que se
    // actualiza cuando DownloadOrchestrator persiste el nuevo estado.

    fun pauseDownload(id: Long) {
        downloadManager.pause(id)
    }

    fun resumeDownload(id: Long) {
        downloadManager.resume(id)
    }

    fun cancelDownload(id: Long) {
        downloadManager.cancel(id)
    }
}
