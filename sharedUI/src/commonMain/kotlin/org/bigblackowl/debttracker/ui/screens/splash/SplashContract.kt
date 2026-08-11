package org.bigblackowl.debttracker.ui.screens.splash

/** Where [SplashScreen] sends the user next. */
enum class SplashDestination { ONBOARDING, AUTH_GATE, UNLOCKED }

sealed interface SplashEffect {
    data class NavigateTo(val destination: SplashDestination) : SplashEffect
}
