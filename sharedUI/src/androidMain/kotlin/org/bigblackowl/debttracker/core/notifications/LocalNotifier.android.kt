package org.bigblackowl.debttracker.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

private const val CHANNEL_ID = "debt_sync"

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DebtTracker",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override suspend fun requestPermission(): Boolean = manager.areNotificationsEnabled()

    override fun notify(title: String, body: String) {
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(nextId.getAndIncrement(), notification) }
    }
}
