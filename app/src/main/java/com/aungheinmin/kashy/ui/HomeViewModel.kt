package com.aungheinmin.kashy.ui

import com.aungheinmin.kashy.data.AppLanguage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aungheinmin.kashy.KashyApp
import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.Entry
import com.aungheinmin.kashy.data.EntryDao
import com.aungheinmin.kashy.data.EntryType
import com.aungheinmin.kashy.data.Settings
import com.aungheinmin.kashy.data.ThemeMode
import com.aungheinmin.kashy.data.Wallet
import com.aungheinmin.kashy.data.WalletDao
import com.aungheinmin.kashy.data.WalletSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

enum class TypeFilter { ALL, IN, OUT }

/** Category filter key used for entries saved without a category. */
const val NO_CATEGORY = "none"

data class HomeUiState(
    val wallets: List<WalletSummary> = emptyList(),
    val wallet: WalletSummary? = null,
    val month: YearMonth = YearMonth.now(),
    val typeFilter: TypeFilter = TypeFilter.ALL,
    val categoryFilter: Set<String> = emptySet(),
    val entries: List<Entry> = emptyList(),
) {
    val currency: Currency get() = wallet?.currency ?: Currency.MMK
    val isFiltered: Boolean get() = typeFilter != TypeFilter.ALL || categoryFilter.isNotEmpty()
    val totalIn: Long get() = entries.filter { it.type == EntryType.IN }.sumOf { it.amount }
    val totalOut: Long get() = entries.filter { it.type == EntryType.OUT }.sumOf { it.amount }
}

private data class Filters(val month: YearMonth, val type: TypeFilter, val categories: Set<String>)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val entryDao: EntryDao,
    private val walletDao: WalletDao,
    private val settings: Settings,
) : ViewModel() {

    private val filters = MutableStateFlow(Filters(YearMonth.now(), TypeFilter.ALL, emptySet()))

    private val monthEntries = combine(settings.walletId, filters) { id, f -> id to f.month }
        .flatMapLatest { (id, month) ->
            val zone = ZoneId.systemDefault()
            entryDao.observeRange(
                walletId = id,
                from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                until = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            )
        }

    val uiState: StateFlow<HomeUiState> =
        combine(walletDao.observeSummaries(), settings.walletId, filters, monthEntries) { wallets, id, f, entries ->
            HomeUiState(
                wallets = wallets,
                wallet = wallets.firstOrNull { it.id == id } ?: wallets.firstOrNull(),
                month = f.month,
                typeFilter = f.type,
                categoryFilter = f.categories,
                entries = entries.filter { e ->
                    (f.type == TypeFilter.ALL || e.type.name == f.type.name) &&
                        (f.categories.isEmpty() || (e.category ?: NO_CATEGORY) in f.categories)
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setMonth(month: YearMonth) {
        if (month <= YearMonth.now()) filters.value = filters.value.copy(month = month)
    }

    fun setTypeFilter(type: TypeFilter) {
        filters.value = filters.value.copy(type = type)
    }

    fun setCategoryFilter(categories: Set<String>) {
        filters.value = filters.value.copy(categories = categories)
    }

    fun selectWallet(id: Long) = settings.setWalletId(id)

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)

    fun setLanguage(language: AppLanguage) = settings.setLanguage(language)

    fun setShortAmounts(enabled: Boolean) = settings.setShortAmounts(enabled)

    fun createWallet(name: String, emoji: String, currency: Currency) {
        viewModelScope.launch {
            val id = walletDao.insert(Wallet(name = name, emoji = emoji, currency = currency))
            settings.setWalletId(id)
        }
    }

    fun save(existing: Entry?, type: EntryType, amount: Long, note: String, category: String?, timestamp: Long) {
        val walletId = uiState.value.wallet?.id ?: return
        val entry = existing?.copy(type = type, amount = amount, note = note, category = category, timestamp = timestamp)
            ?: Entry(type = type, amount = amount, note = note, timestamp = timestamp, walletId = walletId, category = category)
        viewModelScope.launch { entryDao.upsert(entry) }
    }

    fun delete(entry: Entry) {
        viewModelScope.launch { entryDao.delete(entry) }
    }

    fun restore(entry: Entry) {
        viewModelScope.launch { entryDao.upsert(entry) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as KashyApp
                HomeViewModel(app.database.entryDao(), app.database.walletDao(), app.settings)
            }
        }
    }
}
