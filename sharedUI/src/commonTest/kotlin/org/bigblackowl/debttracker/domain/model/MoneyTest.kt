package org.bigblackowl.debttracker.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {

    @Test
    fun `formatMoney appends currency symbol`() {
        assertEquals("1234.56 ₴", BigDecimal.parseString("1234.56").formatMoney(Currency.UAH))
    }

    @Test
    fun `formatMoney uses correct symbol per currency`() {
        assertEquals("10 $", BigDecimal.parseString("10").formatMoney(Currency.USD))
        assertEquals("10 zł", BigDecimal.parseString("10").formatMoney(Currency.PLN))
        assertEquals("10 €", BigDecimal.parseString("10").formatMoney(Currency.EUR))
    }

    private data class Item(val currency: Currency, val amount: BigDecimal)

    @Test
    fun `sumByCurrency groups and sums independently per currency`() {
        val items = listOf(
            Item(Currency.UAH, BigDecimal.parseString("100")),
            Item(Currency.UAH, BigDecimal.parseString("50")),
            Item(Currency.USD, BigDecimal.parseString("20")),
        )

        val totals = items.sumByCurrency({ it.currency }, { it.amount })

        assertEquals(0, BigDecimal.parseString("150").compareTo(totals.getValue(Currency.UAH)))
        assertEquals(0, BigDecimal.parseString("20").compareTo(totals.getValue(Currency.USD)))
    }

    @Test
    fun `sumByCurrency on empty list yields empty map`() {
        val totals = emptyList<Item>().sumByCurrency({ it.currency }, { it.amount })
        assertEquals(emptyMap(), totals)
    }

    @Test
    fun `formatTotals renders each currency on its own line`() {
        val totals = mapOf(
            Currency.UAH to BigDecimal.parseString("100"),
            Currency.USD to BigDecimal.parseString("20"),
        )
        assertEquals("100 ₴\n20 $", totals.formatTotals())
    }

    @Test
    fun `formatTotals hides currencies with zero balance`() {
        val totals = mapOf(
            Currency.UAH to BigDecimal.parseString("100"),
            Currency.USD to BigDecimal.ZERO,
        )
        assertEquals("100 ₴", totals.formatTotals())
    }

    @Test
    fun `formatTotals on empty map defaults to zero UAH`() {
        assertEquals("0 ₴", emptyMap<Currency, BigDecimal>().formatTotals())
    }

    @Test
    fun `formatTotals on all-zero map falls back to zero UAH`() {
        val totals = mapOf(Currency.UAH to BigDecimal.ZERO, Currency.USD to BigDecimal.ZERO)
        assertEquals("0 ₴", totals.formatTotals())
    }
}
