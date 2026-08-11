package org.bigblackowl.debttracker.domain.repository

import kotlinx.coroutines.flow.Flow
import org.bigblackowl.debttracker.domain.model.DeviceSession

/** Session management (Settings → Active devices) — Local-only режим просто не викликає ці методи. */
interface SessionRepository {
    /** Non-revoked devices for the signed-in account, most-recently-active first; empty while signed out. */
    fun observeSessions(): Flow<List<DeviceSession>>

    /** Soft-revokes [sessionId] — that device signs itself out next time it observes the change (see [revokedElsewhere]). */
    suspend fun revokeSession(sessionId: String): Result<Unit>

    /** Soft-revokes every device except the one this call runs on. */
    suspend fun revokeAllOtherSessions(): Result<Unit>

    /** Emits once whenever *this* device's own session row gets revoked (from another device) — the collector must sign out. */
    val revokedElsewhere: Flow<Unit>
}
