package com.aungheinmin.kashy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Entry::class, Wallet::class], version = 2, exportSchema = true)
abstract class KashyDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun walletDao(): WalletDao

    companion object {
        private const val INSERT_DEFAULT_WALLET =
            "INSERT INTO wallets (id, name, emoji, currency) VALUES (1, 'Cash', '💵', 'MMK')"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS wallets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, emoji TEXT NOT NULL, currency TEXT NOT NULL)"
                )
                db.execSQL(INSERT_DEFAULT_WALLET)
                db.execSQL("ALTER TABLE entries ADD COLUMN walletId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE entries ADD COLUMN category TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_entries_walletId ON entries (walletId)")
            }
        }

        private val seedOnCreate = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(INSERT_DEFAULT_WALLET)
            }
        }

        fun create(context: Context): KashyDatabase =
            Room.databaseBuilder(context, KashyDatabase::class.java, "kashy.db")
                .addMigrations(MIGRATION_1_2)
                .addCallback(seedOnCreate)
                .build()
    }
}
