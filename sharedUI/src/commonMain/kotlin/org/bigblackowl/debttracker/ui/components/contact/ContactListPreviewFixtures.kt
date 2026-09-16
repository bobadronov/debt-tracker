package org.bigblackowl.debttracker.ui.components.contact

/** Sample data shared by [ContactListScaffold]'s and [ContactListContent]'s own previews. */
internal data class SampleContact(val id: String, val name: String, val phone: String?, val balanceText: String)

internal val sampleContacts = listOf(
    SampleContact("1", "Олена Коваль", "+380 67 123 4567", "1 200 ₴"),
    SampleContact("2", "Іван Петренко", null, "450 ₴"),
    SampleContact("3", "Марія Бондар", "+380 50 987 6543", "3 000 ₴"),
)
