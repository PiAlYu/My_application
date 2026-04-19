package com.example.storechecklist.domain

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val checklistMode: UserChecklistMode = UserChecklistMode.HIDE_ON_TAP,
)
