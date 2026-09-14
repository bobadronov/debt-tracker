package org.bigblackowl.debttracker.ui.screens.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns [SettingsAboutScreen]'s on-demand update check/download/install pipeline (the desktop/manual-check path; Play's in-app update flow tracks its own state on the launcher). */
class SettingsAboutViewModel : ViewModel() {

    private val _state = MutableStateFlow(SettingsAboutState())
    val state: StateFlow<SettingsAboutState> = _state.asStateFlow()

    fun onIntent(intent: SettingsAboutIntent) {
        when (intent) {
            // Progress lives on the launcher itself (updateStatus/updateReadyToInstall StateFlows,
            // collected directly in SettingsAboutScreen) — Play's update manager is already the source
            // of truth, so there's nothing for this ViewModel to track.
            is SettingsAboutIntent.CheckForInAppUpdate -> intent.launcher.checkForUpdate()

            is SettingsAboutIntent.CheckForUpdate -> viewModelScope.launch {
                _state.update { it.copy(updateState = UpdateCheckState.Checking) }
                runCatching { intent.checker.checkForUpdate() }
                    .onSuccess { info ->
                        _state.update { it.copy(updateState = info?.let { UpdateCheckState.Available(it) } ?: UpdateCheckState.UpToDate) }
                    }
                    .onFailure {
                        _state.update { it.copy(updateState = UpdateCheckState.CheckFailed) }
                    }
            }

            is SettingsAboutIntent.DownloadUpdate -> viewModelScope.launch {
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
