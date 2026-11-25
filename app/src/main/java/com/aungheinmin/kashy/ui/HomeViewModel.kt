package com.aungheinmin.kashy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aungheinmin.kashy.KashyApp
import com.aungheinmin.kashy.data.Entry
import com.aungheinmin.kashy.data.EntryDao
import com.aungheinmin.kashy.data.EntryType
import com.aungheinmin.kashy.data.Totals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val entries: List<Entry> = emptyList(),
    val totals: Totals = Totals(0, 0),
)

class HomeViewModel(private val dao: EntryDao) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(dao.observeAll(), dao.observeTotals(), ::HomeUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun save(existing: Entry?, type: EntryType, amount: Long, note: String) {
        val entry = existing?.copy(type = type, amount = amount, note = note)
            ?: Entry(type = type, amount = amount, note = note, timestamp = System.currentTimeMillis())
        viewModelScope.launch { dao.upsert(entry) }
    }

    fun delete(entry: Entry) {
        viewModelScope.launch { dao.delete(entry) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as KashyApp
                HomeViewModel(app.database.entryDao())
            }
        }
    }
}
