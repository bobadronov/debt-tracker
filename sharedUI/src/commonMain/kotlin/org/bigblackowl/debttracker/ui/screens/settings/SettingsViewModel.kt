package org.bigblackowl.debttracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.domain.usecase.ForceSignOutUseCase

/** Owns [SettingsScreen]'s only async flow — sign-out. Every other row's own screen owns its state (see SettingsProtectionViewModel etc.). */
class SettingsViewModel(
    private val forceSignOut: ForceSignOutUseCase,
) : ViewModel() {

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.SignOut -> viewModelScope.launch { forceSignOut() }
        }
    }
}
