package org.bigblackowl.debttracker.ui.screens.qr

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.bigblackowl.debttracker.core.settings.AppSettings

class EditContactCardViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _state = MutableStateFlow(
        EditContactCardState(
            name = appSettings.myCardName,
            phone = appSettings.myCardPhone,
            email = appSettings.myCardEmail,
        )
    )
    val state: StateFlow<EditContactCardState> = _state.asStateFlow()

    fun onIntent(intent: EditContactCardIntent) {
        when (intent) {
            is EditContactCardIntent.NameChanged -> update(name = intent.value)
            is EditContactCardIntent.PhoneChanged -> update(phone = intent.value)
            is EditContactCardIntent.EmailChanged -> update(email = intent.value)
        }
    }

    // AppSettings.myCard* is Compose-reactive (mutableStateOf-backed) — QrHubViewModel picks this
    // up via snapshotFlow the moment it's written here, no explicit "save and notify" needed.
    private fun update(
        name: String = _state.value.name,
        phone: String = _state.value.phone,
        email: String = _state.value.email,
    ) {
        appSettings.myCardName = name
        appSettings.myCardPhone = phone
        appSettings.myCardEmail = email
        _state.update { it.copy(name = name, phone = phone, email = email) }
    }
}
