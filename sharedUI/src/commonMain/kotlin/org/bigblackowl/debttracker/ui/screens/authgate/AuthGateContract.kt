package org.bigblackowl.debttracker.ui.screens.authgate

import org.bigblackowl.debttracker.core.security.BiometricAuthenticator

/** Which unlock affordance the screen is currently showing. */
enum class UnlockMode { BIOMETRIC, PIN }

data class AuthGateState(
    val mode: UnlockMode = UnlockMode.PIN,
    val pinInput: String = "",
    val error: String? = null,
    /** The system biometric prompt is on screen right now. */
    val biometricRunning: Boolean = false,
    /**
     * Biometry finished without success and there is no PIN to fall back to (biometric-only setup) —
     * the screen shows a prominent "try again", never a dead end.
     */
    val biometricDismissed: Boolean = false,
)

sealed interface AuthGateIntent {
    /** Sent once when the screen first appears — kicks off biometry if that's the configured method. */
    data class Started(val authenticator: BiometricAuthenticator) : AuthGateIntent
    data class RetryBiometric(val authenticator: BiometricAuthenticator) : AuthGateIntent
    data object SwitchToPin : AuthGateIntent
    data class PinChanged(val value: String) : AuthGateIntent
    data object Submit : AuthGateIntent
}

sealed interface AuthGateEffect {
    data object Unlocked : AuthGateEffect
}
