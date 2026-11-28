package com.aungheinmin.kashy.ui

import androidx.compose.ui.res.stringResource
import com.aungheinmin.kashy.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aungheinmin.kashy.data.Category
import com.aungheinmin.kashy.data.CombinedTotal
import com.aungheinmin.kashy.data.combinedTotal
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.Entry
import com.aungheinmin.kashy.data.EntryType
import com.aungheinmin.kashy.ui.theme.LocalCashColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle


private sealed interface Overlay {
    data class EditEntry(val entry: Entry) : Overlay
    data object MonthPicker : Overlay
    data object Filter : Overlay
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenWallets: () -> Unit,
    onCreateWallet: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    val totalCurrency by viewModel.totalCurrency.collectAsStateWithLifecycle()
    val res = LocalContext.current.resources
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var addingEntry by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val selecting = selectedIds.isNotEmpty()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(message: String, undo: (() -> Unit)? = null) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = undo?.let { res.getString(R.string.undo) }, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }

    // Entries that leave the list (another month, wallet or filter) can't stay selected out of sight.
    LaunchedEffect(state.entries) {
        val visible = state.entries.mapTo(HashSet()) { it.id }
        if (!visible.containsAll(selectedIds)) selectedIds = selectedIds intersect visible
    }
    BackHandler(enabled = selecting) { selectedIds = emptySet() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (selecting) {
                SelectionTopBar(
                    count = selectedIds.size,
                    allSelected = selectedIds.size == state.entries.size,
                    onClose = { selectedIds = emptySet() },
                    onSelectAll = {
                        selectedIds = if (selectedIds.size == state.entries.size) emptySet() else state.entries.mapTo(HashSet()) { it.id }
                    },
                    onDelete = {
                        val doomed = state.entries.filter { it.id in selectedIds }
                        selectedIds = emptySet()
                        viewModel.deleteEntries(doomed)
                        notify(
                            res.getQuantityString(R.plurals.entries_deleted, doomed.size, doomed.size),
                            undo = { viewModel.restoreEntries(doomed) },
                        )
                    },
                )
            } else {
                HomeTopBar(
                    state = state,
                    onSelectWallet = viewModel::selectWallet,
                    onOpenWallets = onOpenWallets,
                    onCreateWallet = onCreateWallet,
                    onOpenSettings = onOpenSettings,
                )
            }
        },
        floatingActionButton = {
            if (!selecting) {
                FloatingActionButton(
                    onClick = { addingEntry = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_entry)) }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + FAB_SPACE,
            ),
        ) {
            item {
                BalanceHeader(
                    state = state,
                    combined = if (state.wallets.size > 1) combinedTotal(state.wallets.map { it.currency to it.balance }, rates, totalCurrency) else null,
                    onOpenWallets = onOpenWallets,
                )
            }
            item {
                Toolbar(
                    month = state.month,
                    filterCount = state.categoryFilter.size,
                    onPrev = { viewModel.setMonth(state.month.minusMonths(1)) },
                    onNext = { viewModel.setMonth(state.month.plusMonths(1)) },
                    onOpenMonths = { overlay = Overlay.MonthPicker },
                    onOpenFilter = { overlay = Overlay.Filter },
                )
            }
            item {
                val labels = mapOf(
                    TypeFilter.ALL to stringResource(R.string.all),
                    TypeFilter.IN to stringResource(R.string.cash_in),
                    TypeFilter.OUT to stringResource(R.string.cash_out),
                )
                SegmentedChoice(
                    options = TypeFilter.entries,
                    selected = state.typeFilter,
                    label = { labels.getValue(it) },
                    onSelect = viewModel::setTypeFilter,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    selectedColors = {
                        val cash = LocalCashColors.current
                        when (it) {
                            TypeFilter.ALL -> MaterialTheme.colorScheme.surfaceContainerLowest to MaterialTheme.colorScheme.onSurface
                            TypeFilter.IN -> cash.cashInBg to cash.cashIn
                            TypeFilter.OUT -> cash.cashOutBg to cash.cashOut
                        }
                    },
                )
            }
            if (state.categoryFilter.isNotEmpty()) {
                item {
                    ActiveFilterChips(state.categoryFilter) { key ->
                        viewModel.setCategoryFilter(state.categoryFilter - key)
                    }
                }
            }
            if (state.entries.isEmpty()) {
                item {
                    Text(
                        stringResource(if (state.isFiltered) R.string.empty_filtered else R.string.empty_month),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            val zone = ZoneId.systemDefault()
            val groups = state.entries.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            items(groups.entries.toList(), key = { it.key.toEpochDay() }) { (day, entries) ->
                DayGroup(
                    day = day,
                    entries = entries,
                    currency = state.currency,
                    selecting = selecting,
                    selectedIds = selectedIds,
                    onClick = { entry ->
                        if (!selecting) {
                            overlay = Overlay.EditEntry(entry)
                        } else if (entry.id in selectedIds) {
                            selectedIds = selectedIds - entry.id
                        } else {
                            selectedIds = selectedIds + entry.id
                        }
                    },
                    onLongClick = { entry ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedIds = selectedIds + entry.id
                    },
                )
            }
        }
    }

    when (val current = overlay) {
        null -> Unit
        Overlay.MonthPicker -> MonthPickerDialog(
            selected = state.month,
            latest = YearMonth.now(),
            onSelect = {
                viewModel.setMonth(it)
                overlay = null
            },
            onDismiss = { overlay = null },
        )
        Overlay.Filter -> FilterSheet(
            initial = state.categoryFilter,
            onApply = {
                viewModel.setCategoryFilter(it)
                overlay = null
            },
            onDismiss = { overlay = null },
        )
        is Overlay.EditEntry -> {
            val existing = current.entry
            EntrySheet(
                initial = existing,
                currency = state.currency,
                onDismiss = { overlay = null },
                onSave = { type, amount, note, category, timestamp ->
                    viewModel.save(existing, type, amount, note, category, timestamp)
                    overlay = null
                    notify(res.getString(R.string.entry_updated))
                },
                onDelete = {
                    viewModel.delete(existing)
                    overlay = null
                    notify(res.getString(R.string.entry_deleted), undo = { viewModel.restore(existing) })
                },
            )
        }
    }

    if (addingEntry) {
        EntrySheet(
            initial = null,
            currency = state.currency,
            onDismiss = { addingEntry = false },
            onSave = { type, amount, note, category, timestamp ->
                viewModel.save(null, type, amount, note, category, timestamp)
                addingEntry = false
                notify(res.getString(R.string.entry_added))
            },
            onDelete = null,
        )
    }
}

@Composable
private fun HomeTopBar(
    state: HomeUiState,
    onSelectWallet: (Long) -> Unit,
    onOpenWallets: () -> Unit,
    onCreateWallet: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var walletMenu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Row(
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                        .clickable { walletMenu = true }
                        .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.wallet?.let { WalletIcon(it.emoji, it.currency, size = 22.dp) }
                    Spacer(Modifier.width(6.dp))
                    Text(state.wallet?.name ?: stringResource(R.string.wallet), style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.switch_wallet))
                }
                DropdownMenu(expanded = walletMenu, onDismissRequest = { walletMenu = false }) {
                    state.wallets.forEach { w ->
                        DropdownMenuItem(
                            leadingIcon = { WalletIcon(w.emoji, w.currency, size = 28.dp) },
                            text = { Text(w.name) },
                            trailingIcon = { Text(w.currency.name, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                onSelectWallet(w.id)
                                walletMenu = false
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_wallet)) },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = {
                            walletMenu = false
                            onCreateWallet()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.all_wallets)) },
                        onClick = {
                            walletMenu = false
                            onOpenWallets()
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
            }
        }
    }
}

