package org.bigblackowl.debttracker.ui.screens.creditors

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.creditorBalance

/** MVI contract for [CreditorDetailScreen] — a creditor's profile plus their transaction history. */
data class CreditorDetailState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val creditor: Creditor? = null,
    val transactions: List<CreditorTransaction> = emptyList(),
) {
    val balance: BigDecimal
        get() = transactions.creditorBalance()
}

sealed interface CreditorDetailIntent {
    data class Return(val amount: BigDecimal, val method: PaymentMethod) : CreditorDetailIntent
    data class BorrowMore(val amount: BigDecimal, val method: PaymentMethod) : CreditorDetailIntent
    /** Rewrites one history row. [amount] is the unsigned magnitude — the borrow/return direction is kept from the original. */
    data class EditTransaction(
        val transactionId: String,
        val amount: BigDecimal,
        val method: PaymentMethod,
        val comment: String?,
        val date: kotlin.time.Instant,
    ) : CreditorDetailIntent
    data class DeleteTransaction(val transactionId: String) : CreditorDetailIntent
    data object Refresh : CreditorDetailIntent
}

sealed interface CreditorDetailEffect {
    data class Error(val message: String) : CreditorDetailEffect
}
