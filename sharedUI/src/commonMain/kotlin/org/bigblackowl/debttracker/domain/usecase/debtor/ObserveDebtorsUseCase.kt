package org.bigblackowl.debttracker.domain.usecase.debtor

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** Streams the full debtor list with each contact's computed balance, for [org.bigblackowl.debttracker.ui.screens.debtors.DebtorListScreen]. */
class ObserveDebtorsUseCase(private val repository: DebtorRepository) {
    operator fun invoke(): Flow<List<DebtorWithBalance>> = repository.observeDebtors()
}
