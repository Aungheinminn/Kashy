package com.aungheinmin.kashy.data

import androidx.annotation.StringRes
import androidx.room.Entity
import com.aungheinmin.kashy.R
import androidx.room.PrimaryKey

enum class Currency(@StringRes val label: Int, val decimals: Int) {
    MMK(R.string.currency_mmk, 0),
    USD(R.string.currency_usd, 2),
    JPY(R.string.currency_jpy, 0),
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
