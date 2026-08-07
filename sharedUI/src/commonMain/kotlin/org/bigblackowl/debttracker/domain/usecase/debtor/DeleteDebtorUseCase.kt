package org.bigblackowl.debttracker.domain.usecase.debtor

import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** Soft-deletes a debtor (marks [org.bigblackowl.debttracker.domain.model.Debtor.isDeleted], doesn't purge history). */
class DeleteDebtorUseCase(private val repository: DebtorRepository) {
    suspend operator fun invoke(id: String) = repository.softDeleteDebtor(id)
}
