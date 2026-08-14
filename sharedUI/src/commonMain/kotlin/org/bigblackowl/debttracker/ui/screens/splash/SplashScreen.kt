package org.bigblackowl.debttracker.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.koin.compose.viewmodel.koinViewModel

// Matches the diagonal gradient in the app icon (ic_launcher_background) — brand-fixed, not theme-dependent.
private val SplashGradientStart = Color(0xFF3B82F6)
private val SplashGradientEnd = Color(0xFF29AE38)

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(SplashGradientStart, SplashGradientEnd))),
        contentAlignment = Alignment.Center,
    ) {
        CircularWavyProgressIndicator(
            modifier = Modifier.size(Dimens.space60),
            color = Color.White,
        )
    }
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
