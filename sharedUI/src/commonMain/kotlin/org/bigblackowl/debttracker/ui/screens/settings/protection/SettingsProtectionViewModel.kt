package org.bigblackowl.debttracker.ui.screens.settings.protection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.security.BiometricResult
import org.bigblackowl.debttracker.core.settings.AppSettings

/**
 * Owns [SettingsProtectionScreen]'s async app-lock enable/disable (biometric or PIN). The switch's
 * checked state stays read/written directly off [AppSettings.protectionEnabled] in the Screen —
 * that class is deliberately Compose-reactive (`mutableStateOf`-backed) for exactly that purpose.
 */
class SettingsProtectionViewModel(
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsProtectionState())
    val state: StateFlow<SettingsProtectionState> = _state.asStateFlow()

    fun onIntent(intent: SettingsProtectionIntent) {
        when (intent) {
            is SettingsProtectionIntent.CheckBiometricHardware -> viewModelScope.launch {
                val available = intent.authenticator.isAvailable()
                _state.update { it.copy(biometricHardwareAvailable = available) }
            }

            is SettingsProtectionIntent.TogglePinProtection -> {
                _state.update { it.copy(protectionConfirmError = null) }
                appSettings.protectionEnabled = intent.enabled
            }

            is SettingsProtectionIntent.SetupPinAndEnableProtection -> {
                _state.update { it.copy(protectionConfirmError = null) }
                appSettings.setPinCode(intent.pin)
                appSettings.protectionEnabled = true
            }

            is SettingsProtectionIntent.EnableMobileProtection -> viewModelScope.launch {
                _state.update { it.copy(protectionConfirmError = null) }
                val strings = resolveStrings(appSettings.locale)
                when (intent.authenticator.authenticate(strings.authGate.biometricEnableReason)) {
                    BiometricResult.SUCCESS -> {
                        appSettings.protectionEnabled = true
                        appSettings.biometricEnabled = true
                    }
                    else -> _state.update { it.copy(protectionConfirmError = strings.settings.protectionConfirmFailed) }
                }
            }

            SettingsProtectionIntent.DisableMobileProtection -> {
                _state.update { it.copy(protectionConfirmError = null) }
                appSettings.protectionEnabled = false
                appSettings.biometricEnabled = false
            }
        }
    }
}
