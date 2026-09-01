import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.ComposeUIViewController
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.core.notifications.DueReminderCoordinator
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.setStatusBarStyle

private var koinInitialized = false

/** iOS entry point, called from `App.swift`'s minimal Swift shell — everything below is shared Compose Multiplatform UI. */
fun MainViewController(): UIViewController {
    if (!koinInitialized) {
        val koinApp = initKoin()
        koinApp.koin.get<SyncCoordinator>().start()
        koinApp.koin.get<NotificationsPoller>().start()
        koinApp.koin.get<DueReminderCoordinator>().start()
        koinInitialized = true
    }
    return ComposeUIViewController {
        App(onThemeChanged = { ThemeChanged(it) })
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    LaunchedEffect(isDark) {
        UIApplication.sharedApplication.setStatusBarStyle(
            if (isDark) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        )
    }
}