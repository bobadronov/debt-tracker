package org.bigblackowl.debttracker.androidApp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.glance.appwidget.updateAll
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.androidApp.widget.DebtSummaryWidget
import org.bigblackowl.debttracker.core.auth.handleAuthDeeplink
import org.bigblackowl.debttracker.core.auth.isAuthCallbackIntent
import org.bigblackowl.debttracker.core.notifications.EXTRA_NOTIFICATION_DEEP_LINK
import org.bigblackowl.debttracker.core.notifications.NotificationDeepLinks
import org.bigblackowl.debttracker.core.platform.AndroidActivityProvider
import org.bigblackowl.debttracker.core.qr.ContactDeepLinks
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.koin.core.context.GlobalContext

/**
 * Android entry point — `FragmentActivity` rather than `ComponentActivity` because
 * `androidx.biometric.BiometricPrompt` requires a `FragmentActivity` host.
 */
class AppActivity : FragmentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* результат лише інформативний — LocalNotifier сам перевіряє areNotificationsEnabled() перед кожним показом */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AndroidActivityProvider.set(this) // Credential Manager's Google sign-in sheet needs an Activity
        enableEdgeToEdge()
        forwardDeepLink(intent)
        requestNotificationPermissionOnce()
        setContent {
            App(onThemeChanged = { ThemeChanged(it) })
        }
    }

    /** Android 13+ (`POST_NOTIFICATIONS`) потребує runtime-запиту з Activity — запитуємо раз, при першому запуску. */
    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val appSettings = GlobalContext.get().get<AppSettings>()
        if (appSettings.notificationsPermissionRequested) return
        appSettings.notificationsPermissionRequested = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // launchMode="singleInstance" (AndroidManifest.xml) means a `debttracker://contact` link
    // tapped while the app is already running arrives here instead of a fresh onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forwardDeepLink(intent)
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
    }
}

@Composable
private fun ThemeChanged(isLight: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    LaunchedEffect(isLight) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
        // Keep the home-screen widget on the same light/dark palette: it reads AppSettings.theme
        // when it renders but otherwise only re-renders on its ~30-min tick, so push an update
        // now that the preference changed (also runs once on launch — cheap, and keeps it fresh).
        DebtSummaryWidget().updateAll(context)
    }
}
