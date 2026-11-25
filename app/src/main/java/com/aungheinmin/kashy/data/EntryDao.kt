package com.aungheinmin.kashy.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Entry>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) AS totalIn,
            COALESCE(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) AS totalOut
        FROM entries
        """
    )
    fun observeTotals(): Flow<Totals>

    @Upsert
    suspend fun upsert(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)
}
