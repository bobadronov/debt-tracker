package org.bigblackowl.debttracker.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

private const val CHANNEL_ID = "debt_sync"

/**
 * Intent extra that carries a [NotificationDeepLinks] URI from a tapped notification into
 * `AppActivity` — read there and forwarded to [NotificationDeepLinks.onIncomingLink]. An explicit
 * extra on the launcher intent (not a `data` URI) so the deep link stays internal and needs no
 * `BROWSABLE` intent-filter in the manifest.
 */
const val EXTRA_NOTIFICATION_DEEP_LINK = "org.bigblackowl.debttracker.extra.NOTIFICATION_DEEP_LINK"

/**
 * Android: `NotificationManagerCompat` з одним каналом "debt_sync" (створюється лениво, один
 * раз). [requestPermission] лише ПЕРЕВІРЯЄ поточний стан дозволу (`POST_NOTIFICATIONS`,
 * Android 13+) — сам системний діалог показує [org.bigblackowl.debttracker.androidApp.AppActivity]
 * при запуску (потрібна Activity, якої тут немає — [NotificationsPoller] працює у фоновому
 * `CoroutineScope`).
 */
internal class AndroidLocalNotifier(private val context: Context) : LocalNotifier {
    private val manager = NotificationManagerCompat.from(context)
    private val nextId = AtomicInteger(1)

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DebtTracker",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override suspend fun requestPermission(): Boolean = manager.areNotificationsEnabled()

    override fun notify(title: String, body: String, deepLink: String?) {
        if (!manager.areNotificationsEnabled()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        deepLink?.let { link -> contentIntent(link)?.let(builder::setContentIntent) }
        runCatching { manager.notify(nextId.getAndIncrement(), builder.build()) }
    }

    /**
     * Re-launches the app (`getLaunchIntentForPackage` — can't reference `AppActivity` from this
     * module) with the deep link as an extra. `launchMode="singleInstance"` means a running app
     * gets it via `onNewIntent`; a cold start reads it in `onCreate`. Distinct request codes per
     * link so PendingIntents for different targets don't overwrite each other.
     */
    private fun contentIntent(deepLink: String): PendingIntent? {
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
