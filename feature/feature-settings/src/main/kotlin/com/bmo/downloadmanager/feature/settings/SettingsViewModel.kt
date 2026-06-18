package com.bmo.downloadmanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.enums.SettingValueType
import com.bmo.downloadmanager.domain.model.AppSettings
import com.bmo.downloadmanager.domain.usecase.settings.DeleteSettingUseCase
import com.bmo.downloadmanager.domain.usecase.settings.GetAllSettingsUseCase
import com.bmo.downloadmanager.domain.usecase.settings.SetSettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: List<AppSettings> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAllSettingsUseCase: GetAllSettingsUseCase,
    private val setSettingUseCase: SetSettingUseCase,
    private val deleteSettingUseCase: DeleteSettingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getAllSettingsUseCase().collect { result ->
                when (result) {
                    is AppResult.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is AppResult.Success -> _uiState.update { it.copy(isLoading = false, settings = result.data) }
                    is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toString()) }
                }
            }
        }
    }

    fun setSetting(key: String, value: String, type: SettingValueType = SettingValueType.STRING) {
        viewModelScope.launch {
            setSettingUseCase(AppSettings(key = key, value = value, type = type))
        }
    }

    fun deleteSetting(key: String) {
        viewModelScope.launch {
            deleteSettingUseCase(key)
        }
    }
}
