package org.bigblackowl.debttracker.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Web не має біометрії (спек §1) — лише пароль (WebAuthn — окрема опція Фази 10). */
private class NoOpBiometricAuthenticator : BiometricAuthenticator {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun authenticate(title: String, subtitle: String?): BiometricResult = BiometricResult.NOT_AVAILABLE
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { NoOpBiometricAuthenticator() }
