package org.bigblackowl.debttracker.ui.screens.contacts

import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtDirection
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.domain.model.ScannedContact

/**
 * MVI contract for [AddEditContactScreen] — the merged "Add record" form that creates either a
 * [org.bigblackowl.debttracker.domain.model.Debtor] or a
 * [org.bigblackowl.debttracker.domain.model.Creditor] depending on [AddEditContactState.direction].
 */
data class AddEditContactState(
    val direction: DebtDirection = DebtDirection.DEBTOR,
    val isSaving: Boolean = false,
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val comment: String = "",
    val initialAmountText: String = "",
    val currency: Currency = Currency.UAH,
    val method: PaymentMethod = PaymentMethod.CASH,
    val fullNameError: String? = null,
    val amountError: String? = null,
    /** Ненульове поки не знайдено збіг за email, не застосовано або не відхилено (§ProfileLookup autofill). */
    val profileSuggestion: ProfileSuggestion? = null,
    val suggestedAvatarUrl: String? = null,
    /** Past debtors/creditors whose name matches [fullName] as it's typed — inline name-autocomplete. */
    val nameSuggestions: List<ContactSuggestion> = emptyList(),
)

sealed interface AddEditContactIntent {
    data class DirectionChanged(val value: DebtDirection) : AddEditContactIntent
    data class FullNameChanged(val value: String) : AddEditContactIntent
    data class PhoneChanged(val value: String) : AddEditContactIntent
    data class EmailChanged(val value: String) : AddEditContactIntent
    data class CommentChanged(val value: String) : AddEditContactIntent
    data class InitialAmountChanged(val value: String) : AddEditContactIntent
    data class CurrencyChanged(val value: Currency) : AddEditContactIntent
    data class MethodChanged(val value: PaymentMethod) : AddEditContactIntent
    data object ApplyProfileSuggestion : AddEditContactIntent
    data object DismissProfileSuggestion : AddEditContactIntent
    data class NameSuggestionSelected(val suggestion: ContactSuggestion) : AddEditContactIntent
    data class ApplyScannedContact(val contact: ScannedContact) : AddEditContactIntent
    data object Save : AddEditContactIntent
}

sealed interface AddEditContactEffect {
    data object Saved : AddEditContactEffect
    data class Error(val message: String) : AddEditContactEffect
}
