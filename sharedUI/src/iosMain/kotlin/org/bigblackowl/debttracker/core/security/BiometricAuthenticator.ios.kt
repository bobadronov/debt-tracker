package org.bigblackowl.debttracker.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

/** Wraps `LAContext`, requiring `LAPolicyDeviceOwnerAuthenticationWithBiometrics` (Face ID/Touch ID). */
private class IosBiometricAuthenticator : BiometricAuthenticator {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun isAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun authenticate(title: String, subtitle: String?): BiometricResult =
        suspendCancellableCoroutine { continuation ->
            val context = LAContext()
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = subtitle ?: title,
            ) { success, _ ->
                if (continuation.isActive) {
                    continuation.resume(if (success) BiometricResult.SUCCESS else BiometricResult.FAILED)
                }
            }
        }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { IosBiometricAuthenticator() }
