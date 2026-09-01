@file:OptIn(ExperimentalForeignApi::class)

package org.bigblackowl.debttracker.core.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock

// Must match the key [NotificationClickDelegate] (LocalNotifier.ios.kt) reads from `userInfo`.
private const val USER_INFO_DEEP_LINK = "deepLink"

/**
 * iOS [ReminderScheduler]: one `UNTimeIntervalNotificationTrigger` per pending [ScheduledReminder].
 * `removeAllPendingNotificationRequests` is safe here because [IosLocalNotifier] only ever posts
 * immediate (`trigger = null`) notifications — the pending queue is exclusively ours. Taps route
 * through the delegate that [IosLocalNotifier] installs on the shared center.
 */
internal class IosReminderScheduler : ReminderScheduler {

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun sync(reminders: List<ScheduledReminder>) {
        center.removeAllPendingNotificationRequests()
        val now = Clock.System.now().toEpochMilliseconds()
        reminders.filter { it.atEpochMillis > now }.forEach { reminder ->
            val content = UNMutableNotificationContent().apply {
                setTitle(reminder.title)
                setBody(reminder.body)
                setUserInfo(mapOf<Any?, Any?>(USER_INFO_DEEP_LINK to reminder.deepLink))
            }
            val seconds = (reminder.atEpochMillis - now).toDouble() / 1000.0
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
            val request = UNNotificationRequest.requestWithIdentifier(reminder.key, content, trigger)
            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }
}
