package org.bigblackowl.debttracker.core.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bigblackowl.debttracker.domain.model.AppNotification

/**
 * Bridges a tap on a system notification ([LocalNotifier]) into the nav graph — the OS-level
 * counterpart of tapping a row on
 * [org.bigblackowl.debttracker.ui.screens.notifications.NotificationsScreen]. Built exactly like
 * [org.bigblackowl.debttracker.core.qr.ContactDeepLinks]: a plain conflated [MutableStateFlow] so a
 * link that arrives before Compose starts collecting (cold start straight from a notification tap)
 * isn't lost, and the collector calls [consume] right after reading it.
 *
 * Each platform's [LocalNotifier] attaches [linkFor] to the notification it posts and, when that
 * notification is tapped, feeds the link back here:
 *  - Android: a `contentIntent` `PendingIntent` carrying the link as an extra → `AppActivity`
 *  - iOS: `content.userInfo["deepLink"]` → `UNUserNotificationCenterDelegate`
 *  - Desktop: the native notification's `onActivated` callback (Nucleus `notification-common`)
 *  - Web: `Notification.onclick`
 */
object NotificationDeepLinks {
    private const val SCHEME = "debttracker://notification"

    private val _pendingLink = MutableStateFlow<String?>(null)
    val pendingLink: StateFlow<String?> = _pendingLink.asStateFlow()

    /**
     * Deep-link URI to embed in the system notification posted for [notification]. Always carries
     * `id` (so a tap can mark the row read, like tapping it in the in-app list) plus the related
     * party, if any.
     */
    fun linkFor(notification: AppNotification): String {
        val params = buildList {
            add("id=${notification.id}")
            notification.relatedDebtorId?.let { add("debtor=$it") }
            notification.relatedCreditorId?.let { add("creditor=$it") }
        }
        return "$SCHEME?${params.joinToString("&")}"
    }

    /**
     * Deep-link URI for a locally-scheduled due-date reminder ([DueReminderCoordinator]) — no
     * `notifications` table row behind it, so it carries only the party to open. Parsed by [parse]
     * into a [Route] with a `null` [Route.notificationId], exactly like a bare notification tap.
     */
    fun reminderLink(debtorId: String? = null, creditorId: String? = null): String {
        val params = buildList {
            debtorId?.let { add("debtor=$it") }
            creditorId?.let { add("creditor=$it") }
        }
        return "$SCHEME?${params.joinToString("&")}"
    }

    fun onIncomingLink(rawUri: String) {
        _pendingLink.value = rawUri
    }

    fun consume() {
        _pendingLink.value = null
    }

    /** Where a tapped notification should land — mapped to a `Screen` by the nav graph. */
    sealed interface Target {
        data class Debtor(val id: String) : Target
        data class Creditor(val id: String) : Target

        /** Neither party is set (or the link is bare) — fall back to the notifications history. */
        data object History : Target
    }

    /** A parsed tap: which row to mark read ([notificationId], if the link carried one) and where to go. */
    data class Route(val notificationId: String?, val target: Target)

    fun parse(rawUri: String): Route? {
        if (!rawUri.startsWith(SCHEME)) return null
        val params = rawUri.substringAfter('?', "")
            .split('&')
            .filter { it.isNotEmpty() }
            .associate { pair ->
                val idx = pair.indexOf('=')
                if (idx < 0) pair to "" else pair.substring(0, idx) to pair.substring(idx + 1)
            }
        val target = when {
            !params["debtor"].isNullOrEmpty() -> Target.Debtor(params.getValue("debtor"))
            !params["creditor"].isNullOrEmpty() -> Target.Creditor(params.getValue("creditor"))
            else -> Target.History
        }
        return Route(params["id"]?.takeIf { it.isNotEmpty() }, target)
    }
}
