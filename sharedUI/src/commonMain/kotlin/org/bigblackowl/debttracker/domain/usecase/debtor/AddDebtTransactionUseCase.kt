package org.bigblackowl.debttracker.domain.usecase.debtor

import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** Records a lend/repay transaction against a [org.bigblackowl.debttracker.domain.model.Debtor]. */
class AddDebtTransactionUseCase(private val repository: DebtorRepository) {
    suspend operator fun invoke(transaction: DebtTransaction) = repository.addTransaction(transaction)
}
