package org.bigblackowl.debttracker.ui.screens.settings.account

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
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput

/** Seeds the form from [AuthRepository]'s current profile snapshot and saves edits back via [AuthRepository.updateProfile]. */
class EditAccountViewModel(
    private val authRepository: AuthRepository,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val initialFullName = authRepository.displayName.value.orEmpty()
    private val initialPhone = sanitizePhoneInput(authRepository.phone.value.orEmpty())

    private val _state = MutableStateFlow(
        EditAccountState(
            email = authRepository.email.value.orEmpty(),
            fullName = initialFullName,
            phone = initialPhone,
            avatarUrl = authRepository.avatarUrl.value,
        )
    )
    val state: StateFlow<EditAccountState> = _state.asStateFlow()

    private val effectsChannel = Channel<EditAccountEffect>()
    val effects = effectsChannel.receiveAsFlow()

    fun onIntent(intent: EditAccountIntent) {
        when (intent) {
            is EditAccountIntent.FullNameChanged -> update(fullName = intent.value)
            is EditAccountIntent.PhoneChanged -> update(phone = intent.value)
            is EditAccountIntent.AvatarPicked -> uploadAvatar(intent.picked.bytes, intent.picked.fileExtension)
            EditAccountIntent.Save -> save()
        }
    }

    // Avatar upload is its own instant action (not gated by Save), so it's deliberately excluded
    // from this dirty check — only fullName/phone are what a pending Save would actually write.
    private fun update(
        fullName: String = _state.value.fullName,
        phone: String = _state.value.phone,
    ) {
        _state.update {
            it.copy(
                fullName = fullName,
                fullNameError = null,
                phone = phone,
                hasUnsavedChanges = fullName != initialFullName || phone != initialPhone,
            )
        }
    }

    private fun uploadAvatar(bytes: ByteArray, fileExtension: String) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, avatarError = null) }
            authRepository.updateAvatar(bytes, fileExtension)
                .onSuccess { url -> _state.update { it.copy(isUploadingAvatar = false, avatarUrl = url) } }
                .onFailure {
                    val strings = resolveStrings(appSettings.locale)
                    _state.update { it.copy(isUploadingAvatar = false, avatarError = strings.settings.avatarUploadError) }
                }
        }
    }

    private fun save() {
        val current = _state.value
        val strings = resolveStrings(appSettings.locale)

        if (!isValidFullName(current.fullName)) {
            _state.update { it.copy(fullNameError = strings.fullNameError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            authRepository.updateProfile(current.fullName.trim(), current.phone)
                .onSuccess {
                    _state.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    effectsChannel.send(EditAccountEffect.Saved)
                }
                .onFailure {
                    _state.update { it.copy(isSaving = false, error = strings.saveError) }
                }
        }
    }
}
