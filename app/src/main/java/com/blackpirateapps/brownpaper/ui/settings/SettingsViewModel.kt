package com.blackpirateapps.brownpaper.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isProcessing: Boolean = false,
)

sealed interface SettingsEvent {
    data class Success(val message: String) : SettingsEvent
    data class Error(val message: String) : SettingsEvent
    data class ExportData(val json: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ArticleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val json = repository.exportData()
                _events.emit(SettingsEvent.ExportData(json))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Error("Export failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun importData(jsonData: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                repository.importData(jsonData)
                _events.emit(SettingsEvent.Success("Data restored successfully"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Error("Import failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }
}
