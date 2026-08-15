package org.bigblackowl.debttracker.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase

/**
 * All past debtors + creditors, as [ContactSuggestion]s, for name-autocomplete on the Add
 * debtor/creditor forms — a person dealt with as a creditor before is still worth suggesting
 * when now adding them as a debtor (and vice versa), so both lists are merged.
 */
class ObserveContactSuggestionsUseCase(
    private val observeDebtors: ObserveDebtorsUseCase,
    private val observeCreditors: ObserveCreditorsUseCase,
) {
    operator fun invoke(): Flow<List<ContactSuggestion>> =
        combine(observeDebtors(), observeCreditors()) { debtors, creditors ->
            val fromDebtors = debtors.map { it.debtor.toSuggestion() }
            val fromCreditors = creditors.map { it.creditor.toSuggestion() }
            (fromDebtors + fromCreditors).distinctBy { Triple(it.fullName.lowercase(), it.phone, it.email) }
        }

    private fun Debtor.toSuggestion() = ContactSuggestion(fullName, phone, email, comment)
    private fun Creditor.toSuggestion() = ContactSuggestion(fullName, phone, email, comment)
}
