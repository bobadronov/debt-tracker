package org.bigblackowl.debttracker.domain.usecase.creditor

import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Records a borrow/return transaction against a [org.bigblackowl.debttracker.domain.model.Creditor]. */
class AddCreditorTransactionUseCase(private val repository: CreditorRepository) {
    suspend operator fun invoke(transaction: CreditorTransaction) = repository.addTransaction(transaction)
}
