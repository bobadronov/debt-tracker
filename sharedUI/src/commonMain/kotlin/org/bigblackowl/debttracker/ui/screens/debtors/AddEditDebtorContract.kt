package org.bigblackowl.debttracker.ui.screens.debtors

import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.ProfileSuggestion

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
    val cardLastDigits: String = "",
    val fullNameError: String? = null,
    val amountError: String? = null,
    /** Ненульове поки не знайдено збіг за email, не застосовано або не відхилено (§ProfileLookup autofill). */
    val profileSuggestion: ProfileSuggestion? = null,
    val suggestedAvatarUrl: String? = null,
)

sealed interface AddEditDebtorIntent {
    data class FullNameChanged(val value: String) : AddEditDebtorIntent
    data class PhoneChanged(val value: String) : AddEditDebtorIntent
    data class EmailChanged(val value: String) : AddEditDebtorIntent
    data class CommentChanged(val value: String) : AddEditDebtorIntent
    data class InitialAmountChanged(val value: String) : AddEditDebtorIntent
    data class CurrencyChanged(val value: Currency) : AddEditDebtorIntent
    data class MethodChanged(val value: PaymentMethod) : AddEditDebtorIntent
    data class CardLastDigitsChanged(val value: String) : AddEditDebtorIntent
    data object ApplyProfileSuggestion : AddEditDebtorIntent
    data object DismissProfileSuggestion : AddEditDebtorIntent
    data object Save : AddEditDebtorIntent
}

sealed interface AddEditDebtorEffect {
    data object Saved : AddEditDebtorEffect
    data class Error(val message: String) : AddEditDebtorEffect
}
