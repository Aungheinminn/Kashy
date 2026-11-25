package com.aungheinmin.kashy.ui

import com.aungheinmin.kashy.data.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    private fun major(n: Long) = n * 100

    @Test
    fun fullAmounts() {
        assertEquals("1,250,000 MMK", formatMoney(major(1_250_000), Currency.MMK))
        assertEquals("$1,250.50", formatMoney(125_050, Currency.USD))
        assertEquals("¥1,250", formatMoney(major(1_250), Currency.JPY))
        assertEquals("−$1,250.50", formatMoney(-125_050, Currency.USD))
    }

    @Test
    fun shortAmounts() {
        assertEquals("125K MMK", formatShort(major(125_000), Currency.MMK))
        assertEquals("1.25M MMK", formatShort(major(1_250_000), Currency.MMK))
        assertEquals("1.2M MMK", formatShort(major(1_200_000), Currency.MMK))
        assertEquals("$1.3K", formatShort(major(1_320), Currency.USD))
        assertEquals("¥125K", formatShort(major(125_000), Currency.JPY))
        assertEquals("2.5B MMK", formatShort(major(2_500_000_000), Currency.MMK))
        assertEquals("−125K MMK", formatShort(-major(125_000), Currency.MMK))
    }

    @Test
    fun shortAmountsStayFullBelowOneThousand() {
        assertEquals("$999.99", formatShort(99_999, Currency.USD))
        assertEquals("850 MMK", formatShort(major(850), Currency.MMK))
    }

    @Test
    fun roundingMovesUpToTheNextUnit() {
        assertEquals("1M MMK", formatShort(major(999_950), Currency.MMK))
    }

    @Test
    fun balancesOnlyShortenFromOneMillion() {
        assertEquals("548,500 MMK", formatShort(major(548_500), Currency.MMK, SHORT_FROM_MILLION))
        assertEquals("1.25M MMK", formatShort(major(1_250_000), Currency.MMK, SHORT_FROM_MILLION))
    }

    @Test
    fun signedShortAmounts() {
        assertEquals("+45K MMK", formatShortSigned(major(45_000), Currency.MMK))
        assertEquals("−20K MMK", formatShortSigned(-major(20_000), Currency.MMK))
    }

    @Test
    fun parseRespectsCurrencyDecimals() {
        assertEquals(1_250_000L, parseAmount("12,500", Currency.MMK))
        assertNull(parseAmount("12.5", Currency.MMK))
        assertEquals(1_250L, parseAmount("12.50", Currency.USD))
        assertNull(parseAmount("0", Currency.USD))
    }
}
