package com.aungheinmin.kashy.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EntryType { IN, OUT }

@Entity(tableName = "entries", indices = [Index("walletId")])
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    /** Stored in hundredths for every currency, so a wallet's currency never changes how rows are read. */
    val amount: Long,
    val note: String,
    val timestamp: Long,
    @ColumnInfo(defaultValue = "1") val walletId: Long,
    /** A [com.aungheinmin.kashy.data.Category] key, or null when the user skipped it. */
    val category: String? = null,
)
