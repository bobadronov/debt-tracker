package org.bigblackowl.debttracker.domain.model

/** A past debtor/creditor offered as a name-autocomplete suggestion on the Add debtor/creditor forms. */
data class ContactSuggestion(
    val fullName: String,
    val phone: String?,
    val email: String?,
    val comment: String?,
)
