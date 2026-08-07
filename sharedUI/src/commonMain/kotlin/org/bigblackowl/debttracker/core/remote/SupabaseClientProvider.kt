package org.bigblackowl.debttracker.core.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import org.bigblackowl.debttracker.BuildConfig

/**
 * supabase-kt клієнт (спек §1.1) — проєкт "debt-tracker" (nywvasgnbgnixfjzadbu),
 * створений і мігрований у Фазі 6 через Supabase MCP. Ktor-engine підбирається
 * автоматично з класпату кожної платформи (okhttp — Android/Desktop, darwin — iOS).
 */
fun createAppSupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}
