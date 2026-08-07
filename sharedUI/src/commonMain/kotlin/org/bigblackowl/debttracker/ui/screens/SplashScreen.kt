package org.bigblackowl.debttracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

/** Splash → перевірка auth/biometric → редірект (спек §6, п.1). */
@Composable
fun SplashScreen(onFinished: (skipAuthGate: Boolean) -> Unit) {
    val settings = koinInject<AppSettings>()
    LaunchedEffect(Unit) {
        delay(300.milliseconds)
        onFinished(!settings.protectionEnabled)
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