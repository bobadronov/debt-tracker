package org.bigblackowl.debttracker.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneInputFilterTest {

    @Test
    fun `bare national number passes through unchanged`() {
        assertEquals("501234567", sanitizePhoneInput("501234567"))
    }

    @Test
    fun `domestic trunk-zero notation drops the leading zero`() {
        assertEquals("501234567", sanitizePhoneInput("0501234567"))
    }

    @Test
    fun `plus-prefixed international format is normalized`() {
        assertEquals("501234567", sanitizePhoneInput("+380501234567"))
    }

    @Test
    fun `international format without the leading plus is normalized too`() {
        assertEquals("501234567", sanitizePhoneInput("380501234567"))
    }

    @Test
    fun `double-zero international prefix is normalized`() {
        assertEquals("501234567", sanitizePhoneInput("00380501234567"))
    }

    @Test
    fun `formatting punctuation and spaces are stripped`() {
        assertEquals("501234567", sanitizePhoneInput("+380 (50) 123 45 67"))
        assertEquals("501234567", sanitizePhoneInput("+38 (050) 123 45 67"))
    }

    @Test
    fun `letters mixed with digits are stripped`() {
        assertEquals("501234567", sanitizePhoneInput("tel:0501234567"))
    }

    @Test
    fun `empty input yields empty string`() {
        assertEquals("", sanitizePhoneInput(""))
    }

    @Test
    fun `overlong input is capped at nine digits`() {
        assertEquals("501234567", sanitizePhoneInput("5012345679999"))
    }

    @Test
    fun `partial input while typing keeps its digits`() {
        assertEquals("50", sanitizePhoneInput("050"))
        assertEquals("5", sanitizePhoneInput("05"))
    }

    @Test
    fun `format renders the canonical grouping`() {
        assertEquals("+380 50 123 45 67", formatUkrainianPhone("501234567"))
    }

    @Test
    fun `format tolerates legacy and international stored forms`() {
        assertEquals("+380 50 123 45 67", formatUkrainianPhone("0501234567"))
        assertEquals("+380 50 123 45 67", formatUkrainianPhone("+380501234567"))
    }

    @Test
    fun `format shows a partial number best-effort`() {
        assertEquals("+380 50 12", formatUkrainianPhone("5012"))
    }

    @Test
    fun `format returns null for null or blank`() {
        assertNull(formatUkrainianPhone(null))
        assertNull(formatUkrainianPhone(""))
        assertNull(formatUkrainianPhone("   "))
    }
}
