package org.bigblackowl.debttracker.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneInputFilterTest {

    @Test
    fun `bare national number passes through unchanged`() {
        assertEquals("0501234567", sanitizePhoneInput("0501234567"))
    }

    @Test
    fun `plus-prefixed international format is normalized to national format`() {
        assertEquals("0501234567", sanitizePhoneInput("+380501234567"))
    }

    @Test
    fun `international format without the leading plus is normalized too`() {
        assertEquals("0501234567", sanitizePhoneInput("380501234567"))
    }

    @Test
    fun `formatting punctuation and spaces are stripped`() {
        assertEquals("0501234567", sanitizePhoneInput("+38 (050) 123 45 67"))
    }

    @Test
    fun `letters mixed with digits are stripped`() {
        assertEquals("0501234567", sanitizePhoneInput("tel:0501234567"))
    }

    @Test
    fun `empty input yields empty string`() {
        assertEquals("", sanitizePhoneInput(""))
    }

    @Test
    fun `national number that happens to start with 380 is left alone`() {
        // 10 digits already — not long enough to be an international-prefixed number, so no "380" stripping kicks in.
        assertEquals("3801234567", sanitizePhoneInput("3801234567"))
    }
}
