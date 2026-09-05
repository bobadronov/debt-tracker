package org.bigblackowl.debttracker.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes
import org.bigblackowl.debttracker.core.auth.GoogleSignInLauncher
import org.bigblackowl.debttracker.core.auth.GoogleSignInOutcome
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.RestoreCredentialGateway
import org.bigblackowl.debttracker.domain.validation.isStrongEnoughPassword
import org.bigblackowl.debttracker.domain.validation.isValidFullName

/** Reduces [AuthIntent]s into [AuthState], delegating sign up/in to [AuthRepository]. */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val appSettings: AppSettings,
    private val restoreCredentials: RestoreCredentialGateway,
    private val googleSignIn: GoogleSignInLauncher,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val effectsChannel = Channel<AuthEffect>()
    val effects = effectsChannel.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> _state.update { it.copy(email = intent.value, error = null, offerRegistration = false) }
            is AuthIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null, passwordError = null, confirmPasswordError = null, offerRegistration = false) }
            is AuthIntent.ConfirmPasswordChanged -> _state.update { it.copy(confirmPassword = intent.value, confirmPasswordError = null) }
            is AuthIntent.FullNameChanged -> _state.update { it.copy(fullName = intent.value, fullNameError = null) }
            is AuthIntent.PhoneChanged -> _state.update { it.copy(phone = intent.value) }
            is AuthIntent.AvatarPicked -> _state.update { it.copy(avatarPicked = intent.picked) }
            AuthIntent.ToggleMode -> _state.update {
                it.copy(isSignUpMode = !it.isSignUpMode, error = null, googleError = null, fullNameError = null, passwordError = null, confirmPasswordError = null, offerRegistration = false)
            }
            AuthIntent.SwitchToSignUp -> _state.update {
                // Keep the email/password already typed — the user just needs to add a name to register.
                it.copy(isSignUpMode = true, confirmPassword = it.password, error = null, googleError = null, offerRegistration = false)
            }
            AuthIntent.Submit -> submit()
            AuthIntent.GoogleSignIn -> googleSignInFlow()
        }
    }

    private fun googleSignInFlow() {
        if (_state.value.isGoogleLoading || _state.value.isLoading) return
        // Flip the in-flight flag synchronously so a double-tap can't launch a second flow.
        _state.update { it.copy(isGoogleLoading = true, error = null, googleError = null, offerRegistration = false) }
        viewModelScope.launch {
            when (googleSignIn.signIn()) {
                GoogleSignInOutcome.Success -> {
                    // Android/Desktop have a session by now; iOS establishes it a moment later via
                    // the debttracker://login-callback deep link; web is navigating away entirely.
                    // Wait for the session to actually appear before leaving the screen.
                    val authed = withTimeoutOrNull(2.minutes) {
                        authRepository.isAuthenticated.first { it }
                    } == true
                    if (authed) {
                        // Best-effort OS restore-credential registration, same as the email path.
                        viewModelScope.launch { restoreCredentials.registerForCurrentSession() }
                        _state.update { it.copy(isGoogleLoading = false) }
                        effectsChannel.send(AuthEffect.Success)
                    } else {
                        // Browser dismissed without finishing, or web didn't navigate — drop the spinner.
                        _state.update { it.copy(isGoogleLoading = false) }
                    }
                }
                GoogleSignInOutcome.Cancelled ->
                    _state.update { it.copy(isGoogleLoading = false) }
                is GoogleSignInOutcome.Failure ->
                    _state.update {
                        it.copy(isGoogleLoading = false, googleError = resolveStrings(appSettings.locale).authError)
                    }
            }
        }
    }

    private fun submit() {
        val current = _state.value
        val strings = resolveStrings(appSettings.locale)

        if (current.isSignUpMode) {
            val fullNameError = if (!isValidFullName(current.fullName)) strings.fullNameError else null
            val passwordError = if (!isStrongEnoughPassword(current.password)) strings.authExtra.passwordTooShort else null
            val confirmPasswordError = if (current.password != current.confirmPassword) strings.authPasswordMismatch else null
            if (fullNameError != null || passwordError != null || confirmPasswordError != null) {
                _state.update { it.copy(fullNameError = fullNameError, passwordError = passwordError, confirmPasswordError = confirmPasswordError) }
                return
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, googleError = null) }
            val result = if (current.isSignUpMode) {
                authRepository.signUp(current.email.trim(), current.password)
            } else {
                authRepository.signIn(current.email.trim(), current.password)
            }
            result
                .onSuccess {
                    if (current.isSignUpMode) {
                        // Independent writes against the profile just created by signUp() above — run
                        // concurrently rather than paying for two round trips back to back.
                        coroutineScope {
                            launch { authRepository.updateProfile(current.fullName.trim(), current.phone) }
                            current.avatarPicked?.let { picked ->
                                launch { authRepository.updateAvatar(picked.bytes, picked.fileExtension) }
                            }
                        }
                    }
                    // Register an OS restore credential so this account can be restored with zero
                    // taps on the user's next device. Best-effort and no-op unless the feature is
                    // enabled — fire-and-forget so it never delays or fails the sign-in.
                    viewModelScope.launch { restoreCredentials.registerForCurrentSession() }
                    _state.update { it.copy(isLoading = false) }
                    effectsChannel.send(AuthEffect.Success)
                }
                .onFailure {
                    // On a failed sign-in, offer registration: Supabase can't tell us whether the
                    // email is unknown or the password is just wrong, so the prompt covers both.
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = resolveStrings(appSettings.locale).authError,
                            offerRegistration = !current.isSignUpMode,
                        )
                    }
                }
        }
    }
}
