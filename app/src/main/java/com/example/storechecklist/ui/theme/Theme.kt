package com.example.storechecklist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.storechecklist.domain.AppThemeMode

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1D4ED8),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFDDE9FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0B1F4D),
    secondary = androidx.compose.ui.graphics.Color(0xFF0F766E),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD7F4F0),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF00201D),
    tertiary = androidx.compose.ui.graphics.Color(0xFFB45309),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE0C2),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF3B1800),
    background = androidx.compose.ui.graphics.Color(0xFFF4F7FB),
    onBackground = androidx.compose.ui.graphics.Color(0xFF111827),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF111827),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF475569),
    outline = androidx.compose.ui.graphics.Color(0xFF6B7280),
)
private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF9DBBFF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF082A69),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF143D94),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFDCE7FF),
    secondary = androidx.compose.ui.graphics.Color(0xFF6ED7CB),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF003732),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF0F4D47),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD5F7F2),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFFC36E),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF4B2800),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF6A3C00),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE0C2),
    background = androidx.compose.ui.graphics.Color(0xFF09101C),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6EDF7),
    surface = androidx.compose.ui.graphics.Color(0xFF121B2A),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6EDF7),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF253346),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFBEC8D7),
    outline = androidx.compose.ui.graphics.Color(0xFF8E9AAF),
)

@Composable
fun StoreChecklistTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColors else LightColors
        AppThemeMode.LIGHT -> LightColors
        AppThemeMode.DARK -> DarkColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
