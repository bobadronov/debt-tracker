package org.bigblackowl.debttracker.ui.screens.protectiononboarding

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

/** Drives the first-launch app-lock onboarding: checks biometric hardware, then enables PIN or biometric protection. */
class ProtectionOnboardingViewModel(private val settings: AppSettings) : ViewModel() {

    private val _state = MutableStateFlow(ProtectionOnboardingState())
    val state: StateFlow<ProtectionOnboardingState> = _state.asStateFlow()

    private val effectsChannel = Channel<ProtectionOnboardingEffect>()
    val effects = effectsChannel.receiveAsFlow()

    fun onIntent(intent: ProtectionOnboardingIntent) {
        when (intent) {
            is ProtectionOnboardingIntent.CheckBiometricAvailability -> viewModelScope.launch {
                val available = intent.authenticator.isAvailable()
                _state.update { it.copy(biometricAvailable = available) }
            }

            is ProtectionOnboardingIntent.EnableBiometric -> viewModelScope.launch {
                val strings = resolveStrings(settings.locale)
                when (intent.authenticator.authenticate(strings.biometricEnableReason)) {
                    BiometricResult.SUCCESS -> {
                        settings.protectionEnabled = true
                        settings.biometricEnabled = true
                        finish()
                    }
                    else -> _state.update { it.copy(error = strings.onboardingProtectionConfirmFailed) }
                }
            }

            is ProtectionOnboardingIntent.EnablePin -> {
                settings.setPinCode(intent.pin)
                settings.protectionEnabled = true
                finish()
            }

            ProtectionOnboardingIntent.Skip -> finish()
        }
    }

    private fun finish() {
        settings.hasSeenProtectionOnboarding = true
        viewModelScope.launch { effectsChannel.send(ProtectionOnboardingEffect.Done) }
    }
}
