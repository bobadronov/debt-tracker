package org.bigblackowl.debttracker.core.auth

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import kotlinx.browser.window

/**
 * Web "Continue with Google": a full-page redirect to Google, then back to the app's own origin
 * with a PKCE code in the URL. supabase-kt's web platform detects and exchanges it automatically
 * on the next page load (before App() reads sessionStatus), so this just kicks off the
 * navigation — the returned [GoogleSignInOutcome.Success] only means "we're leaving now".
 *
 * The redirect target (current URL, no query or hash) must be in the Supabase redirect allow-list:
 * the deployed GitHub Pages site, or a localhost dev origin.
 */
class WebGoogleSignInLauncher(
    private val client: SupabaseClient,
) : GoogleSignInLauncher {

    override suspend fun signIn(): GoogleSignInOutcome = try {
        val redirectUrl = window.location.href.substringBefore('?').substringBefore('#')
        client.auth.signInWith(Google, redirectUrl = redirectUrl)
        GoogleSignInOutcome.Success
    } catch (e: Exception) {
        Napier.w(tag = "WebGoogleSignInLauncher", throwable = e) { "Web Google sign-in failed to start" }
        GoogleSignInOutcome.Failure(e.message)
    }
}
