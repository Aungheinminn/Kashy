package com.aungheinmin.kashy.ui

import androidx.room.withTransaction
import com.aungheinmin.kashy.data.AmountStyle
import com.aungheinmin.kashy.data.AppLanguage
import com.aungheinmin.kashy.data.Backup
import com.aungheinmin.kashy.data.ExchangeRates
import com.aungheinmin.kashy.data.KashyDatabase
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
import java.math.BigDecimal
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
    private val database: KashyDatabase,
    private val settings: Settings,
) : ViewModel() {
    private val entryDao: EntryDao = database.entryDao()
    private val walletDao: WalletDao = database.walletDao()

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

    fun setAmountStyle(style: AmountStyle) = settings.setAmountStyle(style)

    val rates: StateFlow<ExchangeRates> = settings.rates

    val totalCurrency: StateFlow<Currency> = settings.totalCurrency

    fun setTotalCurrency(currency: Currency) = settings.setTotalCurrency(currency)

    /** Sets the rate shown on [currency]'s row; returns false when the other rate it depends on is missing. */
    fun setRate(currency: Currency, value: BigDecimal): Boolean {
        val updated = settings.rates.value.withPair(currency, settings.totalCurrency.value, value) ?: return false
        settings.setRates(updated)
        return true
    }

    fun updateWallet(id: Long, name: String, emoji: String) {
        viewModelScope.launch { walletDao.updateDetails(id, name, emoji) }
    }

    /** Deletes the wallet and its entries. The last wallet is kept so there is always one to add entries to. */
    fun deleteWallet(id: Long) {
        viewModelScope.launch {
            val remaining = database.withTransaction {
                if (walletDao.getAll().size <= 1) return@withTransaction null
                entryDao.deleteForWallet(id)
                walletDao.delete(id)
                walletDao.getAll()
            } ?: return@launch
            if (remaining.none { it.id == settings.walletId.value }) settings.setWalletId(remaining.first().id)
        }
    }

    suspend fun exportBackup(): Backup = database.withTransaction { Backup(walletDao.getAll(), entryDao.getAll()) }

    /**
     * Adds [backup] to the current data, or replaces it when [replace] is set. Wallets with the same name and
     * currency are merged, and entries identical to one already saved are skipped. Returns how many entries were added.
     */
    suspend fun importBackup(backup: Backup, replace: Boolean): Int = database.withTransaction {
        if (replace) {
            entryDao.deleteAll()
            walletDao.deleteAll()
        }
        val existing = walletDao.getAll()
        val walletIds = backup.wallets.associate { w ->
            val match = existing.firstOrNull { it.name == w.name && it.currency == w.currency }
            w.id to (match?.id ?: walletDao.insert(w.copy(id = 0)))
        }
        val seen = entryDao.getAll().mapTo(HashSet()) { it.copy(id = 0) }
        val fresh = backup.entries
            .map { it.copy(id = 0, walletId = walletIds.getValue(it.walletId)) }
            .filter { seen.add(it) }
        entryDao.insertAll(fresh)

        var wallets = walletDao.getAll()
        if (wallets.isEmpty()) {
            walletDao.insert(Wallet(name = "Cash", emoji = "💵", currency = Currency.MMK))
            wallets = walletDao.getAll()
        }
        if (replace || wallets.none { it.id == settings.walletId.value }) {
            settings.setWalletId(walletIds.values.firstOrNull() ?: wallets.first().id)
        }
        fresh.size
    }

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
                HomeViewModel(app.database, app.settings)
            }
        }
    }
}
