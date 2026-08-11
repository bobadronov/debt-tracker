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
    val isEmailStepDone: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val fullNameError: String? = null,
    val confirmPasswordError: String? = null,
)

sealed interface AuthIntent {
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class ConfirmPasswordChanged(val value: String) : AuthIntent
    data class FullNameChanged(val value: String) : AuthIntent
    data class PhoneChanged(val value: String) : AuthIntent
    data class AvatarPicked(val picked: PickedImage) : AuthIntent
    data object ToggleMode : AuthIntent
    data object ContinueFromEmailStep : AuthIntent
    data object EditEmail : AuthIntent
    data object Submit : AuthIntent
}

sealed interface AuthEffect {
    data object Success : AuthEffect
}
