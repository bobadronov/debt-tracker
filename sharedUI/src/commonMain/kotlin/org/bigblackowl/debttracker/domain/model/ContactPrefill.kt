package org.bigblackowl.debttracker.domain.model

import kotlinx.serialization.Serializable

/**
 * Дані для передзаповнення форми «Додати запис» — приходять або зі сканованої QR-картки
 * ([ScannedContact]), або з вибору раніше введеного контакту в пікері ([ContactSuggestion]).
 *
 * `@Serializable` — щоб цей стан переживав перестворення Activity у складі [org.bigblackowl.debttracker.navigation.Screen].
 */
@Serializable
data class ContactPrefill(
    val fullName: String,
    val phone: String?,
    val email: String?,
    val comment: String?,
)

/** QR-картка не несе коментаря. */
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
