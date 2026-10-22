package org.bigblackowl.debttracker.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.repository.HiddenContactsRepository
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase

/**
 * All past debtors + creditors, as [ContactSuggestion]s, for name-autocomplete on the Add
 * debtor/creditor forms — a person dealt with as a creditor before is still worth suggesting
 * when now adding them as a debtor (and vice versa), so both lists are merged. Contacts the user
 * removed from the list ([HiddenContactsRepository]) are left out.
 */
class ObserveContactSuggestionsUseCase(
    private val observeDebtors: ObserveDebtorsUseCase,
    private val observeCreditors: ObserveCreditorsUseCase,
    private val hiddenContacts: HiddenContactsRepository,
) {
    operator fun invoke(): Flow<List<ContactSuggestion>> =
        combine(observeDebtors(), observeCreditors(), hiddenContacts.keys) { debtors, creditors, hidden ->
            val fromDebtors = debtors.map { it.debtor.toSuggestion() }
            val fromCreditors = creditors.map { it.creditor.toSuggestion() }
            (fromDebtors + fromCreditors).distinctBy { it.key }
                .filterNot { it.key in hidden }
        }

    private fun Debtor.toSuggestion() = ContactSuggestion(fullName, phone, email, comment, avatarUrl)
    private fun Creditor.toSuggestion() = ContactSuggestion(fullName, phone, email, comment, avatarUrl)
}
