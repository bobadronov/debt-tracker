package org.bigblackowl.debttracker.ui.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Splash → перевірка onboarding/auth/biometric → редірект (спек §6, п.1), decided by [SplashViewModel].
 */
@Composable
fun SplashScreen(onFinished: (SplashDestination) -> Unit, viewModel: SplashViewModel = koinViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SplashEffect.NavigateTo -> onFinished(effect.destination)
            }
        }
    }
    PlaceholderScreen(title = LocalStrings.current.appName)
}

@Preview
@Composable
private fun SplashScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    SplashScreen(onFinished = {})
}

@Preview
@Composable
private fun SplashScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    SplashScreen(onFinished = {})
}

@Preview(device = DESKTOP)
@Composable
private fun SplashScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    SplashScreen(onFinished = {})
}

@Preview(device = DESKTOP)
@Composable
private fun SplashScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    SplashScreen(onFinished = {})
}
