package org.bigblackowl.debttracker.domain.usecase.debtor

import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** Soft-deletes one transaction from a debtor's history (syncs, re-derives the balance/status). */
class DeleteDebtTransactionUseCase(private val repository: DebtorRepository) {
    suspend operator fun invoke(transactionId: String) = repository.softDeleteTransaction(transactionId)
}
