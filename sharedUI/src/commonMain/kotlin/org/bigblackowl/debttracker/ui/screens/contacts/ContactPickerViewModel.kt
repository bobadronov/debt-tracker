package org.bigblackowl.debttracker.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.usecase.ObserveContactSuggestionsUseCase

/**
 * Feeds [ContactPickerScreen] the list of previously entered debtors/creditors (via
 * [observeContactSuggestions] — merged + de-duplicated from both lists), filtered live by the
 * search query. No dedicated contacts table: the list syncs across devices simply because the
 * underlying debtors/creditors sync when signed in.
 */
class ContactPickerViewModel(
    observeContactSuggestions: ObserveContactSuggestionsUseCase,
) : ViewModel() {

    private var allContacts: List<ContactSuggestion> = emptyList()

    private val _state = MutableStateFlow(ContactPickerState())
    val state: StateFlow<ContactPickerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeContactSuggestions().collect { contacts ->
                allContacts = contacts
                _state.update {
                    it.copy(
                        contacts = filterContacts(contacts, it.query),
                        hasAnyContacts = contacts.isNotEmpty(),
                    )
                }
            }
        }
    }

    fun onIntent(intent: ContactPickerIntent) {
        when (intent) {
            is ContactPickerIntent.Search -> _state.update {
                it.copy(query = intent.value, contacts = filterContacts(allContacts, intent.value))
            }
        }
    }
}

/** Case-insensitive match on name or phone; a blank query returns the full list. */
fun filterContacts(contacts: List<ContactSuggestion>, query: String): List<ContactSuggestion> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return contacts
    return contacts.filter {
        it.fullName.contains(trimmed, ignoreCase = true) ||
            it.phone?.contains(trimmed, ignoreCase = true) == true
    }
}
