package com.aungheinmin.kashy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aungheinmin.kashy.data.ThemeMode

@Immutable
data class CashColors(
    val cashIn: Color,
    val cashOut: Color,
    val cashInBg: Color,
    val cashOutBg: Color,
    val isDark: Boolean,
)

val LocalCashColors = staticCompositionLocalOf {
    CashColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, false)
}

private val LightCash = CashColors(
    cashIn = Color(0xFF2E7D32),
    cashOut = Color(0xFFC62828),
    cashInBg = Color(0xFFDDEFD9),
    cashOutBg = Color(0xFFF9DEDC),
    isDark = false,
)
private val DarkCash = CashColors(
    cashIn = Color(0xFF81C784),
    cashOut = Color(0xFFEF9A9A),
    cashInBg = Color(0xFF1F3A20),
    cashOutBg = Color(0xFF46201F),
    isDark = true,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF386A20),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFD6E8C6),
    onSecondaryContainer = Color(0xFF1A1C19),
    background = Color(0xFFFBFDF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFBFDF7),
    onSurface = Color(0xFF1A1C19),
    onSurfaceVariant = Color(0xFF5E6A5C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F7EE),
    surfaceContainer = Color(0xFFEDF3E7),
    surfaceContainerHigh = Color(0xFFE7EEE1),
    surfaceContainerHighest = Color(0xFFE1E8DC),
    outline = Color(0xFF75796F),
    outlineVariant = Color(0xFFE1E8DC),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9CD67D),
    onPrimary = Color(0xFF10380A),
    secondaryContainer = Color(0xFF2E4A1F),
    onSecondaryContainer = Color(0xFFE2E3DC),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE2E3DC),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE2E3DC),
    onSurfaceVariant = Color(0xFFA0AA9C),
    surfaceContainerLowest = Color(0xFF0C0F0B),
    surfaceContainerLow = Color(0xFF171C15),
    surfaceContainer = Color(0xFF1D241A),
    surfaceContainerHigh = Color(0xFF232B20),
    surfaceContainerHighest = Color(0xFF2A3027),
    outline = Color(0xFF8D9387),
    outlineVariant = Color(0xFF2A3027),
)

@Composable
fun KashyTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalCashColors provides if (dark) DarkCash else LightCash) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}
