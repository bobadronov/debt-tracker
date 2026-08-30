package org.bigblackowl.debttracker.ui.screens.contacts

import org.bigblackowl.debttracker.domain.model.ContactSuggestion

/** MVI contract for [ContactPickerScreen] — the "pick a previously entered person" step before the form. */
data class ContactPickerState(
    val query: String = "",
    /** Already filtered by [query]. */
    val contacts: List<ContactSuggestion> = emptyList(),
    val hasAnyContacts: Boolean = false,
)

sealed interface ContactPickerIntent {
    data class Search(val value: String) : ContactPickerIntent
}
