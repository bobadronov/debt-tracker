package org.bigblackowl.debttracker.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Web has no biometrics (spec §1) — password only (WebAuthn is a separate Phase 10 option). */
private class NoOpBiometricAuthenticator : BiometricAuthenticator {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun authenticate(title: String, subtitle: String?): BiometricResult = BiometricResult.NOT_AVAILABLE
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { NoOpBiometricAuthenticator() }
