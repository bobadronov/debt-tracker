package org.bigblackowl.debttracker.core.auth

/**
 * Starts the "Continue with Google" flow and drives it to completion.
 *
 * Android runs the native Credential Manager flow (Google mints an ID token which is exchanged for
 * a Supabase session via [org.bigblackowl.debttracker.domain.repository.AuthRepository.signInWithGoogleIdToken]).
 * Desktop/Web/iOS run the Supabase OAuth browser flow; the session is established inside the
 * launcher (desktop loopback + `exchangeCodeForSession`) or by the platform on the callback
 * (web page reload / iOS `handleDeeplinks`), so the caller only ever sees an outcome.
 *
 * Bound per platform in `platformDataModule()` (like
 * [org.bigblackowl.debttracker.domain.repository.RestoreCredentialGateway]).
 */
interface GoogleSignInLauncher {
    suspend fun signIn(): GoogleSignInOutcome
}

sealed interface GoogleSignInOutcome {
    /** A session is now active (or, on web, the page is navigating away to Google). */
    data object Success : GoogleSignInOutcome

    /** The user dismissed the account picker / consent screen. Not an error — show nothing. */
    data object Cancelled : GoogleSignInOutcome

    /** Anything else (no Play Services, network failure, token rejected, misconfiguration). */
    data class Failure(val message: String?) : GoogleSignInOutcome
}

/** No-op launcher for targets/builds where the feature is disabled — always [GoogleSignInOutcome.Failure]. */
object UnsupportedGoogleSignInLauncher : GoogleSignInLauncher {
    override suspend fun signIn(): GoogleSignInOutcome =
        GoogleSignInOutcome.Failure("Google sign-in is not available on this platform")
}
