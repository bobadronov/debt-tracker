package org.bigblackowl.debttracker.ui.screens.auth

/** MVI contract for [AuthScreen] — email/password sign in or sign up against Supabase Auth. */
data class AuthState(
    val email: String = "",
    val password: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface AuthIntent {
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data object ToggleMode : AuthIntent
    data object Submit : AuthIntent
}

sealed interface AuthEffect {
    data object Success : AuthEffect
}
