import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.ComposeUIViewController
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin
import org.bigblackowl.debttracker.core.notifications.DueReminderCoordinator
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.koin.core.context.GlobalContext
import platform.Foundation.NSURL
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

/**
 * Bridges an OAuth callback URL (`debttracker://login-callback?code=...`) from `iosApp.swift`'s
 * `.onOpenURL` into supabase-kt, which finishes the PKCE exchange and sets the session.
 * Contact-card `debttracker://contact` links keep going through `ContactDeepLinks` on the Swift side.
 */
fun handleAuthDeeplink(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    GlobalContext.get().get<SupabaseClient>().handleDeeplinks(nsUrl)
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    LaunchedEffect(isDark) {
        UIApplication.sharedApplication.setStatusBarStyle(
            if (isDark) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        )
    }
}