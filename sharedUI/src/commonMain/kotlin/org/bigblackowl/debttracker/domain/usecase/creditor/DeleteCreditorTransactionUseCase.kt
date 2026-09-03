package org.bigblackowl.debttracker.domain.usecase.creditor

import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Soft-deletes one transaction from a creditor's history (syncs, re-derives the balance/status). */
class DeleteCreditorTransactionUseCase(private val repository: CreditorRepository) {
    suspend operator fun invoke(transactionId: String) = repository.softDeleteTransaction(transactionId)
}
