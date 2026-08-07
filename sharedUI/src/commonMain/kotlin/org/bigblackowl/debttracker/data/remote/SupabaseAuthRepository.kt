package org.bigblackowl.debttracker.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.debttracker.domain.repository.AuthRepository

private const val AVATAR_BUCKET = "avatars"

@Serializable
private data class ProfileAvatarDto(
    val id: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/** [AuthRepository] backed by Supabase Auth (email/password) + `profiles.avatar_url` (Storage bucket `avatars`). */
class SupabaseAuthRepository(
    private val client: SupabaseClient,
    scope: CoroutineScope,
) : AuthRepository {

    override val isAuthenticated: StateFlow<Boolean> = client.auth.sessionStatus
        .map { it is SessionStatus.Authenticated }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override val currentUserId: String?
        get() = client.auth.currentUserOrNull()?.id

    private val _avatarUrl = MutableStateFlow<String?>(null)
    override val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                _avatarUrl.value = if (status is SessionStatus.Authenticated) {
                    fetchAvatarUrl(status.session.user?.id)
                } else {
                    null
                }
            }
        }
    }

    private suspend fun fetchAvatarUrl(userId: String?): String? {
        if (userId == null) return null
        return runCatching {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileAvatarDto>()
                ?.avatarUrl
        }.getOrNull()
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    override suspend fun updateAvatar(bytes: ByteArray, fileExtension: String): Result<String> = runCatching {
        val userId = currentUserId ?: error("updateAvatar called while signed out")
        val path = "$userId/avatar.$fileExtension"
        client.storage.from(AVATAR_BUCKET).upload(path, bytes) { upsert = true }
        // Cache-bust the public URL so Coil/browsers don't keep showing a stale image at the same path.
        val url = "${client.storage.from(AVATAR_BUCKET).publicUrl(path)}?t=${kotlin.time.Clock.System.now().toEpochMilliseconds()}"
        client.from("profiles").upsert(ProfileAvatarDto(id = userId, avatarUrl = url))
        _avatarUrl.value = url
        url
    }
}
