package com.aungheinmin.kashy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aungheinmin.kashy.data.Category
import com.aungheinmin.kashy.data.EntryType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(initial: Set<String>, onApply: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    var pending by remember { mutableStateOf(initial) }
    val toggle = { key: String -> pending = if (key in pending) pending - key else pending + key }

    @Composable
    fun Chips(items: List<Pair<String, String>>) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { (key, label) ->
                FilterChip(
                    selected = key in pending,
                    onClick = { toggle(key) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text("Filter", style = MaterialTheme.typography.titleMedium)
            SectionLabel("Cash out categories")
            Chips(Category.forType(EntryType.OUT).map { it.name to "${it.emoji} ${it.label}" })
            SectionLabel("Cash in categories")
            Chips(Category.forType(EntryType.IN).map { it.name to "${it.emoji} ${it.label}" })
            SectionLabel("Other")
            Chips(listOf(NO_CATEGORY to "No category"))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { pending = emptySet() }) { Text("Clear all") }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Button(onClick = { onApply(pending) }) { Text("Show results") }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 14.dp, bottom = 6.dp),
    )
}
