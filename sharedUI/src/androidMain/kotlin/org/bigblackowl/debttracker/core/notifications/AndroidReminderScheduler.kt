package org.bigblackowl.debttracker.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

internal const val REMINDER_PREFS = "debt_reminder_schedule"
internal const val REMINDER_PREFS_KEY = "reminders"

internal const val EXTRA_REMINDER_TITLE = "org.bigblackowl.debttracker.extra.REMINDER_TITLE"
internal const val EXTRA_REMINDER_BODY = "org.bigblackowl.debttracker.extra.REMINDER_BODY"
internal const val EXTRA_REMINDER_DEEP_LINK = "org.bigblackowl.debttracker.extra.REMINDER_DEEP_LINK"
internal const val EXTRA_REMINDER_NOTIF_ID = "org.bigblackowl.debttracker.extra.REMINDER_NOTIF_ID"

internal val reminderJson = Json { ignoreUnknownKeys = true }

/**
 * Android [ReminderScheduler]: one `AlarmManager` exact alarm per pending [ScheduledReminder],
 * delivered to [ReminderAlarmReceiver] which posts the notification. The full pending set is
 * mirrored into [REMINDER_PREFS] so [sync] can cancel alarms that are no longer wanted and
 * [ReminderBootReceiver] can re-arm everything after a reboot (alarms don't survive one).
 */
internal class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)

    override fun sync(reminders: List<ScheduledReminder>) {
        val am = alarmManager ?: return
        val now = System.currentTimeMillis()
        val wanted = reminders.filter { it.atEpochMillis > now }.associateBy { it.key }
        val previous = readPersisted().associateBy { it.key }

        (previous.keys - wanted.keys).forEach { key -> am.cancel(alarmPendingIntent(key, previous.getValue(key))) }
        wanted.values.forEach { reminder -> armAlarm(am, reminder) }

        writePersisted(wanted.values.toList())
    }

    /** Called by [ReminderBootReceiver] — alarms are wiped on reboot, so re-arm the persisted set. */
    fun rescheduleFromPersisted() {
        val am = alarmManager ?: return
        val now = System.currentTimeMillis()
        val survivors = readPersisted().filter { it.atEpochMillis > now }
        survivors.forEach { armAlarm(am, it) }
        writePersisted(survivors)
    }

    private fun armAlarm(am: AlarmManager, reminder: ScheduledReminder) {
        val pi = alarmPendingIntent(reminder.key, reminder)
        runCatching {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.atEpochMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.atEpochMillis, pi)
            }
        }.onFailure { Napier.w(it) { "Failed to arm reminder alarm ${reminder.key}" } }
    }

    private fun alarmPendingIntent(key: String, reminder: ScheduledReminder): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "org.bigblackowl.debttracker.REMINDER_$key"
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_REMINDER_BODY, reminder.body)
            putExtra(EXTRA_REMINDER_DEEP_LINK, reminder.deepLink)
            putExtra(EXTRA_REMINDER_NOTIF_ID, key.hashCode())
        }
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun readPersisted(): List<ScheduledReminder> {
        val raw = prefs.getString(REMINDER_PREFS_KEY, null) ?: return emptyList()
        return runCatching { reminderJson.decodeFromString<List<ScheduledReminder>>(raw) }.getOrDefault(emptyList())
    }

    private fun writePersisted(reminders: List<ScheduledReminder>) {
        prefs.edit { putString(REMINDER_PREFS_KEY, reminderJson.encodeToString(reminders)) }
    }
}
