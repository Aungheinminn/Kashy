package com.aungheinmin.kashy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
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
    secondaryContainer = Color(0xFF2C2E31),
    onSecondaryContainer = Color(0xFFE4E4E6),
    background = Color(0xFF121314),
    onBackground = Color(0xFFE4E4E6),
    surface = Color(0xFF121314),
    onSurface = Color(0xFFE4E4E6),
    onSurfaceVariant = Color(0xFFA5A7AB),
    surfaceContainerLowest = Color(0xFF0D0E0F),
    surfaceContainerLow = Color(0xFF191A1C),
    surfaceContainer = Color(0xFF1E1F21),
    surfaceContainerHigh = Color(0xFF252629),
    surfaceContainerHighest = Color(0xFF2D2E31),
    outline = Color(0xFF8B8D91),
    outlineVariant = Color(0xFF2D2E31),
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
        val burmese = LocalConfiguration.current.locales[0].language == "my"
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = if (burmese) BurmeseTypography else Typography(),
            content = content,
        )
    }
}

private fun TextStyle.naturalLineHeight() = copy(lineHeight = TextUnit.Unspecified)

/** Material's fixed line heights are sized for Latin text and cut off the stacked marks in Burmese words. */
private val BurmeseTypography = Typography().run {
    copy(
        displayLarge = displayLarge.naturalLineHeight(),
        displayMedium = displayMedium.naturalLineHeight(),
        displaySmall = displaySmall.naturalLineHeight(),
        headlineLarge = headlineLarge.naturalLineHeight(),
        headlineMedium = headlineMedium.naturalLineHeight(),
        headlineSmall = headlineSmall.naturalLineHeight(),
        titleLarge = titleLarge.naturalLineHeight(),
        titleMedium = titleMedium.naturalLineHeight(),
        titleSmall = titleSmall.naturalLineHeight(),
        bodyLarge = bodyLarge.naturalLineHeight(),
        bodyMedium = bodyMedium.naturalLineHeight(),
        bodySmall = bodySmall.naturalLineHeight(),
        labelLarge = labelLarge.naturalLineHeight(),
        labelMedium = labelMedium.naturalLineHeight(),
        labelSmall = labelSmall.naturalLineHeight(),
    )
}
