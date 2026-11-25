package com.aungheinmin.kashy

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aungheinmin.kashy.data.AppLanguage
import com.aungheinmin.kashy.data.Settings
import com.aungheinmin.kashy.ui.CreateWalletScreen
import com.aungheinmin.kashy.ui.HomeScreen
import com.aungheinmin.kashy.ui.HomeViewModel
import com.aungheinmin.kashy.ui.LocalShortAmounts
import com.aungheinmin.kashy.ui.WalletsScreen
import com.aungheinmin.kashy.ui.theme.KashyTheme
import java.util.Locale

private enum class Screen { HOME, WALLETS, CREATE_WALLET }

class MainActivity : ComponentActivity() {
    private lateinit var startLanguage: AppLanguage

    override fun attachBaseContext(newBase: Context) {
        startLanguage = Settings.language(newBase)
        val tag = startLanguage.tag
        if (tag == null) {
            Locale.setDefault(newBase.resources.configuration.locales[0])
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration).apply { setLocale(locale) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as KashyApp).settings
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val shortAmounts by settings.shortAmounts.collectAsStateWithLifecycle()
            val language by settings.language.collectAsStateWithLifecycle()
            LaunchedEffect(language) { if (language != startLanguage) recreate() }
            KashyTheme(mode = themeMode) {
                CompositionLocalProvider(LocalShortAmounts provides shortAmounts) {
                    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
                    var stack by rememberSaveable { mutableStateOf(listOf(Screen.HOME)) }
                    val push = { s: Screen -> stack = stack + s }
                    val pop = { stack = stack.dropLast(1) }
                    BackHandler(enabled = stack.size > 1, onBack = pop)

                    when (stack.last()) {
                        Screen.HOME -> HomeScreen(
                            viewModel = viewModel,
                            themeMode = themeMode,
                            language = language,
                            onOpenWallets = { push(Screen.WALLETS) },
                            onCreateWallet = { push(Screen.CREATE_WALLET) },
                        )
                        Screen.WALLETS -> WalletsScreen(
                            viewModel = viewModel,
                            onBack = pop,
                            onCreateWallet = { push(Screen.CREATE_WALLET) },
                            onOpenWallet = {
                                viewModel.selectWallet(it)
                                stack = listOf(Screen.HOME)
                            },
                        )
                        Screen.CREATE_WALLET -> CreateWalletScreen(
                            onClose = pop,
                            onCreate = { name, emoji, currency ->
                                viewModel.createWallet(name, emoji, currency)
                                stack = listOf(Screen.HOME)
                            },
                        )
                    }
                }
            }
        }
    }
}
