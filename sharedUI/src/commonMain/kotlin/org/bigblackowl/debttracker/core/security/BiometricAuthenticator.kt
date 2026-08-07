package org.bigblackowl.debttracker.core.security

import androidx.compose.runtime.Composable

enum class BiometricResult { SUCCESS, FAILED, NOT_AVAILABLE, CANCELLED, ERROR }

interface BiometricAuthenticator {
    suspend fun isAvailable(): Boolean
    suspend fun authenticate(title: String, subtitle: String? = null): BiometricResult
}

/**
 * Фабрика замість expect-класу: Android потребує поточну [androidx.fragment.app.FragmentActivity]
 * (BiometricPrompt), яку можна отримати лише в Composable-контексті — тому саме @Composable
 * factory-функція є expect/actual, а не конструктор класу.
 */
@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
