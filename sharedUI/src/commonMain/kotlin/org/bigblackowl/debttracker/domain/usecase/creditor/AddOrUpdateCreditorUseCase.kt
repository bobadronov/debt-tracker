package org.bigblackowl.debttracker.domain.usecase.creditor

import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Creates a new [Creditor] or persists edits to an existing one. */
class AddOrUpdateCreditorUseCase(private val repository: CreditorRepository) {
    suspend operator fun invoke(creditor: Creditor) = repository.upsertCreditor(creditor)
}
