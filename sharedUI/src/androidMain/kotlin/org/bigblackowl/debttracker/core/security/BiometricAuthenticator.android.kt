package org.bigblackowl.debttracker.core.security

import android.annotation.SuppressLint
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wraps [BiometricPrompt]. Allows [BiometricManager.Authenticators.BIOMETRIC_STRONG] or
 * [BiometricManager.Authenticators.DEVICE_CREDENTIAL], so devices without (or with unenrolled) fingerprint/face
 * hardware fall back to the system PIN/pattern/password screen instead of being locked out.
 */
private class AndroidBiometricAuthenticator(private val activity: FragmentActivity) : BiometricAuthenticator {

    private val allowedAuthenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    override suspend fun isAvailable(): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate(title: String, subtitle: String?): BiometricResult =
        suspendCancellableCoroutine { continuation ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .apply { subtitle?.let { setSubtitle(it) } }
                // setNegativeButtonText() is mutually exclusive with DEVICE_CREDENTIAL in setAllowedAuthenticators() —
                // the system prompt supplies its own cancel/use-PIN affordance.
                .setAllowedAuthenticators(allowedAuthenticators)
                .build()

            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(BiometricResult.SUCCESS)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!continuation.isActive) return
                        val result = if (
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        ) {
                            BiometricResult.CANCELLED
                        } else {
                            // Lockout, hardware failure, no enrolled biometric mid-session, etc. —
                            // the caller ([AuthGateViewModel]) treats this as "fall back to the app PIN".
                            BiometricResult.ERROR
                        }
                        continuation.resume(result)
                    }

                    // A biometric was presented but not recognised. The system prompt STAYS OPEN and
                    // lets the user try again, so we must NOT resume here — resuming would let the app
                    // flip to a "failed" state behind a still-visible prompt and then drop the eventual
                    // success (dead continuation). Only success / error end the suspend.
                    override fun onAuthenticationFailed() = Unit
                },
            )
            prompt.authenticate(promptInfo)
        }
}

/** No-op fallback used when there is no [FragmentActivity] context, e.g. in the layout preview renderer. */
private object NoopBiometricAuthenticator : BiometricAuthenticator {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun authenticate(title: String, subtitle: String?): BiometricResult = BiometricResult.NOT_AVAILABLE
}

@SuppressLint("ContextCastToActivity")
@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val activity = LocalContext.current as? FragmentActivity
    return remember(activity) {
        activity?.let { AndroidBiometricAuthenticator(it) } ?: NoopBiometricAuthenticator
    }
}
