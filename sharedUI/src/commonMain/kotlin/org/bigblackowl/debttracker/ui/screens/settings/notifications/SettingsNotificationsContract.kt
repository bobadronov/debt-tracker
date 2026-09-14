package org.bigblackowl.debttracker.ui.screens.settings.notifications

import org.bigblackowl.debttracker.core.notifications.NotificationPermissionRequester

data class SettingsNotificationsState(
    /** Set after the user turns notifications on but the OS permission request comes back denied — the row then points them at system settings. */
    val notificationsPermissionBlocked: Boolean = false,
)

sealed interface SettingsNotificationsIntent {
    /** Flips the user's notification switch; when turning it on, also asks the OS for permission via [requester]. */
    data class ToggleNotifications(val enabled: Boolean, val requester: NotificationPermissionRequester) : SettingsNotificationsIntent
}
