package org.bigblackowl.debttracker.core.notifications

import kotlinx.serialization.Serializable

/**
 * One local notification to fire in the future about an upcoming debt repayment / payment.
 * [key] is stable across recomputations (`"debtor:<id>:<leadDays>"`), so re-syncing the same
 * reminder is a no-op rather than a duplicate. [Serializable] so the Android scheduler can
 * persist the pending set for re-arming after a reboot.
 */
@Serializable
data class ScheduledReminder(
    val key: String,
    /** Epoch millis (UTC) at which to post the notification. */
    val atEpochMillis: Long,
    val title: String,
    val body: String,
    /** [NotificationDeepLinks] URI opened when the notification is tapped. */
    val deepLink: String,
)

/**
 * Platform delivery of [ScheduledReminder]s. [sync] is authoritative: it cancels every reminder
 * the app previously scheduled and (re)schedules exactly the given list. [DueReminderCoordinator]
 * always passes the full current set recomputed from all debtors/creditors, so the scheduler
 * never has to reconcile partial diffs.
 *
 *  - Android: `AlarmManager` exact alarms + a boot receiver that re-arms them (`AndroidReminderScheduler`).
 *  - iOS: `UNUserNotificationCenter` calendar triggers (`IosReminderScheduler`).
 *  - Desktop / Web: in-process coroutine timers, only while the app runs ([InProcessReminderScheduler]).
 */
interface ReminderScheduler {
    fun sync(reminders: List<ScheduledReminder>)
}
