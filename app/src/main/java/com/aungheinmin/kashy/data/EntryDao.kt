package com.aungheinmin.kashy.data

import androidx.room.Dao
import androidx.room.Delete
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

    @Upsert
    suspend fun upsert(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)
}
