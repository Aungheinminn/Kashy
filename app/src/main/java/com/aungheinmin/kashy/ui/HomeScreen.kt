package com.aungheinmin.kashy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aungheinmin.kashy.data.Entry
import com.aungheinmin.kashy.data.EntryType
import com.aungheinmin.kashy.data.Totals
import com.aungheinmin.kashy.ui.theme.LocalCashColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")

private sealed interface DialogState {
    data object New : DialogState
    data class Edit(val entry: Entry) : DialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<DialogState?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kashy") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = DialogState.New },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add entry") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            item { SummaryCard(state.totals) }
            if (state.entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No entries yet. Tap \"Add entry\" to record cash in or out.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(state.entries, key = { it.id }) { entry ->
                EntryRow(entry, onClick = { dialog = DialogState.Edit(entry) })
                HorizontalDivider()
            }
        }
    }

    when (val current = dialog) {
        null -> Unit
        DialogState.New -> EntryDialog(
            initial = null,
            onDismiss = { dialog = null },
            onSave = { type, amount, note ->
                viewModel.save(null, type, amount, note)
                dialog = null
            },
            onDelete = null,
        )
        is DialogState.Edit -> EntryDialog(
            initial = current.entry,
            onDismiss = { dialog = null },
            onSave = { type, amount, note ->
                viewModel.save(current.entry, type, amount, note)
                dialog = null
            },
            onDelete = {
                viewModel.delete(current.entry)
                dialog = null
            },
        )
    }
}

@Composable
private fun SummaryCard(totals: Totals) {
    val cash = LocalCashColors.current
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Balance", style = MaterialTheme.typography.labelLarge)
            Text(
                formatAmount(totals.balance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (totals.balance < 0) cash.cashOut else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TotalColumn("Cash in", totals.totalIn, cash.cashIn)
                Spacer(Modifier.width(16.dp))
                TotalColumn("Cash out", totals.totalOut, cash.cashOut)
            }
        }
    }
}

@Composable
private fun TotalColumn(label: String, amount: Long, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(formatAmount(amount), style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun EntryRow(entry: Entry, onClick: () -> Unit) {
    val cash = LocalCashColors.current
    val isIn = entry.type == EntryType.IN
    val color = if (isIn) cash.cashIn else cash.cashOut
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).format(dateFormatter)

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                if (isIn) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                contentDescription = if (isIn) "Cash in" else "Cash out",
                tint = color,
            )
        },
        headlineContent = { Text(entry.note.ifBlank { if (isIn) "Cash in" else "Cash out" }) },
        supportingContent = { Text(time) },
        trailingContent = {
            Text(
                (if (isIn) "+" else "−") + formatAmount(entry.amount),
                color = color,
                style = MaterialTheme.typography.titleMedium,
            )
        },
    )
}
