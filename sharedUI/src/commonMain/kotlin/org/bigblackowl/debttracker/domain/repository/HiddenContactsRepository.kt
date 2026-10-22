package org.bigblackowl.debttracker.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Contacts the user removed from their picker list (device-local; the underlying debtors/creditors stay untouched). */
interface HiddenContactsRepository {
    /** [org.bigblackowl.debttracker.domain.model.ContactSuggestion.key]s currently hidden. */
    val keys: StateFlow<Set<String>>
    fun hide(key: String)
}
