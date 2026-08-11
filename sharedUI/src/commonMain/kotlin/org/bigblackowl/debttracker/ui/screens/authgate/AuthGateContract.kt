package org.bigblackowl.debttracker.ui.screens.authgate

import org.bigblackowl.debttracker.core.security.BiometricAuthenticator

data class AuthGateState(
    val pinInput: String = "",
    val error: String? = null,
    val biometricFailed: Boolean = false,
)

sealed interface AuthGateIntent {
    data class Authenticate(val authenticator: BiometricAuthenticator) : AuthGateIntent
    data class PinChanged(val value: String) : AuthGateIntent
    data object TryUnlockWithPin : AuthGateIntent
}

sealed interface AuthGateEffect {
    data object Unlocked : AuthGateEffect
}
