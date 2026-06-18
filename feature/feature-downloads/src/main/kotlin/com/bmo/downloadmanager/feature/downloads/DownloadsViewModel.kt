package com.bmo.downloadmanager.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.downloadmanager.core.common.result.AppResult
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
}
