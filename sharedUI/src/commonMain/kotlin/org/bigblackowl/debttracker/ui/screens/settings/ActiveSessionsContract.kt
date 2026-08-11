package org.bigblackowl.debttracker.ui.screens.settings

import org.bigblackowl.debttracker.domain.model.DeviceSession

/** MVI contract for [ActiveSessionsScreen] — lists the account's signed-in devices and remotely logs any of them out. */
data class ActiveSessionsState(
    val isLoading: Boolean = true,
    val sessions: List<DeviceSession> = emptyList(),
    val revokingId: String? = null,
    val isRevokingAll: Boolean = false,
)

sealed interface ActiveSessionsIntent {
    data class RevokeSession(val sessionId: String) : ActiveSessionsIntent
    data object RevokeAllOthers : ActiveSessionsIntent
}

sealed interface ActiveSessionsEffect {
    data class Error(val message: String) : ActiveSessionsEffect
}
