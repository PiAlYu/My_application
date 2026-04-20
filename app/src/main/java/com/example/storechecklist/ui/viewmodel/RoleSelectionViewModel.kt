package com.example.storechecklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storechecklist.AppGraph
import com.example.storechecklist.domain.AppThemeMode
import com.example.storechecklist.domain.ServerConnectionSettings
import com.example.storechecklist.domain.UserChecklistMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RoleSelectionUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val checklistMode: UserChecklistMode = UserChecklistMode.HIDE_ON_TAP,
    val serverBaseUrl: String = "",
    val serverReadToken: String = "",
    val serverWriteToken: String = "",
    val connectionMessage: String? = null,
)

class RoleSelectionViewModel : ViewModel() {
    private val settingsStore = AppGraph.appSettingsStore
    private val repository = AppGraph.repository
    private val serverConnectionSettings = MutableStateFlow(repository.getServerConnectionSettings())
    private val connectionMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RoleSelectionUiState> = combine(
        settingsStore.settings,
        serverConnectionSettings,
        connectionMessage,
    ) { settings, connectionSettings, message ->
            RoleSelectionUiState(
                themeMode = settings.themeMode,
                checklistMode = settings.checklistMode,
                serverBaseUrl = connectionSettings.baseUrl,
                serverReadToken = connectionSettings.readToken,
                serverWriteToken = connectionSettings.writeToken,
                connectionMessage = message,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.getServerConnectionSettings().toInitialUiState(),
        )

    fun setThemeMode(themeMode: AppThemeMode) {
        settingsStore.saveThemeMode(themeMode)
    }

    fun setChecklistMode(mode: UserChecklistMode) {
        settingsStore.saveChecklistMode(mode)
    }

    fun saveServerConnection(
        rawUrl: String,
        rawReadToken: String,
        rawWriteToken: String,
    ) {
        val result = repository.saveServerConnectionSettings(
            rawUrl = rawUrl,
            rawReadToken = rawReadToken,
            rawWriteToken = rawWriteToken,
        )
        if (result.isSuccess) {
            val savedSettings = result.getOrNull() ?: return
            serverConnectionSettings.value = savedSettings
            connectionMessage.value = "Подключение сохранено."
        } else {
            val reason = result.exceptionOrNull()?.message ?: "Неверный формат URL."
            connectionMessage.value = "Подключение не сохранено. $reason"
        }
    }

    private fun ServerConnectionSettings.toInitialUiState(): RoleSelectionUiState {
        return RoleSelectionUiState(
            serverBaseUrl = baseUrl,
            serverReadToken = readToken,
            serverWriteToken = writeToken,
        )
    }
}
