package org.bigblackowl.debttracker.domain.usecase.debtor

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** Streams the transaction history for a single debtor, used by [org.bigblackowl.debttracker.ui.screens.debtors.DebtorDetailScreen]. */
class ObserveDebtorTransactionsUseCase(private val repository: DebtorRepository) {
    operator fun invoke(debtorId: String): Flow<List<DebtTransaction>> =
        repository.observeTransactions(debtorId)
}
