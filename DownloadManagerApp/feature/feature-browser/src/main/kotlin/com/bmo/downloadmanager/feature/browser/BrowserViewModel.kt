package com.bmo.downloadmanager.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserTab
import com.bmo.downloadmanager.domain.usecase.tab.DeleteBrowserTabUseCase
import com.bmo.downloadmanager.domain.usecase.tab.GetBrowserTabsUseCase
import com.bmo.downloadmanager.domain.usecase.tab.InsertBrowserTabUseCase
import com.bmo.downloadmanager.domain.usecase.tab.UpdateBrowserTabUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserUiState(
    val tabs: List<BrowserTab> = emptyList(),
    val activeTab: BrowserTab? = null,
    val urlBarText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val getTabsUseCase: GetBrowserTabsUseCase,
    private val insertTabUseCase: InsertBrowserTabUseCase,
    private val updateTabUseCase: UpdateBrowserTabUseCase,
    private val deleteTabUseCase: DeleteBrowserTabUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        getTabsUseCase()
            .onEach { result ->
                when (result) {
                    is AppResult.Success -> {
                        val tabs = result.data
                        val active = tabs.firstOrNull { it.isActive } ?: tabs.firstOrNull()
                        _uiState.update {
                            it.copy(
                                tabs = tabs,
                                activeTab = active,
                                urlBarText = active?.currentUrl ?: "",
                                error = null,
                            )
                        }
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(error = result.error.toString())
                    }
                    is AppResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onUrlBarTextChange(text: String) {
        _uiState.update { it.copy(urlBarText = text) }
    }

    fun navigateTo(url: String) {
        val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else if (url.contains(".") && !url.contains(" ")) {
            "https://$url"
        } else {
            "https://www.google.com/search?q=${url.trim().replace(" ", "+")}"
        }

        val active = _uiState.value.activeTab
        if (active != null) {
            viewModelScope.launch {
                updateTabUseCase(active.copy(currentUrl = finalUrl, lastAccessedAt = System.currentTimeMillis()))
            }
        } else {
            viewModelScope.launch {
                insertTabUseCase(
                    BrowserTab(
                        currentUrl = finalUrl,
                        tabOrder = 0,
                        isActive = true,
                    )
                )
            }
        }
        _uiState.update { it.copy(urlBarText = finalUrl) }
    }

    fun newTab() {
        viewModelScope.launch {
            val order = _uiState.value.tabs.size
            insertTabUseCase(
                BrowserTab(
                    currentUrl = "https://www.google.com",
                    tabOrder = order,
                    isActive = true,
                )
            )
        }
    }

    fun closeTab(id: Long) {
        viewModelScope.launch {
            deleteTabUseCase(id)
        }
    }

    fun onPageLoadFinished(url: String, title: String?) {
        val active = _uiState.value.activeTab ?: return
        viewModelScope.launch {
            updateTabUseCase(
                active.copy(
                    currentUrl = url,
                    title = title,
                    lastAccessedAt = System.currentTimeMillis(),
                )
            )
        }
        _uiState.update { it.copy(urlBarText = url, isLoading = false) }
    }

    fun onPageStarted() {
        _uiState.update { it.copy(isLoading = true) }
    }
}
