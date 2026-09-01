package org.bigblackowl.debttracker.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.RestoreCredentialGateway
import kotlin.time.Duration.Companion.milliseconds

/** Decides where [SplashScreen] routes next: onboarding (first launch) → auth gate (if app lock is on) → unlocked. */
class SplashViewModel(
    private val settings: AppSettings,
    private val authRepository: AuthRepository,
    private val restoreCredentials: RestoreCredentialGateway,
) : ViewModel() {

    private val effectsChannel = Channel<SplashEffect>()
    val effects = effectsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            delay(300.milliseconds)
            // Zero-tap sign-in on a new device: if this install has no session yet, try to restore
            // one from an OS restore credential before routing. Capped so a slow/offline network
            // can't stall the splash; the local app-lock gate below is unaffected either way.
            if (restoreCredentials.isActive && !authRepository.isAuthenticated.value) {
                withTimeoutOrNull(RESTORE_TIMEOUT_MS) { restoreCredentials.tryRestoreSession() }
            }
            // First-launch onboarding (account → app-lock, in that order) is routed by
            // screenAfterUnlock() in the nav graph, reached via UNLOCKED — so the only thing decided
            // here is whether an already-configured app lock must be cleared first.
            val destination = when {
                settings.protectionEnabled -> SplashDestination.AUTH_GATE
                else -> SplashDestination.UNLOCKED
            }
            effectsChannel.send(SplashEffect.NavigateTo(destination))
        }
    }

    private companion object {
        const val RESTORE_TIMEOUT_MS = 4_000L
    }
}
