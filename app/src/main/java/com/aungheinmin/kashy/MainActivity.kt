package com.aungheinmin.kashy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aungheinmin.kashy.ui.CreateWalletScreen
import com.aungheinmin.kashy.ui.HomeScreen
import com.aungheinmin.kashy.ui.HomeViewModel
import com.aungheinmin.kashy.ui.WalletsScreen
import com.aungheinmin.kashy.ui.theme.KashyTheme

private enum class Screen { HOME, WALLETS, CREATE_WALLET }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as KashyApp).settings
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            KashyTheme(mode = themeMode) {
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
                var stack by rememberSaveable { mutableStateOf(listOf(Screen.HOME)) }
                val push = { s: Screen -> stack = stack + s }
                val pop = { stack = stack.dropLast(1) }
                BackHandler(enabled = stack.size > 1, onBack = pop)

                when (stack.last()) {
                    Screen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        themeMode = themeMode,
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
