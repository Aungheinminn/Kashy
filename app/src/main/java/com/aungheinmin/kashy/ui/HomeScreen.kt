package com.aungheinmin.kashy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aungheinmin.kashy.data.Category
import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.Entry
import com.aungheinmin.kashy.data.EntryType
import com.aungheinmin.kashy.data.ThemeMode
import com.aungheinmin.kashy.ui.theme.LocalCashColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val monthShort = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
private val monthLong = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

private sealed interface Overlay {
    data object NewEntry : Overlay
    data class EditEntry(val entry: Entry) : Overlay
    data object MonthPicker : Overlay
    data object Filter : Overlay
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    themeMode: ThemeMode,
    onOpenWallets: () -> Unit,
    onCreateWallet: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(message: String, undo: (() -> Unit)? = null) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = undo?.let { "Undo" }, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            HomeTopBar(
                state = state,
                themeMode = themeMode,
                onSelectWallet = viewModel::selectWallet,
                onOpenWallets = onOpenWallets,
                onCreateWallet = onCreateWallet,
                onThemeMode = viewModel::setThemeMode,
                onShortAmounts = viewModel::setShortAmounts,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { overlay = Overlay.NewEntry },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add entry") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
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
            item { BalanceHeader(state) }
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
                SegmentedChoice(
                    options = TypeFilter.entries,
                    selected = state.typeFilter,
                    label = {
                        when (it) {
                            TypeFilter.ALL -> "All"
                            TypeFilter.IN -> "Cash in"
                            TypeFilter.OUT -> "Cash out"
                        }
                    },
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
                        if (state.isFiltered) "No entries match these filters." else "No entries this month.\nTap \"Add entry\" to record cash in or out.",
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
                DayGroup(day, entries, state.currency, onClick = { overlay = Overlay.EditEntry(it) })
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
        Overlay.NewEntry, is Overlay.EditEntry -> {
            val existing = (current as? Overlay.EditEntry)?.entry
            EntrySheet(
                initial = existing,
                currency = state.currency,
                onDismiss = { overlay = null },
                onSave = { type, amount, note, category, timestamp ->
                    viewModel.save(existing, type, amount, note, category, timestamp)
                    overlay = null
                    notify(if (existing == null) "Entry added" else "Entry updated")
                },
                onDelete = existing?.let {
                    {
                        viewModel.delete(it)
                        overlay = null
                        notify("Entry deleted", undo = { viewModel.restore(it) })
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    state: HomeUiState,
    themeMode: ThemeMode,
    onSelectWallet: (Long) -> Unit,
    onOpenWallets: () -> Unit,
    onCreateWallet: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onShortAmounts: (Boolean) -> Unit,
) {
    val shortAmounts = LocalShortAmounts.current
    var walletMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
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
                    Text(state.wallet?.name ?: "Wallet", style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch wallet")
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
                        text = { Text("New wallet") },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = {
                            walletMenu = false
                            onCreateWallet()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("All wallets") },
                        onClick = {
                            walletMenu = false
                            onOpenWallets()
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { moreMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Wallets") },
                        onClick = {
                            moreMenu = false
                            onOpenWallets()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Short amounts") },
                        trailingIcon = { Switch(checked = shortAmounts, onCheckedChange = null) },
                        onClick = { onShortAmounts(!shortAmounts) },
                    )
                    HorizontalDivider()
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    ThemeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "System default"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    }
                                )
                            },
                            leadingIcon = { RadioButton(selected = mode == themeMode, onClick = null) },
                            onClick = {
                                onThemeMode(mode)
                                moreMenu = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceHeader(state: HomeUiState) {
    val cash = LocalCashColors.current
    val net = state.totalIn - state.totalOut
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (state.isFiltered) "Net · ${state.month.month.getDisplayName(TextStyle.FULL, Locale.US)} (filtered)"
            else "Net · ${state.month.format(monthLong)}",
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
                full = "Wallet balance ${formatMoney(it.balance, it.currency)}",
                short = "Wallet balance ${formatShort(it.balance, it.currency, SHORT_FROM_MILLION)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TotalCard(
            "Cash in",
            plusIfAny(state.totalIn) + formatMoney(state.totalIn, state.currency),
            plusIfAny(state.totalIn) + formatShort(state.totalIn, state.currency, SHORT_FROM_MILLION),
            cash.cashIn, cash.cashInBg, Modifier.weight(1f),
        )
        TotalCard(
            "Cash out",
            minusIfAny(state.totalOut) + formatMoney(state.totalOut, state.currency),
            minusIfAny(state.totalOut) + formatShort(state.totalOut, state.currency, SHORT_FROM_MILLION),
            cash.cashOut, cash.cashOutBg, Modifier.weight(1f),
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
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Row(
                Modifier.clickable(onClick = onOpenMonths).widthIn(min = 108.dp).padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(month.format(monthShort), style = MaterialTheme.typography.titleSmall)
            }
            IconButton(onClick = onNext, enabled = month < YearMonth.now(), modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
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
                Icon(Icons.Filled.FilterList, contentDescription = "Filter by category", modifier = Modifier.size(20.dp))
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
                label = { Text(category?.let { "${it.emoji} ${it.label}" } ?: "No category") },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove filter", Modifier.size(InputChipDefaults.IconSize)) },
            )
        }
    }
}

@Composable
private fun DayGroup(day: LocalDate, entries: List<Entry>, currency: Currency, onClick: (Entry) -> Unit) {
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
                YearMonth.from(day).format(monthLong),
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
            EntryRow(entry, currency, onClick = { onClick(entry) })
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, currency: Currency, onClick: () -> Unit) {
    val cash = LocalCashColors.current
    val category = Category.of(entry.category)
    val isIn = entry.type == EntryType.IN
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).format(timeFormat)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIcon(entry.type, category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.note.ifBlank { category?.label ?: if (isIn) "Cash in" else "Cash out" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${category?.label ?: "No category"} · $time",
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

private fun plusIfAny(amount: Long) = if (amount == 0L) "" else "+"

private fun minusIfAny(amount: Long) = if (amount == 0L) "" else "−"
