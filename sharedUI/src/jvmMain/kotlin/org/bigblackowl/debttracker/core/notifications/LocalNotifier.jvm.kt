package org.bigblackowl.debttracker.core.notifications

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * Set by the desktop app's `main()` so a clicked tray notification can bring the (possibly
 * hidden — Settings → "Run in background") window back to the front. `null` on every other
 * platform / before the window exists.
 */
object DesktopNotificationWindow {
    @Volatile
    var bringToFront: (() -> Unit)? = null
}

/**
 * Desktop: `java.awt.SystemTray`/`TrayIcon.displayMessage` — вбудований AWT баланс-тост, без
 * зовнішніх залежностей. Немає окремого дозволу на показ (на відміну від Android/iOS/Web), тож
 * [requestPermission] завжди `true`. Якщо трей не підтримується ОС ([SystemTray.isSupported]) —
 * тихий no-op (банер усередині застосунку все одно показує подію).
 *
 * Клік по банеру (`TrayIcon.addActionListener`) відкриває застосунок на екрані останнього
 * сповіщення через [NotificationDeepLinks] + [DesktopNotificationWindow].
 */
internal class DesktopLocalNotifier : LocalNotifier {
    // Only the most recent notification's banner is realistically clickable, so one slot is enough.
    @Volatile
    private var pendingDeepLink: String? = null

    private val trayIcon: TrayIcon? by lazy {
        if (!SystemTray.isSupported()) return@lazy null
        runCatching {
            val icon = TrayIcon(notificationIconImage(), "DebtTracker")
            icon.isImageAutoSize = true
            icon.addActionListener {
                val link = pendingDeepLink ?: return@addActionListener
                NotificationDeepLinks.onIncomingLink(link)
                DesktopNotificationWindow.bringToFront?.invoke()
            }
            SystemTray.getSystemTray().add(icon)
            icon
        }.getOrNull()
    }

    override suspend fun requestPermission(): Boolean = true

    override fun notify(title: String, body: String, deepLink: String?) {
        pendingDeepLink = deepLink
        runCatching { trayIcon?.displayMessage(title, body, TrayIcon.MessageType.INFO) }
    }
}

/** Маленька суцільна кругла іконка — намальована програмно, щоб не залежати від ресурсів desktopApp-модуля. */
private fun notificationIconImage(): java.awt.Image {
    val size = 16
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g: Graphics2D = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color(0x2E, 0x7D, 0x32) // той самий зелений акцент, що й "мені винні" в темі застосунку
    g.fillOval(0, 0, size, size)
    g.dispose()
    return Toolkit.getDefaultToolkit().createImage(image.source)
}
