package org.bigblackowl.debttracker.ui.screens.creditors

import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.domain.model.ScannedContact

/** MVI contract for [AddEditCreditorScreen] — creates a new creditor. */
data class AddEditCreditorState(
    val isSaving: Boolean = false,
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val comment: String = "",
    val initialAmountText: String = "",
    val currency: Currency = Currency.UAH,
    val method: PaymentMethod = PaymentMethod.CASH,
    val cardLastDigits: String = "",
    val fullNameError: String? = null,
    val amountError: String? = null,
    /** Ненульове поки не знайдено збіг за email, не застосовано або не відхилено (§ProfileLookup autofill). */
    val profileSuggestion: ProfileSuggestion? = null,
    val suggestedAvatarUrl: String? = null,
    /** Past debtors/creditors whose name matches [fullName] as it's typed — name-autocomplete. */
    val nameSuggestions: List<ContactSuggestion> = emptyList(),
)

sealed interface AddEditCreditorIntent {
    data class FullNameChanged(val value: String) : AddEditCreditorIntent
    data class PhoneChanged(val value: String) : AddEditCreditorIntent
    data class EmailChanged(val value: String) : AddEditCreditorIntent
    data class CommentChanged(val value: String) : AddEditCreditorIntent
    data class InitialAmountChanged(val value: String) : AddEditCreditorIntent
    data class CurrencyChanged(val value: Currency) : AddEditCreditorIntent
    data class MethodChanged(val value: PaymentMethod) : AddEditCreditorIntent
    data class CardLastDigitsChanged(val value: String) : AddEditCreditorIntent
    data object ApplyProfileSuggestion : AddEditCreditorIntent
    data object DismissProfileSuggestion : AddEditCreditorIntent
    data class NameSuggestionSelected(val suggestion: ContactSuggestion) : AddEditCreditorIntent
    data class ApplyScannedContact(val contact: ScannedContact) : AddEditCreditorIntent
    data object Save : AddEditCreditorIntent
}

sealed interface AddEditCreditorEffect {
    data object Saved : AddEditCreditorEffect
    data class Error(val message: String) : AddEditCreditorEffect
}
