package org.bigblackowl.debttracker.ui.screens.auth

import org.bigblackowl.debttracker.core.media.PickedImage

/** MVI contract for [AuthScreen] — email/password sign in, or sign up (+ name/phone/avatar) against Supabase Auth. */
data class AuthState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val phone: String = "",
    val avatarPicked: PickedImage? = null,
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val fullNameError: String? = null,
    val confirmPasswordError: String? = null,
    /**
     * Set after a failed sign-in so the screen can offer to switch to registration. Supabase Auth
     * deliberately returns the same `invalid_credentials` error for an unknown email and a wrong
     * password (anti-enumeration), so this can't be limited to a true "user not found" — the prompt
     * is worded to cover both.
     */
    val offerRegistration: Boolean = false,
)

sealed interface AuthIntent {
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class ConfirmPasswordChanged(val value: String) : AuthIntent
    data class FullNameChanged(val value: String) : AuthIntent
    data class PhoneChanged(val value: String) : AuthIntent
    data class AvatarPicked(val picked: PickedImage) : AuthIntent
    data object ToggleMode : AuthIntent
    /** Jump from a failed sign-in straight into registration, carrying the email/password already typed. */
    data object SwitchToSignUp : AuthIntent
    data object Submit : AuthIntent
}

sealed interface AuthEffect {
    data object Success : AuthEffect
}
