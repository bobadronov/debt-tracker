package org.bigblackowl.debttracker.data.remote

import io.github.aakira.napier.Napier
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
private data class ProfileDto(
    val id: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val phone: String? = null,
)

/**
 * No defaults on any field (unlike [ProfileDto]) so kotlinx.serialization always emits them —
 * needed here because supabase-kt's Postgrest Json drops default-valued (including null) fields,
 * which would otherwise make it impossible to explicitly clear [phone] back to null via upsert.
 */
@Serializable
private data class ProfileUpdateDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val phone: String?,
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

    override val email: StateFlow<String?> = client.auth.sessionStatus
        .map { (it as? SessionStatus.Authenticated)?.session?.user?.email }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _avatarUrl = MutableStateFlow<String?>(null)
    override val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    override val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _phone = MutableStateFlow<String?>(null)
    override val phone: StateFlow<String?> = _phone.asStateFlow()

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                val profile = if (status is SessionStatus.Authenticated) {
                    fetchProfile(status.session.user?.id)
                } else {
                    null
                }
                _avatarUrl.value = profile?.avatarUrl
                _displayName.value = profile?.displayName
                _phone.value = profile?.phone
            }
        }
    }

    private suspend fun fetchProfile(userId: String?): ProfileDto? {
        if (userId == null) return null
        return runCatching {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()
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
        // Unlike every other method here, callers (ForceSignOutUseCase) treat this as best-effort:
        // local cache clearing already happened by the time this runs, so a network failure here
        // must not propagate and crash the caller's coroutine (NavGraph's revokedElsewhere
        // LaunchedEffect and SettingsViewModel's viewModelScope.launch both have no exception handler).
        runCatching { client.auth.signOut() }
            .onFailure { Napier.w(tag = "SupabaseAuthRepository", throwable = it) { "Remote signOut() failed; local sign-out still proceeds" } }
    }

    override suspend fun updateAvatar(bytes: ByteArray, fileExtension: String): Result<String> = runCatching {
        val userId = currentUserId ?: error("updateAvatar called while signed out")
        val path = "$userId/avatar.$fileExtension"
        client.storage.from(AVATAR_BUCKET).upload(path, bytes) { upsert = true }
        // Cache-bust the public URL so Coil/browsers don't keep showing a stale image at the same path.
        val url = "${client.storage.from(AVATAR_BUCKET).publicUrl(path)}?t=${kotlin.time.Clock.System.now().toEpochMilliseconds()}"
        client.from("profiles").upsert(ProfileDto(id = userId, avatarUrl = url))
        _avatarUrl.value = url
        url
    }

    override suspend fun updateProfile(displayName: String, phone: String?): Result<Unit> = runCatching {
        val userId = currentUserId ?: error("updateProfile called while signed out")
        val normalizedPhone = phone?.trim()?.ifBlank { null }
        client.from("profiles").upsert(
            ProfileUpdateDto(id = userId, displayName = displayName, phone = normalizedPhone)
        )
        _displayName.value = displayName
        _phone.value = normalizedPhone
    }
}
