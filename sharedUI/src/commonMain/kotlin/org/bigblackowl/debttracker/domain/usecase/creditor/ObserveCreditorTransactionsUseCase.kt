package org.bigblackowl.debttracker.domain.usecase.creditor

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Streams the transaction history for a single creditor, used by [org.bigblackowl.debttracker.ui.screens.creditors.CreditorDetailScreen]. */
class ObserveCreditorTransactionsUseCase(private val repository: CreditorRepository) {
    operator fun invoke(creditorId: String): Flow<List<CreditorTransaction>> =
        repository.observeTransactions(creditorId)
}
