package org.bigblackowl.debttracker.ui.screens.notifications

import org.bigblackowl.debttracker.domain.model.AppNotification

/** MVI contract for [NotificationsScreen] — історія сповіщень про дзеркальні борги (спек §7). */
data class NotificationsState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
)

sealed interface NotificationsIntent {
    data class Open(val notification: AppNotification) : NotificationsIntent
    data class Delete(val id: String) : NotificationsIntent
    data object MarkAllRead : NotificationsIntent
}

sealed interface NotificationsEffect {
    data class NavigateToDebtor(val debtorId: String) : NotificationsEffect
    data class NavigateToCreditor(val creditorId: String) : NotificationsEffect
}
