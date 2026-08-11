package org.bigblackowl.debttracker.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.realtime.selectSingleValueAsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.platform.deviceDisplayName
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.DeviceSession
import org.bigblackowl.debttracker.domain.repository.SessionRepository

private const val TABLE = "user_sessions"

/**
 * Backoff before re-subscribing after a failure (network blip, table momentarily unreachable,
 * etc.) in [SupabaseSessionRepository.observeSessions]/[SupabaseSessionRepository.revokedElsewhere].
 * Without this, one failed request kills the flow for the rest of the app session — the
 * PGRST205-table-missing incident (2026-08-11) crashed the app this way, since nothing downstream
 * caught it.
 */
private const val SESSION_RETRY_DELAY_MS = 5_000L

@Serializable
private data class SessionDto(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("device_name") val deviceName: String = "",
    val platform: String = "",
    @SerialName("last_seen_at") val lastSeenAt: String = "",
    @SerialName("revoked_at") val revokedAt: String? = null,
) {
    fun toDomain(currentSessionId: String) = DeviceSession(
        id = id,
        deviceName = deviceName,
        platform = AppPlatform.entries.firstOrNull { it.name == platform } ?: AppPlatform.WEB,
        lastSeenAt = Instant.parse(lastSeenAt),
        isCurrentDevice = id == currentSessionId,
    )
}

/**
 * No defaults (unlike [SessionDto]) so kotlinx.serialization always emits every field — needed
 * because supabase-kt's Postgrest Json drops default-valued fields, which would otherwise mean
 * every register/touch upsert silently skips bumping `last_seen_at`. `revoked_at`/`created_at`
 * are intentionally absent here so a touch never un-revokes or resets when the device already
 * has a row.
 */
@Serializable
private data class SessionUpsertDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("device_name") val deviceName: String,
    val platform: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("last_seen_at") val lastSeenAt: String,
)

/** [SessionRepository] backed by `public.user_sessions` — see migration 0006 for the revoke model. */
@OptIn(SupabaseExperimental::class, ExperimentalCoroutinesApi::class)
class SupabaseSessionRepository(
    private val client: SupabaseClient,
    private val appSettings: AppSettings,
) : SessionRepository {

    private val currentSessionId get() = appSettings.deviceSessionId

    override fun observeSessions(): Flow<List<DeviceSession>> =
        client.auth.sessionStatus
            .distinctUntilChangedBy { (it as? SessionStatus.Authenticated)?.session?.user?.id }
            .flatMapLatest { status ->
                val userId = (status as? SessionStatus.Authenticated)?.session?.user?.id
                if (userId == null) {
                    emptyFlow()
                } else {
                    client.from(TABLE).selectAsFlow(
                        SessionDto::id,
                        filter = FilterOperation("user_id", FilterOperator.EQ, userId),
                    ).map { sessions ->
                        sessions
                            .filter { it.revokedAt == null }
                            .map { it.toDomain(currentSessionId) }
                            .sortedByDescending { it.lastSeenAt }
                    }.retry { delay(SESSION_RETRY_DELAY_MS); true }
                }
            }

    override suspend fun revokeSession(sessionId: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("revokeSession called while signed out")
        client.from(TABLE).update({ set("revoked_at", Clock.System.now().toString()) }) {
            filter {
                eq("id", sessionId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun revokeAllOtherSessions(): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("revokeAllOtherSessions called while signed out")
        client.from(TABLE).update({ set("revoked_at", Clock.System.now().toString()) }) {
            filter {
                eq("user_id", userId)
                neq("id", currentSessionId)
            }
        }
    }

    override val revokedElsewhere: Flow<Unit> =
        client.auth.sessionStatus
            .distinctUntilChangedBy { (it as? SessionStatus.Authenticated)?.session?.user?.id }
            .flatMapLatest { status ->
                val userId = (status as? SessionStatus.Authenticated)?.session?.user?.id
                if (userId == null) {
                    emptyFlow()
                } else {
                    flow {
                        registerOrTouchSession(userId)
                        emitAll(
                            client.from(TABLE).selectSingleValueAsFlow(SessionDto::id) {
                                eq("id", currentSessionId)
                            }
                        )
                    }.retry { delay(SESSION_RETRY_DELAY_MS); true }
                }
            }.filter { it.revokedAt != null }.map { }

    private suspend fun registerOrTouchSession(userId: String) {
        client.from(TABLE).upsert(
            SessionUpsertDto(
                id = currentSessionId,
                userId = userId,
                deviceName = deviceDisplayName(),
                platform = currentPlatform.name,
                appVersion = BuildConfig.APP_VERSION,
                lastSeenAt = Clock.System.now().toString(),
            )
        )
    }
}
