package com.aungheinmin.kashy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.ZoneId

class BackupCodecTest {
    private val zone = ZoneId.of("Asia/Yangon")
    private val backup = Backup(
        wallets = listOf(
            Wallet(id = 1, name = "Cash", emoji = "💵", currency = Currency.MMK),
            Wallet(id = 7, name = "Savings, USD", emoji = "", currency = Currency.USD),
        ),
        entries = listOf(
            Entry(type = EntryType.OUT, amount = 1_200_000, note = "ထမင်း \"lunch\"", timestamp = 1_764_000_000_000, walletId = 1, category = "FOOD"),
            Entry(type = EntryType.IN, amount = 125_050, note = "line one\nline two", timestamp = 1_764_100_000_000, walletId = 7),
        ),
    )

    @Test
    fun jsonRoundTrip() {
        val (decoded, format) = BackupCodec.decode(BackupCodec.encode(backup, BackupFormat.JSON, zone), zone)
        assertEquals(BackupFormat.JSON, format)
        assertEquals(backup, decoded)
    }

    @Test
    fun csvRoundTripKeepsEntriesAndWallets() {
        val text = BackupCodec.encode(backup, BackupFormat.CSV, zone)
        val (decoded, format) = BackupCodec.decode(text, zone)
        assertEquals(BackupFormat.CSV, format)
        assertEquals(listOf("Cash" to Currency.MMK, "Savings, USD" to Currency.USD), decoded.wallets.map { it.name to it.currency })
        val byWallet = decoded.wallets.associate { it.id to it.name }
        assertEquals(
            backup.entries.map { listOf(it.type, it.amount, it.note, it.timestamp, it.category) },
            decoded.entries.map { listOf(it.type, it.amount, it.note, it.timestamp, it.category) },
        )
        assertEquals(listOf("Cash", "Savings, USD"), decoded.entries.map { byWallet.getValue(it.walletId) })
    }

    @Test
    fun csvAcceptsHandWrittenRows() {
        val text = "Type,Amount,Date,Note\nout,\"12,000\",2026-11-25 12:30,Taxi\nIN,5000,2026-11-24,\n"
        val (decoded, _) = BackupCodec.decode(text, zone)
        assertEquals(listOf("Imported"), decoded.wallets.map { it.name })
        assertEquals(listOf(1_200_000L, 500_000L), decoded.entries.map { it.amount })
        assertEquals(listOf(EntryType.OUT, EntryType.IN), decoded.entries.map { it.type })
    }

    @Test
    fun rejectsOtherFiles() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("{\"hello\": 1}", zone) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("name,age\nbob,3\n", zone) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("type,amount,date\nout,12.5,2026-11-25\n", zone) }
    }
}
