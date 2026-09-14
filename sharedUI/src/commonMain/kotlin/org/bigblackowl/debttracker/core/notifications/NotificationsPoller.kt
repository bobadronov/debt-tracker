package org.bigblackowl.debttracker.core.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.NotificationRepository
import kotlin.time.Duration.Companion.milliseconds

private const val POLL_INTERVAL_MS = 15_000L

/**
 * Polls the `notifications` table every 15s while there is an active session (Account+Sync —
 * Local-only doesn't participate in debt mirroring at all) and shows a system notification
 * ([LocalNotifier]) for each new row. Modeled on
 * [org.bigblackowl.debttracker.data.sync.SyncCoordinator.start] — `collectLatest` on
 * [AuthRepository.isAuthenticated] stops/restarts the loop itself on sign-in/sign-out.
 * Deliberately NOT Realtime — by explicit requirement: each client platform polls once every
 * 15s itself, rather than subscribing.
 */
class NotificationsPoller(
    private val scope: CoroutineScope,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val localNotifier: LocalNotifier,
    private val appSettings: AppSettings,
) {
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun start() {
        scope.launch {
            authRepository.isAuthenticated.collectLatest { authenticated ->
                if (!authenticated) {
                    _unreadCount.value = 0
                    return@collectLatest
                }
                if (appSettings.notificationsEnabled) localNotifier.requestPermission()
                while (currentCoroutineContext().isActive) {
                    runCatching { poll() }
                    delay(POLL_INTERVAL_MS.milliseconds)
                }
            }
        }
    }

    private suspend fun poll() = coroutineScope {
        val lastSeen = appSettings.lastSeenNotificationAt?.let { runCatching { kotlin.time.Instant.parse(it) }.getOrNull() }
        // Independent requests — run in parallel instead of sequential round trips.
        val freshDeferred = async { notificationRepository.fetchSince(lastSeen) }
        val unreadDeferred = async { notificationRepository.unreadCount() }

        val fresh = freshDeferred.await()
        // appSettings.notificationsEnabled — the user's toggle (Settings → Preferences). When off:
        // the lastSeenNotificationAt cursor is still advanced (so re-enabling doesn't dump a flood
        // of missed ones), and the unread count is updated independently below — so the in-app bell stays live.
        if (fresh.isNotEmpty() && appSettings.notificationsEnabled) {
            val strings = resolveStrings(appSettings.locale)
            fresh.forEach { notification ->
                localNotifier.notify(strings.appName, notification.formatBody(strings, redactAmount = appSettings.hideAmountsInNotifications), NotificationDeepLinks.linkFor(notification))
            }
        }
        // fetchSince returns the newest first (SupabaseNotificationRepository), so the first
        // element is the freshest, with no need to rescan the list with a comparator.
        fresh.firstOrNull()?.let { appSettings.lastSeenNotificationAt = it.createdAt.toString() }
        _unreadCount.value = unreadDeferred.await()
    }
}
