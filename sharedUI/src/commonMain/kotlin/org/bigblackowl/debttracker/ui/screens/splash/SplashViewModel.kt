package org.bigblackowl.debttracker.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.settings.AppSettings
import kotlin.time.Duration.Companion.milliseconds

/** Decides where [SplashScreen] routes next: onboarding (first launch) → auth gate (if app lock is on) → unlocked. */
class SplashViewModel(private val settings: AppSettings) : ViewModel() {

    private val effectsChannel = Channel<SplashEffect>()
    val effects = effectsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            delay(300.milliseconds)
            val destination = when {
                currentPlatform != AppPlatform.WEB && !settings.hasSeenProtectionOnboarding -> SplashDestination.ONBOARDING
                settings.protectionEnabled -> SplashDestination.AUTH_GATE
                else -> SplashDestination.UNLOCKED
            }
            effectsChannel.send(SplashEffect.NavigateTo(destination))
        }
    }
}
