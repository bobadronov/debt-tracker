package org.bigblackowl.debttracker.ui.screens.contacts

import kotlin.test.Test
import kotlin.test.assertEquals
import org.bigblackowl.debttracker.domain.model.ContactSuggestion

class ContactPickerFilterTest {

    private val contacts = listOf(
        ContactSuggestion("Олена Ковальчук", "+380671112233", null, null),
        ContactSuggestion("Oleg Koval", null, "oleg@example.com", null),
        ContactSuggestion("Марія", "0509998877", null, null),
    )

    @Test
    fun `blank query returns everything`() {
        assertEquals(contacts, filterContacts(contacts, "   "))
    }

    @Test
    fun `matches name case-insensitively`() {
        assertEquals(listOf("Oleg Koval"), filterContacts(contacts, "oleg").map { it.fullName })
    }

    @Test
    fun `matches phone substring`() {
        assertEquals(listOf("Олена Ковальчук"), filterContacts(contacts, "1112").map { it.fullName })
    }

    @Test
    fun `no match returns empty`() {
        assertEquals(emptyList(), filterContacts(contacts, "zzz"))
    }
}
