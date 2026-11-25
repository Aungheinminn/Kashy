package com.aungheinmin.kashy.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aungheinmin.kashy.data.Category
import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.Entry
import com.aungheinmin.kashy.data.EntryType
import com.aungheinmin.kashy.ui.theme.LocalCashColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    initial: Entry?,
    currency: Currency,
    onDismiss: () -> Unit,
    onSave: (type: EntryType, amount: Long, note: String, category: String?, timestamp: Long) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var type by rememberSaveable { mutableStateOf(initial?.type ?: EntryType.OUT) }
    var amountText by rememberSaveable { mutableStateOf(initial?.let { minorToInput(it.amount, currency) } ?: "") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category) }
    var timestamp by rememberSaveable { mutableLongStateOf(initial?.timestamp ?: System.currentTimeMillis()) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val amount = parseAmount(amountText, currency)
    val zone = ZoneId.systemDefault()
    val cash = LocalCashColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val amountFocus = remember { FocusRequester() }
    var amountFocused by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Text(if (initial == null) "New entry" else "Edit entry", style = MaterialTheme.typography.titleMedium)
            SegmentedChoice(
                options = listOf(EntryType.IN, EntryType.OUT),
                selected = type,
                label = { if (it == EntryType.IN) "Cash in" else "Cash out" },
                onSelect = {
                    if (it != type) category = null
                    type = it
                    if (amountFocused) keyboard?.show()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                selectedColors = {
                    if (it == EntryType.IN) cash.cashInBg to cash.cashIn else cash.cashOutBg to cash.cashOut
                },
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                suffix = { Text(currency.name) },
                singleLine = true,
                isError = amountText.isNotEmpty() && amount == null,
                supportingText = if (amountText.isNotEmpty() && amount == null && currency.decimals == 0) {
                    { Text("Whole numbers only for ${currency.label}") }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (currency.decimals == 0) KeyboardType.Number else KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .focusRequester(amountFocus)
                    .onFocusChanged { amountFocused = it.isFocused },
            )
            SectionLabel("Category (optional)")
            CategoryGrid(type = type, selected = category, onSelect = { key -> category = if (category == key) null else key })
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = Instant.ofEpochMilli(timestamp).atZone(zone).format(dateFormat),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Date") },
                trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { pickingDate = true },
            )
            Button(
                onClick = { amount?.let { onSave(type, it, note.trim(), category, timestamp) } },
                enabled = amount != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Save") }
            if (onDelete != null) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (pickingDate) {
        val current = Instant.ofEpochMilli(timestamp).atZone(zone)
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // DatePicker reports UTC midnight; keep the entry's local time of day.
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        timestamp = date.atTime(current.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun CategoryGrid(type: EntryType, selected: String?, onSelect: (String?) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val tiles: List<Category?> = listOf(null) + Category.forType(type)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { cat ->
                    val isSelected = selected == cat?.name
                    Column(
                        Modifier
                            .weight(1f)
                            .focusProperties { canFocus = false }
                            .clickable { onSelect(cat?.name) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EntryIcon(
                            type,
                            cat,
                            size = 42.dp,
                            modifier = if (isSelected) Modifier.border(2.dp, primary, CircleShape).padding(3.dp) else Modifier.padding(3.dp),
                        )
                        Text(
                            cat?.label ?: "None",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
