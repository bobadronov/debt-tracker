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
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.notifications.EXTRA_NOTIFICATION_DEEP_LINK
import org.bigblackowl.debttracker.core.notifications.NotificationDeepLinks
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

    private fun forwardDeepLink(intent: Intent?) {
        intent ?: return
        intent.data?.let { ContactDeepLinks.onIncomingLink(it.toString()) }
        intent.getStringExtra(EXTRA_NOTIFICATION_DEEP_LINK)?.let { link ->
            NotificationDeepLinks.onIncomingLink(link)
            intent.removeExtra(EXTRA_NOTIFICATION_DEEP_LINK) // don't re-fire on a later Activity recreate
        }
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
