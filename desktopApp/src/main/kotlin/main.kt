import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.notifications.DesktopNotificationWindow
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.awt.Dimension

/** Desktop (JVM) entry point: starts Koin + background sync/notifications, then opens the app window. */
fun main() {
    FileKit.init(appId = "org.bigblackowl.debttracker") // required for FileKit's Save-As dialogs and cache/files dirs on JVM
    val koinApp = initKoin()
    koinApp.koin.get<SyncCoordinator>().start()
    koinApp.koin.get<NotificationsPoller>().start()
    startApp(koinApp.koin.get())
}

/**
 * [AppSettings.runInBackground] (Settings → "Run in background", desktop-only) changes what the
 * window's close button does: on, it hides the window behind a tray icon instead of exiting, so
 * background sync keeps running; off, it behaves like a normal window and exits the process.
 */
private fun startApp(settings: AppSettings) = application {

    var isWindowVisible by remember { mutableStateOf(true) }

    // Tapping a system notification's banner (core/notifications) un-hides the window when
    // "Run in background" had tucked it away behind the tray.
    DisposableEffect(Unit) {
        DesktopNotificationWindow.bringToFront = { isWindowVisible = true }
        onDispose { DesktopNotificationWindow.bringToFront = null }
    }

    val appIcon = remember { windowIcon() }

    val strings = remember(settings.locale) { resolveStrings(settings.locale) }

    if (settings.runInBackground && appIcon != null) {
        Tray(
            icon = appIcon,
            tooltip = "Debt Tracker",
            onAction = { isWindowVisible = true },
            menu = {
                // No icon= here: the tray popup menu is backed by java.awt.Menu, which has no
                // icon support at all (unlike Swing's JMenuItem) — passing one throws
                // UnsupportedOperationException at runtime (Menu.desktop.kt's AwtMenuScope.Item).
                Item(text = "🗔 " + strings.trayOpen, onClick = { isWindowVisible = true })
                Item(text = "🗙 " + strings.trayQuit, onClick = ::exitApplication)
            },
        )
    }

    Window(
        title = "Debt Tracker",
        icon = appIcon,
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        visible = isWindowVisible,
        onCloseRequest = {
            if (settings.runInBackground) isWindowVisible = false else exitApplication()
        },
    ) {
        window.minimumSize = Dimension(350, 600)
        App()
    }
}

/**
 * Skia (Compose Desktop's renderer) decodes ICO/PNG directly but not macOS' .icns container,
 * тож на macOS іконка вікна не встановлюється тут — вона й так береться з бандла застосунку
 * при пакуванні (compose.desktop.application.nativeDistributions.macOS.iconFile у build.gradle.kts).
 *
 * Іконки лежать у src/main/resources/appIcons і читаються через classloader (а не File() з
 * відносним шляхом): відносний шлях залежить від робочої директорії процесу, яка в
 * запакованому (jpackage) застосунку інша, ніж під час `gradlew run` — через File() іконка
 * не знаходилась після встановлення.
 */
private fun windowIcon(): Painter? {
    val os = System.getProperty("os.name").lowercase()
    val resourcePath = when {
        os.contains("win") -> "appIcons/WindowsIcon.ico"
        os.contains("mac") -> "appIcons/MacosIcon.icns"
        else -> "appIcons/LinuxIcon.png"
    }
    return resourcePainter(resourcePath)
}

private fun resourcePainter(resourcePath: String): Painter? {
    val bytes = ClassLoader.getSystemResourceAsStream(resourcePath)?.use { it.readAllBytes() }
        ?: return null
    return BitmapPainter(bytes.decodeToImageBitmap())
}

