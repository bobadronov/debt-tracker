
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.awt.Dimension
import java.io.File

/** Desktop (JVM) entry point: starts Koin + background sync, then opens the app window. */
fun main() {
    val koinApp = initKoin()
    koinApp.koin.get<SyncCoordinator>().start()
    startApp()
}

private fun startApp() = application {
    Window(
        title = "Debt Tracker",
        icon = windowIcon(),
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App()
    }
}

/**
 * Skia (Compose Desktop's renderer) decodes ICO/PNG directly but not macOS' .icns container,
 * тож на macOS іконка вікна не встановлюється тут — вона й так береться з бандла застосунку
 * при пакуванні (compose.desktop.application.nativeDistributions.macOS.iconFile у build.gradle.kts).
 */
private fun windowIcon(): Painter? {
    val os = System.getProperty("os.name").lowercase()
    val iconFile = when {
        os.contains("win") -> File("appIcons/WindowsIcon.ico")
        os.contains("mac") -> return null
        else -> File("appIcons/LinuxIcon.png")
    }
    if (!iconFile.exists()) return null
    return BitmapPainter(iconFile.inputStream().readAllBytes().decodeToImageBitmap())
}

