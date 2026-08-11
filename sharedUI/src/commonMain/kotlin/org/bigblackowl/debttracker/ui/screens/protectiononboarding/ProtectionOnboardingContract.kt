package org.bigblackowl.debttracker.ui.screens.protectiononboarding

import org.bigblackowl.debttracker.core.security.BiometricAuthenticator

data class ProtectionOnboardingState(
    val biometricAvailable: Boolean = false,
    val error: String? = null,
)

sealed interface ProtectionOnboardingIntent {
    data class CheckBiometricAvailability(val authenticator: BiometricAuthenticator) : ProtectionOnboardingIntent
    data class EnableBiometric(val authenticator: BiometricAuthenticator) : ProtectionOnboardingIntent
    data class EnablePin(val pin: String) : ProtectionOnboardingIntent
    data object Skip : ProtectionOnboardingIntent
}

sealed interface ProtectionOnboardingEffect {
    data object Done : ProtectionOnboardingEffect
}
