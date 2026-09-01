package org.bigblackowl.debttracker.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val REMINDER_CHANNEL_ID = "debt_reminders"

/**
 * Fires when an `AlarmManager` due-date reminder ([AndroidReminderScheduler]) comes due — posts
 * the notification with a tap intent that re-launches the app carrying the [NotificationDeepLinks]
 * URI (same mechanism as [AndroidLocalNotifier]).
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_REMINDER_BODY) ?: return
        val deepLink = intent.getStringExtra(EXTRA_REMINDER_DEEP_LINK)
        val notifId = intent.getIntExtra(EXTRA_REMINDER_NOTIF_ID, title.hashCode())

        val manager = NotificationManagerCompat.from(context)
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL_ID, "DebtTracker", NotificationManager.IMPORTANCE_HIGH),
        )
        if (!manager.areNotificationsEnabled()) return

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        deepLink?.let { link -> contentIntent(context, link)?.let(builder::setContentIntent) }
        runCatching { manager.notify(notifId, builder.build()) }
    }

    private fun contentIntent(context: Context, deepLink: String): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.putExtra(EXTRA_NOTIFICATION_DEEP_LINK, deepLink)
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            deepLink.hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Re-arms the persisted reminder alarms after a reboot (alarms don't survive one). */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        runCatching { AndroidReminderScheduler(context.applicationContext).rescheduleFromPersisted() }
    }
}
