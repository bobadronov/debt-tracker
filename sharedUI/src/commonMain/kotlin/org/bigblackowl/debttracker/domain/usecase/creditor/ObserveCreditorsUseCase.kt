package org.bigblackowl.debttracker.domain.usecase.creditor

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.CreditorWithBalance
import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Streams the full creditor list with each contact's computed balance, for [org.bigblackowl.debttracker.ui.screens.creditors.CreditorListScreen]. */
class ObserveCreditorsUseCase(private val repository: CreditorRepository) {
    operator fun invoke(): Flow<List<CreditorWithBalance>> = repository.observeCreditors()
}