@Composable
private fun SelectionTopBar(count: Int, allSelected: Boolean, onClose: () -> Unit, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_selection)) }
            Text(
                pluralStringResource(R.plurals.selected_count, count, count),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            IconButton(onClick = onSelectAll) {
                Icon(
                    Icons.Filled.SelectAll,
                    contentDescription = stringResource(R.string.select_all),
                    tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun BalanceHeader(state: HomeUiState, combined: CombinedTotal?, onOpenWallets: () -> Unit) {
    val cash = LocalCashColors.current
    val net = state.totalIn - state.totalOut
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (state.isFiltered) stringResource(R.string.net_month_filtered, state.month.month.getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0]))
            else stringResource(R.string.net_month, state.month.format(rememberFormatter(R.string.pattern_month_long))),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AmountText(
            full = formatMoney(net, state.currency),
            short = formatShort(net, state.currency, SHORT_FROM_MILLION),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = if (net < 0) cash.cashOut else MaterialTheme.colorScheme.onSurface,
        )
        state.wallet?.let {
            AmountText(
                full = stringResource(R.string.wallet_balance, formatMoney(it.balance, it.currency)),
                short = stringResource(R.string.wallet_balance, formatShort(it.balance, it.currency, SHORT_FROM_MILLION)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        combined?.let { AllWalletsLine(it, onOpenWallets) }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TotalCard(
            stringResource(R.string.cash_in),
            plusIfAny(state.totalIn) + formatMoney(state.totalIn, state.currency),
            plusIfAny(state.totalIn) + formatShort(state.totalIn, state.currency, SHORT_FROM_MILLION),
            cash.cashIn, cash.cashInBg, Modifier.weight(1f),
        )
        TotalCard(
            stringResource(R.string.cash_out),
            minusIfAny(state.totalOut) + formatMoney(state.totalOut, state.currency),
            minusIfAny(state.totalOut) + formatShort(state.totalOut, state.currency, SHORT_FROM_MILLION),
            cash.cashOut, cash.cashOutBg, Modifier.weight(1f),
        )
    }
}

@Composable
private fun AllWalletsLine(total: CombinedTotal, onClick: () -> Unit) {
    val amount = (if (total.approximate) "≈ " else "") +
        if (LocalShortAmounts.current) formatShort(total.minor, total.currency) else formatMoney(total.minor, total.currency)
    val missing = if (total.missing.isEmpty()) "" else " · " + pluralStringResource(R.plurals.not_included, total.missing.size, total.missing.size)
    Row(
        Modifier
            .padding(top = 2.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.all_wallets_total, amount) + missing,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TotalCard(label: String, full: String, short: String, color: Color, background: Color, modifier: Modifier) {
    Column(modifier.background(background, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(full, short, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun Toolbar(
    month: YearMonth,
    filterCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpenMonths: () -> Unit,
    onOpenFilter: () -> Unit,
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.border(1.dp, line, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
            }
            Row(
                Modifier.clickable(onClick = onOpenMonths).widthIn(min = 108.dp).padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(month.format(rememberFormatter(R.string.pattern_month_short)), style = MaterialTheme.typography.titleSmall)
            }
            IconButton(onClick = onNext, enabled = month < YearMonth.now(), modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(40.dp).border(1.dp, line, RoundedCornerShape(12.dp)).clickable(onClick = onOpenFilter),
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(badge = {
                if (filterCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("$filterCount") }
                }
            }) {
                Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.filter_by_category), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterChips(keys: Set<String>, onRemove: (String) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keys.forEach { key ->
            val category = Category.of(key)
            InputChip(
                selected = true,
                onClick = { onRemove(key) },
                label = { Text(category?.let { "${it.emoji} ${stringResource(it.label)}" } ?: stringResource(R.string.no_category)) },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_filter), Modifier.size(InputChipDefaults.IconSize)) },
            )
        }
    }
}

@Composable
private fun DayGroup(
    day: LocalDate,
    entries: List<Entry>,
    currency: Currency,
    selecting: Boolean,
    selectedIds: Set<Long>,
    onClick: (Entry) -> Unit,
    onLongClick: (Entry) -> Unit,
) {
    val cash = LocalCashColors.current
    val net = entries.sumOf { if (it.type == EntryType.IN) it.amount else -it.amount }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
            Text("${day.dayOfMonth}", fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                YearMonth.from(day).format(rememberFormatter(R.string.pattern_month_long)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            AmountText(
                full = formatSigned(net, currency),
                short = formatShortSigned(net, currency),
                style = MaterialTheme.typography.labelLarge,
                color = if (net >= 0) cash.cashIn else cash.cashOut,
            )
        }
        entries.forEach { entry ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            EntryRow(
                entry = entry,
                currency = currency,
                selecting = selecting,
                selected = entry.id in selectedIds,
                onClick = { onClick(entry) },
                onLongClick = { onLongClick(entry) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: Entry,
    currency: Currency,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val cash = LocalCashColors.current
    val category = Category.of(entry.category)
    val isIn = entry.type == EntryType.IN
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).format(rememberFormatter(R.string.pattern_time))
    val categoryLabel = category?.let { stringResource(it.label) }
    val typeLabel = stringResource(if (isIn) R.string.cash_in else R.string.cash_out)
    val noCategory = stringResource(R.string.no_category)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(end = 10.dp))
        }
        EntryIcon(entry.type, category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.note.ifBlank { categoryLabel ?: typeLabel },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${categoryLabel ?: noCategory} · $time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatEntry(entry.amount, entry.type, currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isIn) cash.cashIn else cash.cashOut,
        )
    }
}

/** Room under the list so the floating add button never covers the last entry. */
private val FAB_SPACE = 88.dp

private fun plusIfAny(amount: Long) = if (amount == 0L) "" else "+"

private fun minusIfAny(amount: Long) = if (amount == 0L) "" else "−"
