package org.bigblackowl.debttracker.ui.screens.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.settings.AppSettings

class EditContactCardViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val initialName = appSettings.myCardName
    private val initialPhone = appSettings.myCardPhone
    private val initialEmail = appSettings.myCardEmail

    private val _state = MutableStateFlow(EditContactCardState(name = initialName, phone = initialPhone, email = initialEmail))
    val state: StateFlow<EditContactCardState> = _state.asStateFlow()

    private val effectsChannel = Channel<EditContactCardEffect>()
    val effects = effectsChannel.receiveAsFlow()

    fun onIntent(intent: EditContactCardIntent) {
        when (intent) {
            is EditContactCardIntent.NameChanged -> update(name = intent.value)
            is EditContactCardIntent.PhoneChanged -> update(phone = intent.value)
            is EditContactCardIntent.EmailChanged -> update(email = intent.value)
            EditContactCardIntent.Save -> save()
        }
    }

    private fun update(
        name: String = _state.value.name,
        phone: String = _state.value.phone,
        email: String = _state.value.email,
    ) {
        _state.update {
            it.copy(
                name = name,
                phone = phone,
                email = email,
                hasUnsavedChanges = name != initialName || phone != initialPhone || email != initialEmail,
            )
        }
    }

    // Local settings write — synchronous and can't fail, so unlike a network-backed Save this can
    // safely fire its effect (and let the caller navigate away) immediately, no isSaving needed.
    private fun save() {
        val current = _state.value
        appSettings.myCardName = current.name
        appSettings.myCardPhone = current.phone
        appSettings.myCardEmail = current.email
        _state.update { it.copy(hasUnsavedChanges = false) }
        viewModelScope.launch { effectsChannel.send(EditContactCardEffect.Saved) }
    }
}
