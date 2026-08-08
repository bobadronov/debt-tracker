package org.bigblackowl.debttracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

/** Where [SplashScreen] sends the user next. */
enum class SplashDestination { ONBOARDING, AUTH_GATE, UNLOCKED }

/**
 * Splash → перевірка onboarding/auth/biometric → редірект (спек §6, п.1). First launch
 * (on non-Web platforms) always routes through [SplashDestination.ONBOARDING] once, tracked by
 * [AppSettings.hasSeenProtectionOnboarding], before falling back to the regular auth-gate check.
 */
@Composable
fun SplashScreen(onFinished: (SplashDestination) -> Unit) {
    val settings = koinInject<AppSettings>()
    LaunchedEffect(Unit) {
        delay(300.milliseconds)
        val destination = when {
            currentPlatform != AppPlatform.WEB && !settings.hasSeenProtectionOnboarding -> SplashDestination.ONBOARDING
            settings.protectionEnabled -> SplashDestination.AUTH_GATE
            else -> SplashDestination.UNLOCKED
        }
        onFinished(destination)
    }
    PlaceholderScreen(title = LocalStrings.current.appName)
}

@Preview
@Composable
private fun SplashScreenPreview() = DebtTrackerPreview {
    SplashScreen(onFinished = {})
}

@Preview(device = DESKTOP)
@Composable
private fun SplashScreenPreview2() = DebtTrackerPreview {
    SplashScreen(onFinished = {})
}