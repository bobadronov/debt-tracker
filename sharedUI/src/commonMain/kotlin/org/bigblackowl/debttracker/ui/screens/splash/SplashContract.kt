package org.bigblackowl.debttracker.ui.screens.splash

/**
 * Where [SplashScreen] sends the user next. First-launch onboarding order (account → app-lock) is
 * decided by `screenAfterUnlock()` in the nav graph, so [UNLOCKED] covers "first launch" too.
 */
enum class SplashDestination { AUTH_GATE, UNLOCKED }

sealed interface SplashEffect {
    data class NavigateTo(val destination: SplashDestination) : SplashEffect
}
