package org.bigblackowl.debttracker.domain.usecase.creditor

import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Soft-deletes a creditor (marks [org.bigblackowl.debttracker.domain.model.Creditor.isDeleted], doesn't purge history). */
class DeleteCreditorUseCase(private val repository: CreditorRepository) {
    suspend operator fun invoke(id: String) = repository.softDeleteCreditor(id)
}
