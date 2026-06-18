package com.bmo.downloadmanager.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.usecase.browser.DeleteBrowserHistoryEntryUseCase
import com.bmo.downloadmanager.domain.usecase.browser.GetBrowserHistoryUseCase
import com.bmo.downloadmanager.domain.usecase.browser.ClearAllBrowserHistoryUseCase
import com.bmo.downloadmanager.domain.usecase.browser.SearchBrowserHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val entries: List<BrowserHistory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetBrowserHistoryUseCase,
    private val deleteEntryUseCase: DeleteBrowserHistoryEntryUseCase,
    private val clearAllUseCase: ClearAllBrowserHistoryUseCase,
    private val searchHistoryUseCase: SearchBrowserHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            getHistoryUseCase().collect { result ->
                when (result) {
                    is AppResult.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is AppResult.Success -> _uiState.update { it.copy(isLoading = false, entries = result.data) }
                    is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toString()) }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                loadHistory()
            } else {
                searchHistoryUseCase(query).collect { result ->
                    when (result) {
                        is AppResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is AppResult.Success -> _uiState.update { it.copy(isLoading = false, entries = result.data) }
                        is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toString()) }
                    }
                }
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            deleteEntryUseCase(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            clearAllUseCase()
        }
    }
}
