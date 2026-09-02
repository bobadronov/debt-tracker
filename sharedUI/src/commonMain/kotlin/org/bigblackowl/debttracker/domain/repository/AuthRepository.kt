package org.bigblackowl.debttracker.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Account+Sync (спек §1.1, §5) — Local-only режим просто не викликає ці методи. */
interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    val currentUserId: String?

    /** Email сесії (`auth.users.email`) — незмінний тут, зміна пошти потребує окремого потоку підтвердження. */
    val email: StateFlow<String?>

    /** `profiles.display_name` — null поки не авторизований або ім'я не задане. */
    val displayName: StateFlow<String?>

    /** `profiles.phone` — null поки не авторизований або телефон не задано (на відміну від телефону контакту debtor/creditor). */
    val phone: StateFlow<String?>

    /** URL фото акаунта (`profiles.avatar_url`) — null поки не авторизований або фото не завантажене. */
    val avatarUrl: StateFlow<String?>

    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>

    /**
     * Exchanges a Google ID token (from Android's Credential Manager) for a Supabase session.
     * [rawNonce] is the un-hashed nonce whose SHA-256 was passed to Google; Supabase re-hashes and
     * compares it. The OAuth ("Continue with Google") flow on Desktop/Web/iOS goes through
     * [org.bigblackowl.debttracker.core.auth.GoogleSignInLauncher] instead and never calls this.
     */
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String?): Result<Unit>

    suspend fun signOut()

    /** Завантажує фото в Storage (`avatars/{userId}/avatar.{fileExtension}`) і зберігає URL у [avatarUrl]/`profiles.avatar_url`. */
    suspend fun updateAvatar(bytes: ByteArray, fileExtension: String): Result<String>

    /** Оновлює `profiles.display_name`/`profiles.phone` разом (Edit Account screen) — порожній phone зберігається як null. */
    suspend fun updateProfile(displayName: String, phone: String?): Result<Unit>
}
