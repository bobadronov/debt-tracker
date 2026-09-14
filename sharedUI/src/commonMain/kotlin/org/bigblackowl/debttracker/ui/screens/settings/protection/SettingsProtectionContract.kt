package org.bigblackowl.debttracker.ui.screens.settings.protection

import org.bigblackowl.debttracker.core.security.BiometricAuthenticator

data class SettingsProtectionState(
    val biometricHardwareAvailable: Boolean = false,
    val protectionConfirmError: String? = null,
)

sealed interface SettingsProtectionIntent {
    data class CheckBiometricHardware(val authenticator: BiometricAuthenticator) : SettingsProtectionIntent
    /** Toggles protection when it's PIN-backed — Desktop (no biometric at all) or mobile with no biometric hardware/enrollment. */
    data class TogglePinProtection(val enabled: Boolean) : SettingsProtectionIntent
    data class SetupPinAndEnableProtection(val pin: String) : SettingsProtectionIntent
    data class EnableMobileProtection(val authenticator: BiometricAuthenticator) : SettingsProtectionIntent
    data object DisableMobileProtection : SettingsProtectionIntent
}
