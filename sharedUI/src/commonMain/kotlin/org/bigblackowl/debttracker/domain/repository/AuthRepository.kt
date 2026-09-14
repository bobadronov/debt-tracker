package org.bigblackowl.debttracker.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Account+Sync (spec §1.1, §5) — Local-only mode simply never calls these methods. */
interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    val currentUserId: String?

    /** Session email (`auth.users.email`) — immutable here, changing the email requires a separate confirmation flow. */
    val email: StateFlow<String?>

    /** `profiles.display_name` — null until authenticated or the name isn't set. */
    val displayName: StateFlow<String?>

    /** `profiles.phone` — null until authenticated or the phone isn't set (as opposed to a debtor/creditor contact's phone). */
    val phone: StateFlow<String?>

    /** Account photo URL (`profiles.avatar_url`) — null until authenticated or the photo isn't uploaded. */
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

    /** Uploads the photo to Storage (`avatars/{userId}/avatar.{fileExtension}`) and stores the URL in [avatarUrl]/`profiles.avatar_url`. */
    suspend fun updateAvatar(bytes: ByteArray, fileExtension: String): Result<String>

    /** Updates `profiles.display_name`/`profiles.phone` together (Edit Account screen) — an empty phone is stored as null. */
    suspend fun updateProfile(displayName: String, phone: String?): Result<Unit>
}
