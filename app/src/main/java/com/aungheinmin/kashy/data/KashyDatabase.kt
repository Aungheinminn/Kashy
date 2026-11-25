package com.aungheinmin.kashy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Entry::class], version = 1, exportSchema = true)
abstract class KashyDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        fun create(context: Context): KashyDatabase =
            Room.databaseBuilder(context, KashyDatabase::class.java, "kashy.db").build()
    }
}
