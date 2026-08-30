package org.bigblackowl.debttracker.domain.model

/** A past debtor/creditor offered on the contact picker / name-autocomplete on the Add record form. */
data class ContactSuggestion(
    val fullName: String,
    val phone: String?,
    val email: String?,
    val comment: String?,
    val avatarUrl: String? = null,
)
