package com.example.storechecklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storechecklist.AppGraph
import com.example.storechecklist.domain.AppThemeMode
import com.example.storechecklist.domain.UserChecklistMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RoleSelectionUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val checklistMode: UserChecklistMode = UserChecklistMode.HIDE_ON_TAP,
)

class RoleSelectionViewModel : ViewModel() {
    private val settingsStore = AppGraph.appSettingsStore

    val uiState: StateFlow<RoleSelectionUiState> = settingsStore.settings
        .map { settings ->
            RoleSelectionUiState(
                themeMode = settings.themeMode,
                checklistMode = settings.checklistMode,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RoleSelectionUiState(),
        )

    fun setThemeMode(themeMode: AppThemeMode) {
        settingsStore.saveThemeMode(themeMode)
    }

    fun setChecklistMode(mode: UserChecklistMode) {
        settingsStore.saveChecklistMode(mode)
    }
}
