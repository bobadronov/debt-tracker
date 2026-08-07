package org.bigblackowl.debttracker.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountInputFilterTest {

    @Test
    fun `pure digits pass through unchanged`() {
        assertEquals("1234", sanitizeAmountInput("1234"))
    }

    @Test
    fun `letters mixed with digits are stripped`() {
        assertEquals("1234", sanitizeAmountInput("12abc34"))
    }

    @Test
    fun `letters only yields empty string`() {
        assertEquals("", sanitizeAmountInput("abc"))
    }

    @Test
    fun `empty input yields empty string`() {
        assertEquals("", sanitizeAmountInput(""))
    }

    @Test
    fun `comma is normalized to dot`() {
        assertEquals("12.50", sanitizeAmountInput("12,50"))
    }

    @Test
    fun `dot separator is kept`() {
        assertEquals("12.50", sanitizeAmountInput("12.50"))
    }

    @Test
    fun `second separator is dropped`() {
        assertEquals("12.3456", sanitizeAmountInput("12.34.56"))
    }

    @Test
    fun `second comma separator is dropped`() {
        assertEquals("12.3456", sanitizeAmountInput("12,34,56"))
    }

    @Test
    fun `mixed dot then comma keeps only first as separator`() {
        assertEquals("12.3456", sanitizeAmountInput("12.34,56"))
    }

    @Test
    fun `letters interleaved with separator are stripped`() {
        assertEquals("1.234", sanitizeAmountInput("1a.b2c3d4"))
    }
}
