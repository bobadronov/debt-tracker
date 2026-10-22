package org.bigblackowl.debttracker.core.notifications

import io.github.aakira.napier.Napier
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.Timer
import javax.imageio.ImageIO
import kotlin.concurrent.schedule

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
 * Desktop notifications use the JDK's AWT [SystemTray] balloon (no native libs of our own). Each
 * notification adds a short-lived [TrayIcon], shows the balloon and removes the icon again, so it
 * never competes with the app's own tray icon. Clicking the balloon fires the deep link and
 * surfaces the window. If the platform has no system tray the event is only logged: it is still in
 * the `notifications` table, so the in-app screen and unread badge show it.
 *
 * There is no separate permission (unlike Android/iOS/Web), so [requestPermission] is always `true`.
 */
internal class DesktopLocalNotifier : LocalNotifier {

    override suspend fun requestPermission(): Boolean = true

    override fun notify(title: String, body: String, deepLink: String?) {
        val failure: String? = runCatching {
            if (!SystemTray.isSupported()) return@runCatching "system tray not supported"
            val tray = SystemTray.getSystemTray()
            val icon = TrayIcon(trayImage(), "Debt Tracker")
            icon.isImageAutoSize = true
            icon.addActionListener {
                deepLink?.let(NotificationDeepLinks::onIncomingLink)
                DesktopNotificationWindow.bringToFront?.invoke()
            }
            tray.add(icon)
            icon.displayMessage(title, body, TrayIcon.MessageType.INFO)
            Timer(true).schedule(REMOVE_DELAY_MS) { runCatching { tray.remove(icon) } }
            null
        }.getOrElse { it.message ?: it::class.simpleName }

        if (failure != null) Napier.w { "Desktop notification not shown ($failure): $title" }
    }

    private fun trayImage() = runCatching {
        ClassLoader.getSystemResourceAsStream("appIcons/LinuxIcon.png")?.use(ImageIO::read)
    }.getOrNull() ?: BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)

    private companion object {
        const val REMOVE_DELAY_MS = 15_000L
    }
}
