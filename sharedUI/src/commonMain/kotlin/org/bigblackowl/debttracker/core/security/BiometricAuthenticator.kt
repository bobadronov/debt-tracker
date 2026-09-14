package org.bigblackowl.debttracker.core.security

import androidx.compose.runtime.Composable

enum class BiometricResult { SUCCESS, FAILED, NOT_AVAILABLE, CANCELLED, ERROR }

interface BiometricAuthenticator {
    suspend fun isAvailable(): Boolean
    suspend fun authenticate(title: String, subtitle: String? = null): BiometricResult
}

/**
 * A factory instead of an expect class: Android needs the current [androidx.fragment.app.FragmentActivity]
 * (BiometricPrompt), which can only be obtained in a Composable context — that's why the @Composable
 * factory function is the expect/actual, rather than a class constructor.
 */
@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
