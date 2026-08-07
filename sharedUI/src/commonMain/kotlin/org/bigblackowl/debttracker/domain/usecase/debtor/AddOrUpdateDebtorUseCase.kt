package org.bigblackowl.debttracker.domain.usecase.debtor

import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** Creates a new [Debtor] or persists edits to an existing one. */
class AddOrUpdateDebtorUseCase(private val repository: DebtorRepository) {
    suspend operator fun invoke(debtor: Debtor) = repository.upsertDebtor(debtor)
}
