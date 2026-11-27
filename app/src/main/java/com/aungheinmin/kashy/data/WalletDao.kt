package com.aungheinmin.kashy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query(
        """
        SELECT w.id, w.name, w.emoji, w.currency,
            COALESCE(SUM(CASE WHEN e.type = 'IN' THEN e.amount WHEN e.type = 'OUT' THEN -e.amount ELSE 0 END), 0) AS balance,
            COUNT(e.id) AS entryCount
        FROM wallets w LEFT JOIN entries e ON e.walletId = w.id
        GROUP BY w.id
        ORDER BY w.id
        """
    )
    fun observeSummaries(): Flow<List<WalletSummary>>

    @Query("SELECT * FROM wallets ORDER BY id")
    suspend fun getAll(): List<Wallet>

    @Insert
    suspend fun insert(wallet: Wallet): Long

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()
}
