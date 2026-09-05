package org.bigblackowl.debttracker.core.auth

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.core.context.GlobalContext

/**
 * OAuth "Continue with Google" callback handling for `AppActivity`. Lives here (not in the app
 * module) because supabase-kt is an `implementation` dependency of `:sharedUI` and isn't on
 * `:androidApp`'s compile classpath.
 */

/** True when [intent] is the `debttracker://login-callback` OAuth redirect. */
fun isAuthCallbackIntent(intent: Intent): Boolean =
    intent.data?.scheme == "debttracker" && intent.data?.host == "login-callback"

/** Hands the callback intent to supabase-kt, which finishes the PKCE code exchange. */
fun handleAuthDeeplink(intent: Intent) {
    GlobalContext.get().get<SupabaseClient>().handleDeeplinks(intent)
}
