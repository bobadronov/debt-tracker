package org.bigblackowl.debttracker.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Account+Sync (спек §1.1, §5) — Local-only режим просто не викликає ці методи. */
interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    val currentUserId: String?

    /** URL фото акаунта (`profiles.avatar_url`) — null поки не авторизований або фото не завантажене. */
    val avatarUrl: StateFlow<String?>

    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()

    /** Завантажує фото в Storage (`avatars/{userId}/avatar.{fileExtension}`) і зберігає URL у [avatarUrl]/`profiles.avatar_url`. */
    suspend fun updateAvatar(bytes: ByteArray, fileExtension: String): Result<String>
}
