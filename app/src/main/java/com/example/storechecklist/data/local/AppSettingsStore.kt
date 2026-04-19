package com.example.storechecklist.data.local

import android.content.Context
import com.example.storechecklist.domain.AppSettings
import com.example.storechecklist.domain.AppThemeMode
import com.example.storechecklist.domain.UserChecklistMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())

    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun saveThemeMode(themeMode: AppThemeMode) {
        preferences.edit()
            .putString(KEY_THEME_MODE, themeMode.name)
            .apply()
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    fun saveChecklistMode(mode: UserChecklistMode) {
        preferences.edit()
            .putString(KEY_CHECKLIST_MODE, mode.name)
            .apply()
        _settings.value = _settings.value.copy(checklistMode = mode)
    }

    private fun readSettings(): AppSettings {
        return AppSettings(
            themeMode = readThemeMode(),
            checklistMode = readChecklistMode(),
        )
    }

    private fun readThemeMode(): AppThemeMode {
        val rawValue = preferences.getString(KEY_THEME_MODE, null)
        return AppThemeMode.values().firstOrNull { it.name == rawValue } ?: AppThemeMode.SYSTEM
    }

    private fun readChecklistMode(): UserChecklistMode {
        val rawValue = preferences.getString(KEY_CHECKLIST_MODE, null)
        return UserChecklistMode.values().firstOrNull { it.name == rawValue }
            ?: UserChecklistMode.HIDE_ON_TAP
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CHECKLIST_MODE = "checklist_mode"
    }
}
