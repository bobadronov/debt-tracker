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
import org.bigblackowl.debttracker.core.qr.ContactDeepLinks

/**
 * Android entry point — `FragmentActivity` rather than `ComponentActivity` because
 * `androidx.biometric.BiometricPrompt` requires a `FragmentActivity` host.
 */
class AppActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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

    private fun forwardDeepLink(intent: Intent?) {
        intent?.data?.let { ContactDeepLinks.onIncomingLink(it.toString()) }
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
