package com.aungheinmin.kashy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ExchangeRatesTest {
    private val rates = ExchangeRates(mapOf(Currency.USD to BigDecimal("4500"), Currency.JPY to BigDecimal("30")))

    @Test
    fun pairsReadAtLeastOne() {
        assertEquals(RatePair(Currency.USD, Currency.MMK, BigDecimal("4500")), rates.pair(Currency.USD, Currency.MMK))
        assertEquals(RatePair(Currency.USD, Currency.MMK, BigDecimal("4500")), rates.pair(Currency.MMK, Currency.USD))
        assertEquals(RatePair(Currency.USD, Currency.JPY, BigDecimal("150")), rates.pair(Currency.JPY, Currency.USD))
        assertEquals(RatePair(Currency.USD, Currency.JPY, BigDecimal("150")), rates.pair(Currency.USD, Currency.JPY))
    }

    @Test
    fun editingAPairUpdatesTheKyatRate() {
        val usdMain = rates.withPair(Currency.JPY, Currency.USD, BigDecimal("100"))!!
        assertEquals(0, BigDecimal("45").compareTo(usdMain.of(Currency.JPY)))
        val mmkRow = rates.withPair(Currency.MMK, Currency.USD, BigDecimal("4000"))!!
        assertEquals(0, BigDecimal("4000").compareTo(mmkRow.of(Currency.USD)))
    }

    @Test
    fun convertsAndTotals() {
        assertEquals(594_225_000L, rates.convert(132_050, Currency.USD, Currency.MMK))
        val total = combinedTotal(
            listOf(Currency.MMK to 124_850_000L, Currency.USD to 132_050L, Currency.JPY to 0L),
            rates,
            Currency.MMK,
        )
        assertEquals(719_075_000L, total.minor)
        assertTrue(total.approximate)
        assertTrue(total.missing.isEmpty())
    }

    @Test
    fun missingRatesAreLeftOut() {
        val onlyUsd = ExchangeRates(mapOf(Currency.USD to BigDecimal("4500")))
        val total = combinedTotal(listOf(Currency.MMK to 100_00L, Currency.JPY to 500_00L), onlyUsd, Currency.MMK)
        assertEquals(100_00L, total.minor)
        assertFalse(total.approximate)
        assertEquals(listOf(Currency.JPY), total.missing)
        assertNull(onlyUsd.pair(Currency.JPY, Currency.USD).value)
        assertFalse(onlyUsd.canSet(Currency.USD, Currency.JPY))
        assertTrue(onlyUsd.canSet(Currency.MMK, Currency.JPY))
    }
}
