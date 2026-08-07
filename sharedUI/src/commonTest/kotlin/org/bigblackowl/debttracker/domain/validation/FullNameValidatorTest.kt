package org.bigblackowl.debttracker.domain.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FullNameValidatorTest {

    @Test
    fun `two cyrillic words is valid`() {
        assertTrue(isValidFullName("Олена Ковальчук"))
    }

    @Test
    fun `two latin words is valid`() {
        assertTrue(isValidFullName("John Smith"))
    }

    @Test
    fun `three words with patronymic is valid`() {
        assertTrue(isValidFullName("Олена Іванівна Ковальчук"))
    }

    @Test
    fun `single word is invalid`() {
        assertFalse(isValidFullName("Олена"))
    }

    @Test
    fun `empty string is invalid`() {
        assertFalse(isValidFullName(""))
    }

    @Test
    fun `blank string is invalid`() {
        assertFalse(isValidFullName("   "))
    }

    @Test
    fun `word containing digits is invalid`() {
        assertFalse(isValidFullName("Олена1 Ковальчук"))
    }

    @Test
    fun `word shorter than two chars is invalid`() {
        assertFalse(isValidFullName("О Ковальчук"))
    }

    @Test
    fun `extra whitespace between words is still valid`() {
        assertTrue(isValidFullName("Олена   Ковальчук"))
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertTrue(isValidFullName("  Олена Ковальчук  "))
    }
}
