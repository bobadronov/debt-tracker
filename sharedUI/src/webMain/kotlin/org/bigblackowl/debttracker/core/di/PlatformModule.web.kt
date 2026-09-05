package org.bigblackowl.debttracker.core.di

import kotlinx.coroutines.flow.MutableStateFlow
import org.bigblackowl.debttracker.core.auth.GoogleSignInLauncher
import org.bigblackowl.debttracker.core.auth.WebGoogleSignInLauncher
import org.bigblackowl.debttracker.core.notifications.InProcessReminderScheduler
import org.bigblackowl.debttracker.core.notifications.LocalNotifier
import org.bigblackowl.debttracker.core.notifications.ReminderScheduler
import org.bigblackowl.debttracker.core.notifications.WebLocalNotifier
import org.bigblackowl.debttracker.core.security.RestoreCredentialClient
import org.bigblackowl.debttracker.core.security.UnsupportedRestoreCredentialClient
import org.bigblackowl.debttracker.data.repository.SupabaseCreditorRepository
import org.bigblackowl.debttracker.data.repository.SupabaseDebtorRepository
import org.bigblackowl.debttracker.domain.model.SyncUiStatus
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Web: Room has no wasmJs/js target (спек §1), so `DebtorRepository`/`CreditorRepository` are
 * online-only Supabase-backed implementations with no local cache, unlike Android/iOS/Desktop's
 * Room-backed offline-first ones. `SyncStatusProvider` is a stub always reporting
 * [SyncUiStatus.Synced] since there's no pending-sync queue to report on here.
 */
actual fun platformDataModule(): Module = module {
    single<DebtorRepository> { SupabaseDebtorRepository(get(), get()) }
    single<CreditorRepository> { SupabaseCreditorRepository(get(), get()) }
    single<SyncStatusProvider> {
        object : SyncStatusProvider {
            override val status = MutableStateFlow<SyncUiStatus>(SyncUiStatus.Synced)
            override suspend fun refreshNow() {}
            override suspend fun refetchAll() {}
        }
    }
    single<LocalNotifier> { WebLocalNotifier() }
    single<ReminderScheduler> { InProcessReminderScheduler(get(), get()) }
    single<RestoreCredentialClient> { UnsupportedRestoreCredentialClient }
    single<GoogleSignInLauncher> { WebGoogleSignInLauncher(get()) }
}

actual val requiresRemoteAuthGate: Boolean = true
