package com.example.storechecklist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.storechecklist.domain.AppThemeMode

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2E4F67),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFBFD2E0),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF1A3146),
    secondary = androidx.compose.ui.graphics.Color(0xFF4C655C),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFC7D7CF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF1E312A),
    tertiary = androidx.compose.ui.graphics.Color(0xFF74593E),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE0D1B7),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF372613),
    background = androidx.compose.ui.graphics.Color(0xFFEFE6D8),
    onBackground = androidx.compose.ui.graphics.Color(0xFF26211A),
    surface = androidx.compose.ui.graphics.Color(0xFFEDE2D1),
    onSurface = androidx.compose.ui.graphics.Color(0xFF26211A),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFD6C8B8),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5A5043),
    outline = androidx.compose.ui.graphics.Color(0xFF847A6C),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFC0B2A2),
    surfaceTint = androidx.compose.ui.graphics.Color(0xFF2E4F67),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFF3A332A),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFFF7EEE1),
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
