package org.bigblackowl.debttracker.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactQrPayloadTest {

    @Test
    fun `round trips full name phone and email`() {
        val contact = ScannedContact(fullName = "John Smith", phone = "+380501234567", email = "john@example.com")
        val decoded = ContactQrPayload.decode(ContactQrPayload.encode(contact))
        assertEquals(contact, decoded)
    }

    @Test
    fun `round trips with null phone and email`() {
        val contact = ScannedContact(fullName = "John Smith", phone = null, email = null)
        val decoded = ContactQrPayload.decode(ContactQrPayload.encode(contact))
        assertEquals(contact, decoded)
    }

    @Test
    fun `round trips cyrillic name`() {
        val contact = ScannedContact(fullName = "Олена Ковальчук", phone = "0501234567", email = null)
        val decoded = ContactQrPayload.decode(ContactQrPayload.encode(contact))
        assertEquals(contact, decoded)
    }

    @Test
    fun `decode returns null for a random unrelated string`() {
        assertNull(ContactQrPayload.decode("https://example.com"))
    }

    @Test
    fun `decode returns null for prefixed but missing name param`() {
        assertNull(ContactQrPayload.decode("debttracker://contact?phone=123"))
    }

    @Test
    fun `decode returns null for blank name`() {
        assertNull(ContactQrPayload.decode("debttracker://contact?name="))
    }

    @Test
    fun `decode returns null for empty string`() {
        assertNull(ContactQrPayload.decode(""))
    }
}
