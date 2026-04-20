package com.example.storechecklist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.storechecklist.domain.AppThemeMode

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2F5D8C),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD9E7F5),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF10253A),
    secondary = androidx.compose.ui.graphics.Color(0xFF537568),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFDCEBE4),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF142720),
    tertiary = androidx.compose.ui.graphics.Color(0xFF946B36),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF3E4CF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF32210D),
    background = androidx.compose.ui.graphics.Color(0xFFF6F1E8),
    onBackground = androidx.compose.ui.graphics.Color(0xFF23201B),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBF3),
    onSurface = androidx.compose.ui.graphics.Color(0xFF23201B),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8DED0),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5F574C),
    outline = androidx.compose.ui.graphics.Color(0xFF8C8174),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFD7CBBE),
    surfaceTint = androidx.compose.ui.graphics.Color(0xFF2F5D8C),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFF383128),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFFF7EEE2),
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
