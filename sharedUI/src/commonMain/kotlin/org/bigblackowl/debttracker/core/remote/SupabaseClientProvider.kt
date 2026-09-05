package org.bigblackowl.debttracker.core.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import org.bigblackowl.debttracker.BuildConfig

/**
 * supabase-kt клієнт (спек §1.1) — проєкт "debt-tracker" (nywvasgnbgnixfjzadbu),
 * створений і мігрований у Фазі 6 через Supabase MCP. Ktor-engine підбирається
 * автоматично з класпату кожної платформи (okhttp — Android/Desktop, darwin — iOS).
 *
 * @param sessionManager Platform-specific encrypted session storage (see [org.bigblackowl.debttracker.core.di.platformDataModule]) —
 * `null` (Web) falls back to supabase-kt's own default [io.github.jan.supabase.auth.SettingsSessionManager] (localStorage there).
 */
fun createAppSupabaseClient(sessionManager: SessionManager?): SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
) {
    install(Auth) {
        // PKCE for the OAuth ("Continue with Google") flow on Desktop/Web/iOS — the desktop
        // loopback handler and web page-reload both finish it with exchangeCodeForSession /
        // the platform's automatic URL detection.
        flowType = FlowType.PKCE
        // Deep-link the OAuth callback lands on for the mobile targets: debttracker://login-callback
        // (registered in AndroidManifest.xml and iOS Info.plist). Ignored on JVM/Web.
        scheme = "debttracker"
        host = "login-callback"
        this.sessionManager = sessionManager
    }
    install(Postgrest)
    install(Realtime)
    install(Storage)
}
