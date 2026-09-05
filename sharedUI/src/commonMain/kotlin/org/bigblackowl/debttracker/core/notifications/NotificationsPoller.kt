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
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.NotificationRepository
import org.bigblackowl.debttracker.core.settings.AppSettings
import kotlin.time.Duration.Companion.milliseconds

private const val POLL_INTERVAL_MS = 15_000L

/**
 * Опитує таблицю `notifications` кожні 15с, поки є активна сесія (Account+Sync — Local-only не
 * бере участі в дзеркалюванні боргів взагалі) і показує системне сповіщення ([LocalNotifier]) для
 * кожного нового рядка. За зразком [org.bigblackowl.debttracker.data.sync.SyncCoordinator.start] —
 * `collectLatest` на [AuthRepository.isAuthenticated] сам зупиняє/перезапускає цикл при вході/виході.
 * Навмисно НЕ Realtime — за явним запитом: клієнт-платформа сама опитує раз на 15с, а не підписується.
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
        // Незалежні запити — виконуються паралельно замість послідовних round-trip'ів.
        val freshDeferred = async { notificationRepository.fetchSince(lastSeen) }
        val unreadDeferred = async { notificationRepository.unreadCount() }

        val fresh = freshDeferred.await()
        // appSettings.notificationsEnabled — перемикач користувача (Settings → Preferences). Вимкнено:
        // курсор lastSeenNotificationAt усе одно рухаємо (щоб повторне ввімкнення не показало лавину
        // пропущених), а лічильник непрочитаних оновлюється незалежно нижче — тож дзвіночок у застосунку живий.
        if (fresh.isNotEmpty() && appSettings.notificationsEnabled) {
            val strings = resolveStrings(appSettings.locale)
            fresh.forEach { notification ->
                localNotifier.notify(strings.appName, notification.formatBody(strings, redactAmount = appSettings.hideAmountsInNotifications), NotificationDeepLinks.linkFor(notification))
            }
        }
        // fetchSince повертає найновіші спочатку (SupabaseNotificationRepository), тож перший
        // елемент — найсвіжіший, без потреби повторно сканувати список компаратором.
        fresh.firstOrNull()?.let { appSettings.lastSeenNotificationAt = it.createdAt.toString() }
        _unreadCount.value = unreadDeferred.await()
    }
}
