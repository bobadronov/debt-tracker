package org.bigblackowl.debttracker.domain.model

import kotlinx.serialization.Serializable

/**
 * Data for prefilling the "Add record" form — comes either from a scanned QR card
 * ([ScannedContact]), or from picking a previously entered contact in the picker ([ContactSuggestion]).
 *
 * `@Serializable` — so this state survives Activity re-creation as part of [org.bigblackowl.debttracker.navigation.Screen].
 */
@Serializable
data class ContactPrefill(
    val fullName: String,
    val phone: String?,
    val email: String?,
    val comment: String?,
)

/** A QR card carries no comment. */
fun ScannedContact.toPrefill() = ContactPrefill(
    fullName = fullName,
    phone = phone,
    email = email,
    comment = null,
)

fun ContactSuggestion.toPrefill() = ContactPrefill(
    fullName = fullName,
    phone = phone,
    email = email,
    comment = comment,
)
