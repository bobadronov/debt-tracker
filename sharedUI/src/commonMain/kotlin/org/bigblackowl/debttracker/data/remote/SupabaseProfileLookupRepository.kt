package org.bigblackowl.debttracker.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.ProfileLookupRepository

@Serializable
private data class FindProfileByEmailParams(@SerialName("p_email") val email: String)

@Serializable
private data class ProfileSuggestionDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/**
 * [ProfileLookupRepository] backed by the `find_profile_by_email` Postgres RPC (SECURITY DEFINER —
 * bypasses `profiles`' owner-only RLS to return only safe public fields for a single email match).
 * Local-only (unauthenticated) users never have the session this RPC requires, so the lookup is
 * skipped rather than attempted; any network/RPC failure is swallowed to null — this is a "nice to
 * have" suggestion, never a blocker for saving a debtor/creditor.
 */
class SupabaseProfileLookupRepository(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository,
) : ProfileLookupRepository {

    override suspend fun findProfileByEmail(email: String): ProfileSuggestion? {
        if (!authRepository.isAuthenticated.value) return null
        return runCatching {
            client.postgrest.rpc("find_profile_by_email", FindProfileByEmailParams(email))
                .decodeSingleOrNull<ProfileSuggestionDto>()
        }.getOrNull()?.let { ProfileSuggestion(displayName = it.displayName, avatarUrl = it.avatarUrl) }
    }
}
