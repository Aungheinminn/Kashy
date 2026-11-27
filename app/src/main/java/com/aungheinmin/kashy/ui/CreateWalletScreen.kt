package com.aungheinmin.kashy.ui

import androidx.compose.ui.res.stringResource
import com.aungheinmin.kashy.R
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aungheinmin.kashy.data.Currency

private val walletEmoji = listOf("💵", "📱", "🏦", "✈️", "🐷", "🛒")

private val currencyExample = mapOf(
    Currency.MMK to 125_000_000L,
    Currency.USD to 125_050L,
    Currency.JPY to 125_000L,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateWalletScreen(onClose: () -> Unit, onCreate: (name: String, emoji: String, currency: Currency) -> Unit) {
    val nameState = rememberTextFieldState()
    val name = nameState.text.toString()
    var emoji by rememberSaveable { mutableStateOf(walletEmoji.first()) }
    var currency by rememberSaveable { mutableStateOf(Currency.MMK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_wallet)) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                state = nameState,
                label = { Text(stringResource(R.string.wallet_name)) },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionLabel(stringResource(R.string.icon_optional))
            WalletEmojiPicker(selected = emoji, onSelect = { emoji = it })
            SectionLabel(stringResource(R.string.currency))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Currency.entries.forEach { c ->
                    val selected = currency == c
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { currency = c }
                            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WalletIcon("", c)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(c.label), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.currency_example, c.name, formatMoney(currencyExample.getValue(c), c)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RadioButton(selected = selected, onClick = { currency = c })
                    }
                }
            }
            Text(
                stringResource(R.string.wallet_starts_at, formatMoney(0, currency)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(
                onClick = { onCreate(name.trim(), emoji, currency) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text(stringResource(R.string.create_wallet)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletEmojiPicker(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (walletEmoji + "").forEach { e ->
            val isSelected = selected == e
            Box(
                Modifier
                    .size(44.dp)
                    .border(
                        if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        RoundedCornerShape(12.dp),
                    )
                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) {
                if (e.isEmpty()) Text(stringResource(R.string.none), style = MaterialTheme.typography.labelSmall) else Text(e, fontSize = 20.sp)
            }
        }
    }
}
