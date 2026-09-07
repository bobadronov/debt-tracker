package org.bigblackowl.debttracker.androidApp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.androidApp.widget.DebtSummaryWidgetReceiver
import org.bigblackowl.debttracker.core.auth.handleAuthDeeplink
import org.bigblackowl.debttracker.core.auth.isAuthCallbackIntent
import org.bigblackowl.debttracker.core.notifications.EXTRA_NOTIFICATION_DEEP_LINK
import org.bigblackowl.debttracker.core.notifications.NotificationDeepLinks
import org.bigblackowl.debttracker.core.platform.AndroidActivityProvider
import org.bigblackowl.debttracker.core.qr.ContactDeepLinks
import org.bigblackowl.debttracker.core.shortcuts.HomeTabRequest

/**
 * Android entry point — `FragmentActivity` rather than `ComponentActivity` because
 * `androidx.biometric.BiometricPrompt` requires a `FragmentActivity` host.
 */
class AppActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AndroidActivityProvider.set(this) // Credential Manager's Google sign-in sheet needs an Activity
        enableEdgeToEdge()
        forwardDeepLink(intent)
        setContent {
            App(onThemeChanged = { ThemeChanged(it) })
        }
    }

    // launchMode="singleInstance" (AndroidManifest.xml) means a `debttracker://contact` link
    // tapped while the app is already running arrives here instead of a fresh onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forwardDeepLink(intent)
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app (usually back to the launcher, where the widget lives) — push the latest
        // balances / theme into it. Cheap: one goAsync DB read, skipped entirely if no widget is placed.
        DebtSummaryWidgetReceiver.refresh(this)
    }

    override fun onDestroy() {
        AndroidActivityProvider.clear(this)
        super.onDestroy()
    }

    private fun forwardDeepLink(intent: Intent?) {
        intent ?: return
        val data = intent.data
        when {
            // OAuth "Continue with Google" callback (debttracker://login-callback?code=...) — hand it
            // straight to supabase-kt (see core/auth/AndroidAuthDeeplink). Android itself uses the
            // native Credential Manager flow, but the filter is registered so a stray callback still
            // resolves here rather than bouncing to a browser.
            isAuthCallbackIntent(intent) -> handleAuthDeeplink(intent)
            data != null -> ContactDeepLinks.onIncomingLink(data.toString())
        }
        intent.getStringExtra(EXTRA_NOTIFICATION_DEEP_LINK)?.let { link ->
            NotificationDeepLinks.onIncomingLink(link)
            intent.removeExtra(EXTRA_NOTIFICATION_DEEP_LINK) // don't re-fire on a later Activity recreate
        }
        // Home-screen widget: a tapped row asks to land on the matching Home tab.
        intent.getIntExtra(DebtSummaryWidgetReceiver.EXTRA_HOME_TAB, -1).takeIf { it >= 0 }?.let { tab ->
            HomeTabRequest.request(tab)
            intent.removeExtra(DebtSummaryWidgetReceiver.EXTRA_HOME_TAB)
        }
    }
}

@Composable
private fun ThemeChanged(isLight: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isLight) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
        // The home-screen widget follows the same light/dark preference; AppActivity.onStop pushes
        // it the fresh palette (and balances) when the user leaves the app.
    }
}
