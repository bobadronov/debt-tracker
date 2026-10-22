package org.bigblackowl.debttracker.ui.screens.contacts

import org.bigblackowl.debttracker.domain.model.ContactSuggestion

/** MVI contract for [ContactPickerScreen] — the "pick a previously entered person" step before the form. */
data class ContactPickerState(
    val query: String = "",
    /** Already filtered by [query]. */
    val contacts: List<ContactSuggestion> = emptyList(),
    val hasAnyContacts: Boolean = false,
    /** Contact awaiting the "remove from list?" confirmation. */
    val pendingRemoval: ContactSuggestion? = null,
)

sealed interface ContactPickerIntent {
    data class Search(val value: String) : ContactPickerIntent
    data class RequestRemove(val contact: ContactSuggestion) : ContactPickerIntent
    data object ConfirmRemove : ContactPickerIntent
    data object DismissRemove : ContactPickerIntent
}
