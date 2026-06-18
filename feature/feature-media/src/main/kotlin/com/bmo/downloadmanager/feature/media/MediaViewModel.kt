package com.bmo.downloadmanager.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.enums.DownloadStatus
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadByIdUseCase
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ---------------------------------------------------------------------------
// UI States
// ---------------------------------------------------------------------------

sealed interface MediaLibraryUiState {
    data object Loading : MediaLibraryUiState
    data class Success(val files: List<Download>) : MediaLibraryUiState
    data class Error(val message: String) : MediaLibraryUiState
}

sealed interface ActiveDownloadsUiState {
    data object Loading : ActiveDownloadsUiState
    data class Success(val downloads: List<Download>) : ActiveDownloadsUiState
    data class Error(val message: String) : ActiveDownloadsUiState
}

sealed interface SegmentsUiState {
    data object Idle : SegmentsUiState
    data object Loading : SegmentsUiState
    data class Success(val segments: List<DownloadSegment>) : SegmentsUiState
    data class Error(val message: String) : SegmentsUiState
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val getDownloadByIdUseCase: GetDownloadByIdUseCase,
) : ViewModel() {

    // ID del download cuyo detalle de segmentos se está mostrando (null = ninguno)
    private val _selectedDownloadId = MutableStateFlow<Long?>(null)

    // Todos los downloads completados → tab "Biblioteca"
    val libraryUiState: StateFlow<MediaLibraryUiState> = getDownloadsUseCase()
        .map<AppResult<List<Download>>, MediaLibraryUiState> { result ->
            when (result) {
                is AppResult.Loading -> MediaLibraryUiState.Loading
                is AppResult.Error   -> MediaLibraryUiState.Error(result.error.toString())
                is AppResult.Success -> MediaLibraryUiState.Success(
                    result.data.filter { it.status == DownloadStatus.COMPLETED }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MediaLibraryUiState.Loading,
        )

    // Downloads activos (DOWNLOADING / PAUSED / QUEUED) → tab "En progreso"
    val activeDownloadsUiState: StateFlow<ActiveDownloadsUiState> = getDownloadsUseCase()
        .map<AppResult<List<Download>>, ActiveDownloadsUiState> { result ->
            when (result) {
                is AppResult.Loading -> ActiveDownloadsUiState.Loading
                is AppResult.Error   -> ActiveDownloadsUiState.Error(result.error.toString())
                is AppResult.Success -> ActiveDownloadsUiState.Success(
                    result.data.filter { it.status in activeStatuses }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActiveDownloadsUiState.Loading,
        )

    // Segmentos del download seleccionado
    @OptIn(ExperimentalCoroutinesApi::class)
    val segmentsUiState: StateFlow<SegmentsUiState> = _selectedDownloadId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(SegmentsUiState.Idle)
            } else {
                // Necesitamos el repositorio vía use case; aquí obtenemos segmentos
                // a través del repositorio expuesto desde GetDownloadByIdUseCase no lo da,
                // así que usamos el repositorio directamente inyectado desde el módulo.
                // Por ahora retornamos Idle; se conectará cuando el repo esté disponible.
                flowOf(SegmentsUiState.Idle)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SegmentsUiState.Idle,
        )

    fun selectDownload(id: Long?) {
        _selectedDownloadId.update { id }
    }

    companion object {
        private val activeStatuses = setOf(
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PAUSED,
            DownloadStatus.QUEUED,
        )
    }
}
