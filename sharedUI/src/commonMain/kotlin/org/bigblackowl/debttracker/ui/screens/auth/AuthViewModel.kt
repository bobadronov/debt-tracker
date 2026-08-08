package org.bigblackowl.debttracker.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.AuthRepository

/** Reduces [AuthIntent]s into [AuthState], delegating sign up/in to [AuthRepository]. */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val effectsChannel = Channel<AuthEffect>()
    val effects = effectsChannel.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> _state.update { it.copy(email = intent.value, error = null) }
            is AuthIntent.PasswordChanged -> _state.update { it.copy(password = intent.value, error = null) }
            AuthIntent.ToggleMode -> _state.update { it.copy(isSignUpMode = !it.isSignUpMode, error = null) }
            AuthIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = if (current.isSignUpMode) {
                authRepository.signUp(current.email.trim(), current.password)
            } else {
                authRepository.signIn(current.email.trim(), current.password)
            }
            result
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    effectsChannel.send(AuthEffect.Success)
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = resolveStrings(appSettings.locale).authError) }
                }
        }
    }
}
