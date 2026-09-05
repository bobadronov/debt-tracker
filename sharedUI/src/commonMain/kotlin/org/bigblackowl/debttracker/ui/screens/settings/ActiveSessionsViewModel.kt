package org.bigblackowl.debttracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.SessionRepository

/**
 * Drives [ActiveSessionsScreen] off [SessionRepository.observeSessions] — a revoked row simply
 * disappears once the server (and this device's own realtime subscription) confirms it, there's
 * no separate optimistic-removal step.
 */
class ActiveSessionsViewModel(
    private val sessionRepository: SessionRepository,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val revokingId = MutableStateFlow<String?>(null)
    private val isRevokingAll = MutableStateFlow(false)

    private val effectsChannel = Channel<ActiveSessionsEffect>()
    val effects = effectsChannel.receiveAsFlow()

    val state: StateFlow<ActiveSessionsState> = combine(
        sessionRepository.observeSessions(), revokingId, isRevokingAll,
    ) { sessions, revoking, revokingAll ->
        ActiveSessionsState(isLoading = false, sessions = sessions, revokingId = revoking, isRevokingAll = revokingAll)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveSessionsState())

    fun onIntent(intent: ActiveSessionsIntent) {
        when (intent) {
            is ActiveSessionsIntent.RevokeSession -> viewModelScope.launch {
                revokingId.value = intent.sessionId
                sessionRepository.revokeSession(intent.sessionId).onFailure { reportError() }
                revokingId.value = null
            }

            ActiveSessionsIntent.RevokeAllOthers -> viewModelScope.launch {
                isRevokingAll.value = true
                sessionRepository.revokeAllOtherSessions().onFailure { reportError() }
                isRevokingAll.value = false
            }
        }
    }

    private suspend fun reportError() {
        effectsChannel.send(ActiveSessionsEffect.Error(resolveStrings(appSettings.locale).activeSessions.error))
    }
}
