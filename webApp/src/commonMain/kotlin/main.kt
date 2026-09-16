import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.core.errorreporting.installGlobalErrorReporting
import org.bigblackowl.debttracker.core.notifications.DueReminderCoordinator
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller

/** Web (JS/Wasm) entry point — mounts [App] into the page's canvas via [ComposeViewport]. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val koinApp = initKoin()
    installGlobalErrorReporting()
    koinApp.koin.get<NotificationsPoller>().start()
    koinApp.koin.get<DueReminderCoordinator>().start()
    ComposeViewport { App() }
}
