package com.aungheinmin.kashy.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null)!!) }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _walletId = MutableStateFlow(prefs.getLong(KEY_WALLET, 1L))
    val walletId: StateFlow<Long> = _walletId.asStateFlow()

    private val _shortAmounts = MutableStateFlow(prefs.getBoolean(KEY_SHORT, true))
    val shortAmounts: StateFlow<Boolean> = _shortAmounts.asStateFlow()

    fun setShortAmounts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHORT, enabled).apply()
        _shortAmounts.value = enabled
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setWalletId(id: Long) {
        prefs.edit().putLong(KEY_WALLET, id).apply()
        _walletId.value = id
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_WALLET = "wallet_id"
        const val KEY_SHORT = "short_amounts"
    }
}
