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
import org.bigblackowl.debttracker.core.security.BiometricAuthenticator
import org.bigblackowl.debttracker.core.security.BiometricResult
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.ui.components.PIN_LENGTH

/**
 * PIN/biometric unlock check (спек §6, п.2) — verification lives here, [AuthGateScreen] only renders state.
 *
 * Failure handling is deliberate: a biometric that errors / is cancelled / is no longer available
 * must NEVER strand the user. If a PIN is set we drop straight to the PIN keypad; otherwise we keep
 * showing "try biometry again". Biometry is only ever launched on an explicit intent
 * ([AuthGateIntent.Started] once, or [AuthGateIntent.RetryBiometric]) — never re-fired by recomposition.
 */
class AuthGateViewModel(private val settings: AppSettings) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthGateState(mode = if (settings.biometricEnabled) UnlockMode.BIOMETRIC else UnlockMode.PIN)
    )
    val state: StateFlow<AuthGateState> = _state.asStateFlow()

    private val effectsChannel = Channel<AuthGateEffect>()
    val effects = effectsChannel.receiveAsFlow()

    private var started = false

    fun onIntent(intent: AuthGateIntent) {
        when (intent) {
            is AuthGateIntent.Started -> {
                if (started) return
                started = true
                if (settings.biometricEnabled) runBiometric(intent.authenticator)
                else _state.update { it.copy(mode = UnlockMode.PIN) }
            }

            is AuthGateIntent.RetryBiometric -> runBiometric(intent.authenticator)

            AuthGateIntent.SwitchToPin ->
                _state.update { it.copy(mode = UnlockMode.PIN, error = null, biometricDismissed = false) }

            is AuthGateIntent.PinChanged -> {
                _state.update { it.copy(pinInput = intent.value, error = null) }
                // Google-style OTP/PIN entry: submit the moment the PIN is complete, no extra tap needed.
                if (intent.value.length == PIN_LENGTH) tryUnlockWithPin()
            }

            AuthGateIntent.Submit -> tryUnlockWithPin()
        }
    }

    private fun runBiometric(authenticator: BiometricAuthenticator) {
        viewModelScope.launch {
            _state.update {
                it.copy(mode = UnlockMode.BIOMETRIC, biometricRunning = true, biometricDismissed = false)
            }
            val strings = resolveStrings(settings.locale)
            val result = authenticator.authenticate(strings.authGate.title, strings.authGate.biometricUnlockReason)
            _state.update { it.copy(biometricRunning = false) }
            when {
                result == BiometricResult.SUCCESS -> effectsChannel.send(AuthGateEffect.Unlocked)
                // No biometric hardware / not recognised / lockout / cancelled — fall back to the PIN
                // keypad if one exists, otherwise keep offering a retry.
                settings.hasPinCode -> _state.update { it.copy(mode = UnlockMode.PIN) }
                else -> _state.update { it.copy(biometricDismissed = true) }
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
            _state.update { it.copy(pinInput = "", error = resolveStrings(settings.locale).authGate.wrongPin) }
        }
    }
}
