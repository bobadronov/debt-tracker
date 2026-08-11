package org.bigblackowl.debttracker.ui.screens.accountonboarding

sealed interface AccountOnboardingIntent {
    data object SignIn : AccountOnboardingIntent
    data object Skip : AccountOnboardingIntent
}

sealed interface AccountOnboardingEffect {
    data object NavigateSignIn : AccountOnboardingEffect
    data object NavigateSkip : AccountOnboardingEffect
}
