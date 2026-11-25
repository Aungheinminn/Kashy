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
