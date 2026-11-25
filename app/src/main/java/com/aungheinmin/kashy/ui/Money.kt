package com.aungheinmin.kashy.ui

import java.math.BigDecimal
import java.text.NumberFormat

private val formatter = NumberFormat.getNumberInstance().apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
}

fun formatAmount(minor: Long): String = formatter.format(BigDecimal.valueOf(minor, 2))

fun minorToInput(minor: Long): String = BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()

/** Returns null unless the text is a positive number with at most two decimals. */
fun parseAmount(text: String): Long? {
    val value = text.trim().replace(",", "").toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 2) return null
    return runCatching { value.movePointRight(2).longValueExact() }.getOrNull()
}
