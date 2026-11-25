package com.aungheinmin.kashy.ui

import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.EntryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Fixed to US grouping so amounts read the same whatever the phone's language is.
private val symbols = DecimalFormatSymbols(Locale.US)

private fun pattern(decimals: Int) =
    DecimalFormat(if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals), symbols)

private fun number(minor: Long, currency: Currency): String =
    pattern(currency.decimals).format(BigDecimal.valueOf(minor, 2).setScale(currency.decimals, RoundingMode.HALF_UP))

fun Currency.symbol(): String = when (this) {
    Currency.MMK -> "MMK"
    Currency.USD -> "$"
    Currency.JPY -> "¥"
}

/** [minor] is in hundredths; negative values get a typographic minus. */
fun formatMoney(minor: Long, currency: Currency): String {
    val n = number(kotlin.math.abs(minor), currency)
    val body = when (currency) {
        Currency.MMK -> "$n MMK"
        Currency.USD -> "$$n"
        Currency.JPY -> "¥$n"
    }
    return if (minor < 0) "−$body" else body
}

fun formatSigned(minor: Long, currency: Currency): String =
    (if (minor >= 0) "+" else "") + formatMoney(minor, currency)

fun formatEntry(amount: Long, type: EntryType, currency: Currency): String =
    (if (type == EntryType.IN) "+" else "−") + formatMoney(amount, currency)

fun minorToInput(minor: Long, currency: Currency): String =
    BigDecimal.valueOf(minor, 2).setScale(currency.decimals, RoundingMode.HALF_UP).toPlainString()

/** Returns hundredths, or null unless the text is positive and fits the currency's decimals. */
fun parseAmount(text: String, currency: Currency): Long? {
    val value = text.trim().replace(",", "").toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value.stripTrailingZeros().scale() > currency.decimals) return null
    return runCatching { value.movePointRight(2).longValueExact() }.getOrNull()
}

private val shortUnits = listOf(
    BigDecimal("1000") to "K",
    BigDecimal("1000000") to "M",
    BigDecimal("1000000000") to "B",
)

/**
 * Shortens to K / M / B once the whole amount reaches [from], e.g. `125K MMK`, `1.25M MMK`, `$1.3K`.
 * Below 1,000 it is always the full amount, so dollar amounts keep their cents.
 */
fun formatShort(minor: Long, currency: Currency, from: Long = 1_000): String {
    val major = BigDecimal.valueOf(kotlin.math.abs(minor), 2)
    if (major < BigDecimal.valueOf(maxOf(from, 1_000))) return formatMoney(minor, currency)
    // Walk up the units so a value like 999,950 becomes 1M instead of 1000K.
    val (n, suffix) = shortUnits.firstNotNullOf { (divisor, suffix) ->
        val rounded = major.divide(divisor).setScale(if (suffix == "K") 1 else 2, RoundingMode.HALF_UP)
        if (rounded < BigDecimal("1000") || suffix == "B") rounded.stripTrailingZeros().toPlainString() to suffix else null
    }
    val body = when (currency) {
        Currency.MMK -> "$n$suffix MMK"
        Currency.USD -> "$$n$suffix"
        Currency.JPY -> "¥$n$suffix"
    }
    return if (minor < 0) "−$body" else body
}

fun formatShortSigned(minor: Long, currency: Currency, from: Long = 1_000): String =
    (if (minor >= 0) "+" else "") + formatShort(minor, currency, from)

/** Balances only shorten from 1 million so everyday amounts stay exact. */
const val SHORT_FROM_MILLION = 1_000_000L
