package org.bigblackowl.debttracker.core.notifications

import dev.nucleusframework.notification.common.NotificationManager
import dev.nucleusframework.notification.common.NotificationResult
import dev.nucleusframework.notification.common.notification
import io.github.aakira.napier.Napier

/**
 * Set by the desktop app's `main()` so a clicked notification can bring the (possibly
 * hidden — Settings → "Run in background") window back to the front. `null` on every other
 * platform / before the window exists.
 */
object DesktopNotificationWindow {
    @Volatile
    var bringToFront: (() -> Unit)? = null
}

/**
 * Desktop notifications go straight to Nucleus `notification-common` — a real OS notification
 * (WinRT toast / Freedesktop D-Bus / `UNUserNotificationCenter`). There is no app-drawn fallback:
 * an event that can't be shown as a toast is still in the `notifications` table, so the in-app
 * notifications screen and the unread-count badge surface it when the window is next opened.
 *
 * The Windows toast AUMID + Start Menu shortcut are derived from the `nucleus.app.id` system
 * property, set in the desktop app's `main()`.
 *
 * Немає окремого дозволу (на відміну від Android/iOS/Web), тож [requestPermission] завжди `true`.
 */
internal class DesktopLocalNotifier : LocalNotifier {

    override suspend fun requestPermission(): Boolean = true

    override fun notify(title: String, body: String, deepLink: String?) {
        // runCatching covers the whole path: isAvailable() / send() throw UnsatisfiedLinkError when
        // a backend's native lib can't load. A failure is logged, not surfaced (see the class doc).
        val failure: String? = runCatching {
            if (!NotificationManager.isAvailable()) return@runCatching "no notification backend on this platform"
            when (
                val result = notification(
                    title = title,
                    message = body,
                    onActivated = {
                        // Nucleus does not guarantee this runs on a UI thread; both callees are
                        // thread-safe (a StateFlow write / a snapshot-state write).
                        deepLink?.let(NotificationDeepLinks::onIncomingLink)
                        DesktopNotificationWindow.bringToFront?.invoke()
                    },
                ).send()
            ) {
                is NotificationResult.Success -> null
                is NotificationResult.Failure -> result.reason
            }
        }.getOrElse { it.message ?: it::class.simpleName }

        if (failure != null) Napier.w { "Desktop notification not shown ($failure): $title" }
    }
}
