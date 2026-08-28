package org.bigblackowl.debttracker.ui.screens.debtors

import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.domain.model.ScannedContact

/** MVI contract for [AddEditDebtorScreen] — creates a new debtor. */
data class AddEditDebtorState(
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
    /** Past debtors/creditors whose name matches [fullName] as it's typed — name-autocomplete. */
    val nameSuggestions: List<ContactSuggestion> = emptyList(),
)

sealed interface AddEditDebtorIntent {
    data class FullNameChanged(val value: String) : AddEditDebtorIntent
    data class PhoneChanged(val value: String) : AddEditDebtorIntent
    data class EmailChanged(val value: String) : AddEditDebtorIntent
    data class CommentChanged(val value: String) : AddEditDebtorIntent
    data class InitialAmountChanged(val value: String) : AddEditDebtorIntent
    data class CurrencyChanged(val value: Currency) : AddEditDebtorIntent
    data class MethodChanged(val value: PaymentMethod) : AddEditDebtorIntent
    data object ApplyProfileSuggestion : AddEditDebtorIntent
    data object DismissProfileSuggestion : AddEditDebtorIntent
    data class NameSuggestionSelected(val suggestion: ContactSuggestion) : AddEditDebtorIntent
    data class ApplyScannedContact(val contact: ScannedContact) : AddEditDebtorIntent
    data object Save : AddEditDebtorIntent
}

sealed interface AddEditDebtorEffect {
    data object Saved : AddEditDebtorEffect
    data class Error(val message: String) : AddEditDebtorEffect
}
