package org.bigblackowl.debttracker.core.auth

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google

/**
 * iOS "Continue with Google": opens the system browser at the Supabase authorize URL. Supabase
 * redirects back to `debttracker://login-callback` (the `scheme`/`host` set on the `Auth` plugin),
 * which `iosApp.swift`'s `.onOpenURL` forwards to `handleAuthDeeplink` in `iosMain/main.kt` →
 * `SupabaseClient.handleDeeplinks(...)`, establishing the session.
 *
 * So [GoogleSignInOutcome.Success] here only means the browser opened; the session arrives
 * asynchronously via the deep link and `sessionStatus`.
 */
class IosGoogleSignInLauncher(
    private val client: SupabaseClient,
) : GoogleSignInLauncher {

    override suspend fun signIn(): GoogleSignInOutcome = try {
        client.auth.signInWith(Google)
        GoogleSignInOutcome.Success
    } catch (e: Exception) {
        Napier.w(tag = "IosGoogleSignInLauncher", throwable = e) { "iOS Google sign-in failed to start" }
        GoogleSignInOutcome.Failure(e.message)
    }
}
