import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.awt.Dimension

/** Desktop (JVM) entry point: starts Koin + background sync, then opens the app window. */
fun main() {
    val koinApp = initKoin()
    koinApp.koin.get<SyncCoordinator>().start()
    startApp(koinApp.koin.get())
}

/**
 * [AppSettings.runInBackground] (Settings → "Run in background", desktop-only) changes what the
 * window's close button does: on, it hides the window behind a tray icon instead of exiting, so
 * background sync keeps running; off, it behaves like a normal window and exits the process.
 */
private fun startApp(settings: AppSettings) = application {
    var isWindowVisible by remember { mutableStateOf(true) }
    val icon = remember { windowIcon() }
    // Skia only decodes ICO/PNG (see windowIcon() below) — on platforms where that returns null
    // (macOS) there's no tray icon to show, so the tray is skipped there rather than crashing.
    val strings = remember(settings.locale) { resolveStrings(settings.locale) }

    if (settings.runInBackground && icon != null) {
        Tray(
            icon = icon,
            tooltip = "Debt Tracker",
            onAction = { isWindowVisible = true },
            menu = {
                Item(strings.trayOpen, onClick = { isWindowVisible = true })
                Item(strings.trayQuit, onClick = ::exitApplication)
            },
        )
    }

    Window(
        title = "Debt Tracker",
        icon = icon,
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

@Preview
@Composable
private fun Preview() {
    val icon = remember { windowIcon() }
    Card {
        icon?.let { Icon(painter = it, contentDescription = null) }
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
        os.contains("mac") -> return null
        else -> "appIcons/LinuxIcon.png"
    }
    val bytes = ClassLoader.getSystemResourceAsStream(resourcePath)?.use { it.readAllBytes() }
        ?: return null
    return BitmapPainter(bytes.decodeToImageBitmap())
}

