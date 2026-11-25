package com.aungheinmin.kashy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EntryType { IN, OUT }

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    /** Stored in minor units (1/100) to avoid floating-point rounding. */
    val amount: Long,
    val note: String,
    val timestamp: Long,
)

data class Totals(
    val totalIn: Long,
    val totalOut: Long,
) {
    val balance: Long get() = totalIn - totalOut
}
