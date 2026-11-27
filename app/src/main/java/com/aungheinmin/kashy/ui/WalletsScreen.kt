package com.aungheinmin.kashy.ui

import android.text.InputType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aungheinmin.kashy.R
import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.WalletSummary
import com.aungheinmin.kashy.data.combinedTotal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onCreateWallet: () -> Unit,
    onOpenRates: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    val totalCurrency by viewModel.totalCurrency.collectAsStateWithLifecycle()
    val res = LocalContext.current.resources
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val totals = state.wallets.groupBy { it.currency }.mapValues { (_, ws) -> ws.sumOf { it.balance } }
    val combined = combinedTotal(state.wallets.map { it.currency to it.balance }, rates, totalCurrency)

    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onOpenRates) { Text(stringResource(R.string.rates)) }
                    }
                    AmountText(
                        full = approx(combined.approximate) + formatMoney(combined.minor, combined.currency),
                        short = approx(combined.approximate) + formatShort(combined.minor, combined.currency, SHORT_FROM_MILLION),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Currency.entries.filter { it in totals }.forEach { c ->
                        val minor = totals.getValue(c)
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text(stringResource(c.label), Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                AmountText(formatMoney(minor, c), formatShort(minor, c), fontWeight = FontWeight.SemiBold)
                                if (c != totalCurrency) {
                                    rates.convert(minor, c, totalCurrency)?.let { converted ->
                                        Text(
                                            "≈ " + formatMoney(converted, totalCurrency),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (combined.missing.isNotEmpty()) {
                        Text(
                            stringResource(R.string.not_included_rate, combined.missing.joinToString(", ") { it.name }),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
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
                        WalletRow(w, onClick = { editingId = w.id })
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.tap_wallet_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    state.wallets.firstOrNull { it.id == editingId }?.let { wallet ->
        EditWalletSheet(
            wallet = wallet,
            canDelete = state.wallets.size > 1,
            onDismiss = { editingId = null },
            onSave = { name, emoji ->
                viewModel.updateWallet(wallet.id, name, emoji)
                editingId = null
                notify(res.getString(R.string.wallet_updated))
            },
            onDelete = { deletingId = wallet.id },
        )
    }

    state.wallets.firstOrNull { it.id == deletingId }?.let { wallet ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(stringResource(R.string.delete_wallet_title, wallet.name)) },
            text = {
                Text(
                    if (wallet.entryCount > 0) pluralStringResource(R.plurals.delete_wallet_entries, wallet.entryCount, wallet.entryCount)
                    else stringResource(R.string.cant_undo)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWallet(wallet.id)
                    deletingId = null
                    editingId = null
                    notify(res.getString(R.string.wallet_deleted))
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun approx(approximate: Boolean) = if (approximate) "≈ " else ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditWalletSheet(
    wallet: WalletSummary,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit,
    onDelete: () -> Unit,
) {
    var nameText by rememberSaveable { mutableStateOf(wallet.name) }
    var emoji by rememberSaveable { mutableStateOf(wallet.emoji) }
    val name = nameText.trim()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.edit_wallet), style = MaterialTheme.typography.titleMedium)
            NativeTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = stringResource(R.string.wallet_name),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionLabel(stringResource(R.string.icon_optional))
            WalletEmojiPicker(selected = emoji, onSelect = { emoji = it })
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WalletIcon("", wallet.currency, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(wallet.currency.label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.currency_locked),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { onSave(name, emoji) },
                enabled = name.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) { Text(stringResource(R.string.save)) }
            OutlinedButton(onClick = onDelete, enabled = canDelete, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.delete_wallet),
                    color = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
