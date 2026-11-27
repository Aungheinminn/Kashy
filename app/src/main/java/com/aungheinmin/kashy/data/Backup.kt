package com.aungheinmin.kashy.data

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Wallets and entries read from or written to a file. Each entry's walletId points at a wallet id inside the same backup. */
data class Backup(val wallets: List<Wallet>, val entries: List<Entry>)

enum class BackupFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
}

object BackupCodec {
    private const val APP = "kashy"
    private const val VERSION = 1
    private val CSV_HEADER = listOf("date", "wallet", "currency", "type", "amount", "category", "note")

    fun encode(backup: Backup, format: BackupFormat, zone: ZoneId, now: Long = System.currentTimeMillis()): String =
        when (format) {
            BackupFormat.JSON -> toJson(backup, now)
            BackupFormat.CSV -> toCsv(backup, zone)
        }

    /** Reads either format. Throws [IllegalArgumentException] when the text is neither. */
    fun decode(text: String, zone: ZoneId): Pair<Backup, BackupFormat> {
        val clean = text.removePrefix("﻿").trim()
        require(clean.isNotEmpty()) { "Empty file" }
        return if (clean.startsWith("{")) {
            runCatching { fromJson(clean) }.getOrElse { throw IllegalArgumentException("Not a Kashy backup", it) } to BackupFormat.JSON
        } else {
            fromCsv(clean, zone) to BackupFormat.CSV
        }
    }

    private fun toJson(backup: Backup, now: Long): String {
        val wallets = backup.wallets.map {
            JSONObject()
                .put("id", it.id)
                .put("name", it.name)
                .put("emoji", it.emoji)
                .put("currency", it.currency.name)
        }
        val entries = backup.entries.map {
            JSONObject()
                .put("wallet", it.walletId)
                .put("type", it.type.name)
                .put("amount", it.amount)
                .put("note", it.note)
                .put("timestamp", it.timestamp)
                .put("category", it.category ?: JSONObject.NULL)
        }
        return JSONObject()
            .put("app", APP)
            .put("version", VERSION)
            .put("exportedAt", now)
            .put("wallets", JSONArray(wallets))
            .put("entries", JSONArray(entries))
            .toString(2)
    }

    private fun fromJson(text: String): Backup {
        val root = JSONObject(text)
        require(root.optString("app") == APP) { "Not a Kashy backup" }
        val wallets = root.getJSONArray("wallets").objects().map {
            Wallet(
                id = it.getLong("id"),
                name = it.getString("name"),
                emoji = it.optString("emoji", ""),
                currency = Currency.valueOf(it.getString("currency")),
            )
        }
        val ids = wallets.map { it.id }.toSet()
        val entries = root.getJSONArray("entries").objects().map {
            val walletId = it.getLong("wallet")
            val amount = it.getLong("amount")
            require(walletId in ids) { "Entry points at a missing wallet" }
            require(amount > 0) { "Amount must be positive" }
            Entry(
                type = EntryType.valueOf(it.getString("type")),
                amount = amount,
                note = it.optString("note", ""),
                timestamp = it.getLong("timestamp"),
                walletId = walletId,
                category = if (it.isNull("category")) null else Category.of(it.optString("category"))?.name,
            )
        }
        return Backup(wallets, entries)
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

    private fun toCsv(backup: Backup, zone: ZoneId): String {
        val wallets = backup.wallets.associateBy { it.id }
        val dateFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val rows = backup.entries.sortedBy { it.timestamp }.map { e ->
            val wallet = wallets.getValue(e.walletId)
            listOf(
                Instant.ofEpochMilli(e.timestamp).atZone(zone).truncatedTo(ChronoUnit.SECONDS).format(dateFormat),
                wallet.name,
                wallet.currency.name,
                e.type.name.lowercase(),
                BigDecimal.valueOf(e.amount, 2).setScale(wallet.currency.decimals, RoundingMode.HALF_UP).toPlainString(),
                e.category?.lowercase() ?: "",
                e.note,
            )
        }
        // The BOM makes Excel read the file as UTF-8, so Burmese and Japanese notes show correctly.
        return "﻿" + (listOf(CSV_HEADER) + rows).joinToString("\r\n") { row -> row.joinToString(",") { csvField(it) } } + "\r\n"
    }

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    private fun fromCsv(text: String, zone: ZoneId): Backup {
        val rows = parseCsv(text)
        require(rows.isNotEmpty()) { "Empty file" }
        val header = rows.first().map { it.trim().lowercase() }
        fun column(name: String) = header.indexOf(name)
        val date = column("date")
        val type = column("type")
        val amount = column("amount")
        require(date >= 0 && type >= 0 && amount >= 0) { "Missing date, type or amount column" }
        val walletName = column("wallet")
        val currency = column("currency")
        val category = column("category")
        val note = column("note")

        val wallets = linkedMapOf<Pair<String, Currency>, Wallet>()
        val entries = rows.drop(1).mapIndexed { index, row ->
            val line = index + 2
            fun cell(i: Int) = if (i >= 0) row.getOrElse(i) { "" }.trim() else ""
            val cur = cell(currency).ifEmpty { Currency.MMK.name }.let { c ->
                Currency.entries.firstOrNull { it.name.equals(c, ignoreCase = true) }
                    ?: throw IllegalArgumentException("Row $line: unknown currency $c")
            }
            val name = cell(walletName).ifEmpty { "Imported" }
            val wallet = wallets.getOrPut(name to cur) { Wallet(id = wallets.size + 1L, name = name, emoji = "", currency = cur) }
            Entry(
                type = parseType(cell(type)) ?: throw IllegalArgumentException("Row $line: type must be in or out"),
                amount = parseMinor(cell(amount), cur) ?: throw IllegalArgumentException("Row $line: bad amount"),
                note = cell(note),
                timestamp = parseTime(cell(date), zone) ?: throw IllegalArgumentException("Row $line: bad date"),
                walletId = wallet.id,
                category = Category.entries.firstOrNull { it.name.equals(cell(category), ignoreCase = true) }?.name,
            )
        }
        require(entries.isNotEmpty()) { "No entries" }
        return Backup(wallets.values.toList(), entries)
    }

    private fun parseType(text: String): EntryType? = when (text.lowercase()) {
        "in", "cash in" -> EntryType.IN
        "out", "cash out" -> EntryType.OUT
        else -> null
    }

    private fun parseMinor(text: String, currency: Currency): Long? {
        val value = text.replace(",", "").toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0 || value.stripTrailingZeros().scale() > currency.decimals) return null
        return runCatching { value.movePointRight(2).longValueExact() }.getOrNull()
    }

    private fun parseTime(text: String, zone: ZoneId): Long? {
        val t = text.replace(' ', 'T')
        return runCatching { OffsetDateTime.parse(t).toInstant().toEpochMilli() }
            .recoverCatching { LocalDateTime.parse(t).atZone(zone).toInstant().toEpochMilli() }
            .recoverCatching { LocalDate.parse(t).atStartOfDay(zone).toInstant().toEpochMilli() }
            .getOrNull()
    }

    internal fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        quoted = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> quoted = true
                    ',' -> { row.add(field.toString()); field.clear() }
                    '\r' -> Unit
                    '\n' -> { row.add(field.toString()); field.clear(); rows.add(row); row = mutableListOf() }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }
}
