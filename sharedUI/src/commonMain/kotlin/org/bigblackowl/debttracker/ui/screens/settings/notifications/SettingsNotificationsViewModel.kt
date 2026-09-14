package org.bigblackowl.debttracker.ui.screens.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.notifications.NotificationPermissionStatus
import org.bigblackowl.debttracker.core.settings.AppSettings

/** Owns [SettingsNotificationsScreen]'s OS permission request on enable. The switch's checked state stays read/written directly off [AppSettings.notificationsEnabled] in the Screen. */
class SettingsNotificationsViewModel(
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsNotificationsState())
    val state: StateFlow<SettingsNotificationsState> = _state.asStateFlow()

    fun onIntent(intent: SettingsNotificationsIntent) {
        when (intent) {
            is SettingsNotificationsIntent.ToggleNotifications -> {
                appSettings.notificationsEnabled = intent.enabled
                if (!intent.enabled) {
                    _state.update { it.copy(notificationsPermissionBlocked = false) }
                } else viewModelScope.launch {
                    val granted = intent.requester.request() == NotificationPermissionStatus.GRANTED
                    _state.update { it.copy(notificationsPermissionBlocked = !granted) }
                }
            }
        }
    }
}
