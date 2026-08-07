package org.bigblackowl.debttracker.domain.usecase.debtor

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** Streams a single [Debtor] by id, or `null` once it's (soft-)deleted. */
class ObserveDebtorUseCase(private val repository: DebtorRepository) {
    operator fun invoke(id: String): Flow<Debtor?> = repository.observeDebtor(id)
}
