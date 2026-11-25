package com.aungheinmin.kashy.ui

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
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(walletEmoji.first()) }
    var currency by rememberSaveable { mutableStateOf(Currency.MMK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New wallet") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Wallet name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionLabel("Icon (optional)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (walletEmoji + "").forEach { e ->
                    val selected = emoji == e
                    Box(
                        Modifier
                            .size(44.dp)
                            .border(
                                if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                RoundedCornerShape(12.dp),
                            )
                            .background(if (selected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (e.isEmpty()) Text("None", style = MaterialTheme.typography.labelSmall) else Text(e, fontSize = 20.sp)
                    }
                }
            }
            SectionLabel("Currency")
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
                            Text(c.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${c.name} · e.g. ${formatMoney(currencyExample.getValue(c), c)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RadioButton(selected = selected, onClick = { currency = c })
                    }
                }
            }
            Text(
                "Starts at ${formatMoney(0, currency)}. Its balance comes from the entries you add.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(
                onClick = { onCreate(name.trim(), emoji, currency) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Create wallet") }
        }
    }
}
