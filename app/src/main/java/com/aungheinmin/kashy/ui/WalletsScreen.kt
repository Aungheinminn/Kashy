package com.aungheinmin.kashy.ui

import androidx.compose.ui.res.stringResource
import com.aungheinmin.kashy.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.WalletSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onCreateWallet: () -> Unit,
    onOpenWallet: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val totals = state.wallets.groupBy { it.currency }.mapValues { (_, ws) -> ws.sumOf { it.balance } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wallets)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = onCreateWallet) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_wallet)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(R.string.total_by_currency), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Currency.entries.filter { it in totals }.forEach { c ->
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text(stringResource(c.label), Modifier.weight(1f))
                            AmountText(formatMoney(totals.getValue(c), c), formatShort(totals.getValue(c), c), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { SectionLabel(stringResource(R.string.your_wallets), Modifier.padding(horizontal = 16.dp)) }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
                ) {
                    state.wallets.forEachIndexed { i, w ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        WalletRow(w, onClick = { onOpenWallet(w.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletRow(wallet: WalletSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletIcon(wallet.emoji, wallet.currency)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(wallet.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(wallet.currency.label) + " · " + if (wallet.entryCount == 0) stringResource(R.string.wallet_new) else pluralStringResource(R.plurals.entry_count, wallet.entryCount, wallet.entryCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(formatMoney(wallet.balance, wallet.currency), formatShort(wallet.balance, wallet.currency), fontWeight = FontWeight.SemiBold)
    }
}
