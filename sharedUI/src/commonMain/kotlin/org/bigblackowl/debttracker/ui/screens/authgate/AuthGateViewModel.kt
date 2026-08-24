package org.bigblackowl.debttracker.ui.screens.authgate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.security.BiometricResult
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.ui.components.PIN_LENGTH

/** PIN/biometric unlock check (спек §6, п.2) — verification lives here, [AuthGateScreen] only renders state. */
class AuthGateViewModel(private val settings: AppSettings) : ViewModel() {

    private val _state = MutableStateFlow(AuthGateState())
    val state: StateFlow<AuthGateState> = _state.asStateFlow()

    private val effectsChannel = Channel<AuthGateEffect>()
    val effects = effectsChannel.receiveAsFlow()

    fun onIntent(intent: AuthGateIntent) {
        when (intent) {
            is AuthGateIntent.PinChanged -> {
                _state.update { it.copy(pinInput = intent.value, error = null) }
                // Google-style OTP/PIN entry: submit the moment the PIN is complete, no extra tap needed.
                if (intent.value.length == PIN_LENGTH) tryUnlockWithPin()
            }

            AuthGateIntent.TryUnlockWithPin -> tryUnlockWithPin()

            is AuthGateIntent.Authenticate -> viewModelScope.launch {
                val strings = resolveStrings(settings.locale)
                val result = intent.authenticator.authenticate(strings.biometricUnlockReason)
                if (result == BiometricResult.SUCCESS) {
                    effectsChannel.send(AuthGateEffect.Unlocked)
                } else {
                    _state.update { it.copy(biometricFailed = true) }
                }
            }
        }
    }

    private fun tryUnlockWithPin() {
        val pin = _state.value.pinInput
        if (pin.isNotEmpty() && settings.verifyPinCode(pin)) {
            viewModelScope.launch { effectsChannel.send(AuthGateEffect.Unlocked) }
        } else {
            // Wrong PIN: clear the input so the user retypes from scratch rather than editing digits
            // they already know are wrong.
            _state.update { it.copy(pinInput = "", error = resolveStrings(settings.locale).authGateWrongPin) }
        }
    }
}
