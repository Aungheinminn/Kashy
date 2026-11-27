package com.aungheinmin.kashy.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** How one currency reads against another, e.g. `1 USD = 4,500 MMK`. [value] is null until the needed rates are set. */
data class RatePair(val from: Currency, val to: Currency, val value: BigDecimal?)

/**
 * Rates the user typed in, stored as kyat per one unit of each currency so only two numbers are ever kept.
 * Any other pair, such as USD to JPY, is worked out from them.
 */
data class ExchangeRates(val kyatPer: Map<Currency, BigDecimal> = emptyMap()) {

    fun of(currency: Currency): BigDecimal? = if (currency == Currency.MMK) BigDecimal.ONE else kyatPer[currency]

    /** Converts hundredths of [from] into hundredths of [to], or null when a rate is missing. */
    fun convert(minor: Long, from: Currency, to: Currency): Long? {
        if (from == to) return minor
        val a = of(from) ?: return null
        val b = of(to) ?: return null
        return BigDecimal.valueOf(minor).multiply(a).divide(b, 0, RoundingMode.HALF_UP).toLong()
    }

    /** [currency] against [main], written in the direction whose number is at least 1. */
    fun pair(currency: Currency, main: Currency): RatePair {
        val a = of(currency)
        val b = of(main)
        if (a == null || b == null) {
            return if (currency == Currency.MMK) RatePair(main, Currency.MMK, null)
            else if (main == Currency.MMK) RatePair(currency, Currency.MMK, null)
            else RatePair(currency, main, null)
        }
        val value = a.divide(b, MathContext.DECIMAL64)
        return if (value >= BigDecimal.ONE) RatePair(currency, main, value.stripTrailingZeros())
        else RatePair(main, currency, b.divide(a, MathContext.DECIMAL64).stripTrailingZeros())
    }

    /** Whether the row for [currency] can be edited; a USD/JPY pair needs the other one's kyat rate first. */
    fun canSet(currency: Currency, main: Currency): Boolean {
        val other = if (currency == Currency.MMK) Currency.MMK else main
        return other == Currency.MMK || of(other) != null
    }

    /** Applies "1 [RatePair.from] = [value] [RatePair.to]" for the row of [currency], as shown by [pair]. */
    fun withPair(currency: Currency, main: Currency, value: BigDecimal): ExchangeRates? {
        if (value.signum() <= 0 || currency == main) return null
        val shown = pair(currency, main)
        val changed = if (currency == Currency.MMK) main else currency
        val other = if (shown.from == changed) shown.to else shown.from
        val otherKyat = of(other) ?: return null
        val kyat = if (shown.from == changed) value.multiply(otherKyat) else otherKyat.divide(value, MathContext.DECIMAL64)
        return copy(kyatPer = kyatPer + (changed to kyat.stripTrailingZeros()))
    }
}

/** Sum of every wallet in [currency]. Currencies without a rate are left out and counted in [missing]. */
data class CombinedTotal(val minor: Long, val currency: Currency, val approximate: Boolean, val missing: List<Currency>)

fun combinedTotal(balances: List<Pair<Currency, Long>>, rates: ExchangeRates, currency: Currency): CombinedTotal {
    val byCurrency = balances.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
    var total = 0L
    val missing = mutableListOf<Currency>()
    byCurrency.forEach { (c, minor) ->
        val converted = rates.convert(minor, c, currency)
        if (converted == null) missing += c else total += converted
    }
    return CombinedTotal(total, currency, approximate = byCurrency.keys.any { it != currency && it !in missing }, missing = missing)
}
