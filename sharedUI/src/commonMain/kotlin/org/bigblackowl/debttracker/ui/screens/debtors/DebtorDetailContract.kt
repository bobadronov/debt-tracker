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
    data class Repay(val amount: BigDecimal, val method: PaymentMethod, val cardLastDigits: String?) : DebtorDetailIntent
    data class LendMore(val amount: BigDecimal, val method: PaymentMethod, val cardLastDigits: String?) : DebtorDetailIntent
    data object Refresh : DebtorDetailIntent
}

sealed interface DebtorDetailEffect {
    data class Error(val message: String) : DebtorDetailEffect
}
