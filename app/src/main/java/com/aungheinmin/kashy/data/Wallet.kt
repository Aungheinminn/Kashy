package com.aungheinmin.kashy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Currency(val label: String, val decimals: Int) {
    MMK("Myanmar Kyat", 0),
    USD("US Dollar", 2),
    JPY("Japanese Yen", 0),
}

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val currency: Currency,
)

data class WalletSummary(
    val id: Long,
    val name: String,
    val emoji: String,
    val currency: Currency,
    val balance: Long,
    val entryCount: Int,
)
