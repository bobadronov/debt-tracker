package org.bigblackowl.debttracker.ui.screens.debtors

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.debtorBalance

/** MVI contract for [DebtorDetailScreen] — a debtor's profile plus their transaction history. */
data class DebtorDetailState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val debtor: Debtor? = null,
    val transactions: List<DebtTransaction> = emptyList(),
) {
    val balance: BigDecimal
        get() = transactions.debtorBalance()
}

sealed interface DebtorDetailIntent {
    data class Repay(val amount: BigDecimal, val method: PaymentMethod) : DebtorDetailIntent
    data class LendMore(val amount: BigDecimal, val method: PaymentMethod) : DebtorDetailIntent
    /** Rewrites one history row. [amount] is the unsigned magnitude — the lend/repay direction is kept from the original. */
    data class EditTransaction(
        val transactionId: String,
        val amount: BigDecimal,
        val method: PaymentMethod,
        val comment: String?,
        val date: kotlin.time.Instant,
    ) : DebtorDetailIntent
    data class DeleteTransaction(val transactionId: String) : DebtorDetailIntent
    data object Refresh : DebtorDetailIntent
}

sealed interface DebtorDetailEffect {
    data class Error(val message: String) : DebtorDetailEffect
}
