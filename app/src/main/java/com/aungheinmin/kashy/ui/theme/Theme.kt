package com.aungheinmin.kashy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class CashColors(val cashIn: Color, val cashOut: Color)

val LocalCashColors = staticCompositionLocalOf { CashColors(Color.Unspecified, Color.Unspecified) }

private val LightCash = CashColors(cashIn = Color(0xFF2E7D32), cashOut = Color(0xFFC62828))
private val DarkCash = CashColors(cashIn = Color(0xFF81C784), cashOut = Color(0xFFEF9A9A))

@Composable
fun KashyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    CompositionLocalProvider(LocalCashColors provides if (darkTheme) DarkCash else LightCash) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
