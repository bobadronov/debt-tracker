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

/** Wraps [BiometricPrompt], requiring [BiometricManager.Authenticators.BIOMETRIC_STRONG]. */
private class AndroidBiometricAuthenticator(private val activity: FragmentActivity) : BiometricAuthenticator {

    override suspend fun isAvailable(): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate(title: String, subtitle: String?): BiometricResult =
        suspendCancellableCoroutine { continuation ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .apply { subtitle?.let { setSubtitle(it) } }
                .setNegativeButtonText("Скасувати")
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
                            BiometricResult.ERROR
                        }
                        continuation.resume(result)
                    }

                    override fun onAuthenticationFailed() {
                        if (continuation.isActive) continuation.resume(BiometricResult.FAILED)
                    }
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
