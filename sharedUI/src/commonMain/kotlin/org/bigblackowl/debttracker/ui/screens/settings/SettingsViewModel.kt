package org.bigblackowl.debttracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.notifications.NotificationPermissionStatus
import org.bigblackowl.debttracker.core.security.BiometricResult
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.usecase.ClearAppCacheUseCase
import org.bigblackowl.debttracker.domain.usecase.DeleteAllDataUseCase
import org.bigblackowl.debttracker.domain.usecase.ForceSignOutUseCase

/**
 * Owns every async/business flow behind [SettingsScreen]: app-lock enable/disable (biometric or PIN),
 * sign-out, delete-all-data, and the update-check/download/install pipeline. Simple synchronous
 * toggles (sound/haptic/theme/locale) stay read/written directly off [AppSettings] in the Screen —
 * that class is deliberately Compose-reactive (`mutableStateOf`-backed) for exactly that purpose.
 */
class SettingsViewModel(
    private val appSettings: AppSettings,
    private val deleteAllData: DeleteAllDataUseCase,
    private val forceSignOut: ForceSignOutUseCase,
    private val clearAppCache: ClearAppCacheUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.CheckBiometricHardware -> viewModelScope.launch {
                val available = intent.authenticator.isAvailable()
                _state.update { it.copy(biometricHardwareAvailable = available) }
            }

            is SettingsIntent.TogglePinProtection -> {
                _state.update { it.copy(protectionConfirmError = null) }
                appSettings.protectionEnabled = intent.enabled
            }

            is SettingsIntent.ToggleNotifications -> {
                appSettings.notificationsEnabled = intent.enabled
                if (!intent.enabled) {
                    _state.update { it.copy(notificationsPermissionBlocked = false) }
                } else viewModelScope.launch {
                    val granted = intent.requester.request() == NotificationPermissionStatus.GRANTED
                    _state.update { it.copy(notificationsPermissionBlocked = !granted) }
                }
            }

            is SettingsIntent.SetupPinAndEnableProtection -> {
                _state.update { it.copy(protectionConfirmError = null) }
                appSettings.setPinCode(intent.pin)
                appSettings.protectionEnabled = true
            }

            is SettingsIntent.EnableMobileProtection -> viewModelScope.launch {
                _state.update { it.copy(protectionConfirmError = null) }
                val strings = resolveStrings(appSettings.locale)
                when (intent.authenticator.authenticate(strings.authGate.biometricEnableReason)) {
                    BiometricResult.SUCCESS -> {
                        appSettings.protectionEnabled = true
                        appSettings.biometricEnabled = true
                    }
                    else -> _state.update { it.copy(protectionConfirmError = strings.settings.protectionConfirmFailed) }
                }
            }

            SettingsIntent.DisableMobileProtection -> {
                _state.update { it.copy(protectionConfirmError = null) }
                appSettings.protectionEnabled = false
                appSettings.biometricEnabled = false
            }

            SettingsIntent.SignOut -> viewModelScope.launch { forceSignOut() }

            SettingsIntent.DeleteAllData -> viewModelScope.launch {
                _state.update { it.copy(deleteError = false) }
                deleteAllData()
                    .onSuccess { _state.update { it.copy(deleteDone = true) } }
                    .onFailure { _state.update { it.copy(deleteError = true) } }
            }

            SettingsIntent.ClearAppCache -> viewModelScope.launch {
                _state.update { it.copy(cacheCleared = false, cacheClearError = false) }
                clearAppCache()
                    .onSuccess { _state.update { it.copy(cacheCleared = true) } }
                    .onFailure { _state.update { it.copy(cacheClearError = true) } }
            }

            // Progress lives on the launcher itself (updateStatus/updateReadyToInstall StateFlows,
            // collected directly in SettingsScreen) — Play's update manager is already the source
            // of truth, so there's nothing for this ViewModel to track.
            is SettingsIntent.CheckForInAppUpdate -> intent.launcher.checkForUpdate()

            is SettingsIntent.CheckForUpdate -> viewModelScope.launch {
                _state.update { it.copy(updateState = UpdateCheckState.Checking) }
                runCatching { intent.checker.checkForUpdate() }
                    .onSuccess { info ->
                        _state.update { it.copy(updateState = info?.let { UpdateCheckState.Available(it) } ?: UpdateCheckState.UpToDate) }
                    }
                    .onFailure {
                        _state.update { it.copy(updateState = UpdateCheckState.CheckFailed) }
                    }
            }

            is SettingsIntent.DownloadUpdate -> viewModelScope.launch {
                _state.update { it.copy(updateState = UpdateCheckState.Downloading(intent.info)) }
                runCatching {
                    // Progress isn't rendered here (the trailing spinner is indeterminate either way),
                    // so we don't feed it into state — that would recompose this row on every chunk.
                    val path = intent.checker.download(intent.info) {}
                    // Doesn't return on success — the process exits once the install finishes.
                    intent.checker.installAndExit(path)
                }.onFailure {
                    _state.update { it.copy(updateState = UpdateCheckState.Failed(intent.info)) }
                }
            }
        }
    }
}
