package com.blackpirateapps.brownpaper.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpirateapps.brownpaper.data.wallabag.WallabagLoginResult
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSyncResult
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import com.blackpirateapps.brownpaper.domain.repository.WallabagRepository
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
    val isLoggingIn: Boolean = false,
    val isSyncing: Boolean = false,
    val isWallabagConnected: Boolean = false,
    val wallabagHost: String = "https://app.wallabag.it",
    val wallabagUsername: String = "",
    val wallabagPassword: String = "",
    val wallabagClientId: String = "",
    val wallabagClientSecret: String = "",
    val showWallabagAdvanced: Boolean = false,
    val connectedHost: String = "",
    val connectedUsername: String = "",
    val lastSyncAtMillis: Long = 0,
)

sealed interface SettingsEvent {
    data class Success(val message: String) : SettingsEvent
    data class Error(val message: String) : SettingsEvent
    data class ExportData(val json: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val wallabagRepository: WallabagRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            wallabagRepository.accountState.collect { account ->
                _uiState.update { current ->
                    current.copy(
                        isWallabagConnected = account.isConnected,
                        wallabagHost = if (current.connectedHost.isBlank() || current.wallabagHost == current.connectedHost) {
                            account.host
                        } else {
                            current.wallabagHost
                        },
                        wallabagUsername = if (current.connectedUsername.isBlank() || current.wallabagUsername == current.connectedUsername) {
                            account.username
                        } else {
                            current.wallabagUsername
                        },
                        connectedHost = account.host,
                        connectedUsername = account.username,
                        lastSyncAtMillis = account.lastSyncAtMillis,
                    )
                }
            }
        }
    }

    fun updateWallabagHost(value: String) {
        _uiState.update { it.copy(wallabagHost = value) }
    }

    fun updateWallabagUsername(value: String) {
        _uiState.update { it.copy(wallabagUsername = value) }
    }

    fun updateWallabagPassword(value: String) {
        _uiState.update { it.copy(wallabagPassword = value) }
    }

    fun updateWallabagClientId(value: String) {
        _uiState.update { it.copy(wallabagClientId = value) }
    }

    fun updateWallabagClientSecret(value: String) {
        _uiState.update { it.copy(wallabagClientSecret = value) }
    }

    fun toggleWallabagAdvanced() {
        _uiState.update { it.copy(showWallabagAdvanced = !it.showWallabagAdvanced) }
    }

    fun loginWallabag() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true) }
            try {
                when (
                    val result = wallabagRepository.login(
                        host = current.wallabagHost,
                        username = current.wallabagUsername,
                        password = current.wallabagPassword,
                        clientId = current.wallabagClientId,
                        clientSecret = current.wallabagClientSecret,
                    )
                ) {
                    WallabagLoginResult.Success -> {
                        _uiState.update { it.copy(wallabagPassword = "") }
                        _events.emit(SettingsEvent.Success("Connected to wallabag"))
                        syncWallabag()
                    }
                    WallabagLoginResult.MissingClientCredentials -> {
                        _uiState.update { it.copy(showWallabagAdvanced = true) }
                        _events.emit(SettingsEvent.Error("Enter your wallabag client ID and client secret."))
                    }
                    is WallabagLoginResult.InvalidHost -> _events.emit(SettingsEvent.Error(result.message))
                    is WallabagLoginResult.Failure -> _events.emit(SettingsEvent.Error(result.message))
                }
            } finally {
                _uiState.update { it.copy(isLoggingIn = false) }
            }
        }
    }

    fun syncWallabag() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                when (val result = wallabagRepository.syncNow()) {
                    is WallabagSyncResult.Success -> {
                        _events.emit(
                            SettingsEvent.Success(
                                "wallabag sync complete: ${result.pulled} pulled, ${result.pushed} pushed",
                            ),
                        )
                    }
                    WallabagSyncResult.NotConnected -> _events.emit(SettingsEvent.Error("Connect wallabag first."))
                    is WallabagSyncResult.Failure -> _events.emit(SettingsEvent.Error(result.message))
                }
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun disconnectWallabag() {
        viewModelScope.launch {
            wallabagRepository.disconnect()
            _events.emit(SettingsEvent.Success("Disconnected from wallabag"))
        }
    }

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
