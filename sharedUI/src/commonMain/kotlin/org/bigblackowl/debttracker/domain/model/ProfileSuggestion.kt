package org.bigblackowl.debttracker.domain.model

/** Public data of a registered user found by email — for autofilling the debtor/creditor form. */
data class ProfileSuggestion(
    val displayName: String?,
    val avatarUrl: String?,
)
