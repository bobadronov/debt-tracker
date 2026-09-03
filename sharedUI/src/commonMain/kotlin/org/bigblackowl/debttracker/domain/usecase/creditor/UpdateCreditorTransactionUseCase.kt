package org.bigblackowl.debttracker.domain.usecase.creditor

import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Rewrites one existing borrow/return transaction (amount/method/comment/date) against a creditor. */
class UpdateCreditorTransactionUseCase(private val repository: CreditorRepository) {
    suspend operator fun invoke(transaction: CreditorTransaction) = repository.updateTransaction(transaction)
}
