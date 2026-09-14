package org.bigblackowl.debttracker.core.di

import org.koin.core.module.Module

/**
 * Platform-specific DI assembly (spec §1: `localSyncModule` on Android/iOS/Desktop
 * with Room+Supabase, `remoteOnlyModule` on Web with Supabase only). Populated in Phase 2.
 */
expect fun platformDataModule(): Module

/**
 * Web has no local cache (Room-backed repos are unusable without a signed-in Supabase
 * session — see [org.bigblackowl.debttracker.data.repository.SupabaseDebtorRepository]),
 * so [org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph] forces the Supabase
 * sign-in screen before [org.bigblackowl.debttracker.navigation.Screen.Home] there.
 * Android/iOS/Desktop work fully offline — Supabase auth is opt-in sync, not a gate.
 */
expect val requiresRemoteAuthGate: Boolean
