package com.phrolova.corex.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9), onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D), onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBCC7DB), onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3C4858), onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFD3E3FD), onTertiary = Color(0xFF001C38),
    surface = Color(0xFF1A1C1E), onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E), onSurfaceVariant = Color(0xFFC3C7CF),
    background = Color(0xFF1A1C1E), onBackground = Color(0xFFE2E2E6),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    outline = Color(0xFF8D9199)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2), onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB), onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF455A64), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFD8DC), onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF388E3C), onTertiary = Color.White,
    surface = Color(0xFFFAFAFA), onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFEEEEEE), onSurfaceVariant = Color(0xFF616161),
    background = Color(0xFFFAFAFA), onBackground = Color(0xFF212121),
    error = Color(0xFFD32F2F), onError = Color.White,
    outline = Color(0xFFBDBDBD)
)

@Composable
fun PhrolovaTheme(darkMode: String = "System", content: @Composable () -> Unit) {
    val dark = when (darkMode) { "Dark" -> true; "Light" -> false; else -> isSystemInDarkTheme() }
    val colors = if (dark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
