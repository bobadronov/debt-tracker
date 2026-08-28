package org.bigblackowl.debttracker.core.notifications

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * Desktop: `java.awt.SystemTray`/`TrayIcon.displayMessage` — вбудований AWT баланс-тост, без
 * зовнішніх залежностей. Немає окремого дозволу на показ (на відміну від Android/iOS/Web), тож
 * [requestPermission] завжди `true`. Якщо трей не підтримується ОС ([SystemTray.isSupported]) —
 * тихий no-op (банер усередині застосунку все одно показує подію).
 */
internal class DesktopLocalNotifier : LocalNotifier {
    private val trayIcon: TrayIcon? by lazy {
        if (!SystemTray.isSupported()) return@lazy null
        runCatching {
            val icon = TrayIcon(notificationIconImage(), "DebtTracker")
            icon.isImageAutoSize = true
            SystemTray.getSystemTray().add(icon)
            icon
        }.getOrNull()
    }

    override suspend fun requestPermission(): Boolean = true

    override fun notify(title: String, body: String) {
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
