package com.aungheinmin.kashy.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage(val tag: String?) { SYSTEM(null), ENGLISH("en"), BURMESE("my"), JAPANESE("ja") }

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null)!!) }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _walletId = MutableStateFlow(prefs.getLong(KEY_WALLET, 1L))
    val walletId: StateFlow<Long> = _walletId.asStateFlow()

    private val _shortAmounts = MutableStateFlow(prefs.getBoolean(KEY_SHORT, true))
    val shortAmounts: StateFlow<Boolean> = _shortAmounts.asStateFlow()

    private val _language = MutableStateFlow(readLanguage(prefs))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).commit()
        _language.value = language
    }

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

    companion object {
        private const val PREFS = "settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_WALLET = "wallet_id"
        private const val KEY_SHORT = "short_amounts"

        private fun readLanguage(prefs: android.content.SharedPreferences) =
            runCatching { AppLanguage.valueOf(prefs.getString(KEY_LANGUAGE, null)!!) }.getOrDefault(AppLanguage.SYSTEM)

        /** Read before the Activity has its resources, to pick the locale it starts with. */
        fun language(context: Context): AppLanguage =
            readLanguage(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
    }
}
