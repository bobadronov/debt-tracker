package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.repository.HiddenContactsRepository

/** Removes a contact from the picker / name-autocomplete list without touching any debts. */
class HideContactSuggestionUseCase(private val repository: HiddenContactsRepository) {
    operator fun invoke(contact: ContactSuggestion) = repository.hide(contact.key)
}
