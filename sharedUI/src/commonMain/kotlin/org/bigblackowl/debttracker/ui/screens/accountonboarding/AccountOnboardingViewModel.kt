package org.bigblackowl.debttracker.ui.screens.accountonboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.settings.AppSettings

/** Marks [AppSettings.hasSeenAccountOnboarding] before routing to sign-in or skipping straight to Home. */
class AccountOnboardingViewModel(private val settings: AppSettings) : ViewModel() {

    private val effectsChannel = Channel<AccountOnboardingEffect>()
    val effects = effectsChannel.receiveAsFlow()

    fun onIntent(intent: AccountOnboardingIntent) {
        settings.hasSeenAccountOnboarding = true
        val effect = when (intent) {
            AccountOnboardingIntent.SignIn -> AccountOnboardingEffect.NavigateSignIn
            AccountOnboardingIntent.Skip -> AccountOnboardingEffect.NavigateSkip
        }
        viewModelScope.launch { effectsChannel.send(effect) }
    }
}
