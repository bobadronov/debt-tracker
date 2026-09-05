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
    /** [org.bigblackowl.debttracker.domain.model.NotificationType.LINK_REQUEST] row actions (0013, B3-фікс). */
    data class ApproveLinkRequest(val notificationId: String, val requestId: String) : NotificationsIntent
    data class RejectLinkRequest(val notificationId: String, val requestId: String) : NotificationsIntent
}

sealed interface NotificationsEffect {
    data class NavigateToDebtor(val debtorId: String) : NotificationsEffect
    data class NavigateToCreditor(val creditorId: String) : NotificationsEffect
}
