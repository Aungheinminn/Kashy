package com.aungheinmin.kashy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aungheinmin.kashy.data.Entry
import com.aungheinmin.kashy.data.EntryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDialog(
    initial: Entry?,
    onDismiss: () -> Unit,
    onSave: (type: EntryType, amount: Long, note: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var type by rememberSaveable { mutableStateOf(initial?.type ?: EntryType.IN) }
    var amountText by rememberSaveable { mutableStateOf(initial?.let { minorToInput(it.amount) } ?: "") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    val amount = parseAmount(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New entry" else "Edit entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    EntryType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index, EntryType.entries.size),
                        ) {
                            Text(if (option == EntryType.IN) "Cash in" else "Cash out")
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    isError = amountText.isNotEmpty() && amount == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let { onSave(type, it, note.trim()) } },
                enabled = amount != null,
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
