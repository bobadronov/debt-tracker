package org.bigblackowl.debttracker.domain.usecase.debtor

import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** Rewrites one existing lend/repay transaction (amount/method/comment/date) against a debtor. */
class UpdateDebtTransactionUseCase(private val repository: DebtorRepository) {
    suspend operator fun invoke(transaction: DebtTransaction) = repository.updateTransaction(transaction)
}
