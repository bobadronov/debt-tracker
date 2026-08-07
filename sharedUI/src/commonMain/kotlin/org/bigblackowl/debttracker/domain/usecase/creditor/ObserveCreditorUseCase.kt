package org.bigblackowl.debttracker.domain.usecase.creditor

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Streams a single [Creditor] by id, or `null` once it's (soft-)deleted. */
class ObserveCreditorUseCase(private val repository: CreditorRepository) {
    operator fun invoke(id: String): Flow<Creditor?> = repository.observeCreditor(id)
}
