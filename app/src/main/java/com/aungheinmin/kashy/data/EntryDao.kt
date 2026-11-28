package com.aungheinmin.kashy.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query(
        """
        SELECT * FROM entries
        WHERE walletId = :walletId AND timestamp >= :from AND timestamp < :until
        ORDER BY timestamp DESC
        """
    )
    fun observeRange(walletId: Long, from: Long, until: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY timestamp")
    suspend fun getAll(): List<Entry>

    @Insert
    suspend fun insertAll(entries: List<Entry>)

    @Query("DELETE FROM entries WHERE walletId = :walletId")
    suspend fun deleteForWallet(walletId: Long)

    @Query("DELETE FROM entries")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Delete
    suspend fun deleteEntries(entries: List<Entry>)

    @Upsert
    suspend fun upsertAll(entries: List<Entry>)
}
