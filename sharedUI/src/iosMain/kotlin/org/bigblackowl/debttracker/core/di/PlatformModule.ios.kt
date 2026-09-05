package org.bigblackowl.debttracker.core.di

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import org.bigblackowl.debttracker.core.auth.GoogleSignInLauncher
import org.bigblackowl.debttracker.core.auth.IosGoogleSignInLauncher
import org.bigblackowl.debttracker.core.notifications.IosLocalNotifier
import org.bigblackowl.debttracker.core.notifications.IosReminderScheduler
import org.bigblackowl.debttracker.core.notifications.LocalNotifier
import org.bigblackowl.debttracker.core.notifications.ReminderScheduler
import org.bigblackowl.debttracker.core.security.RestoreCredentialClient
import org.bigblackowl.debttracker.core.security.UnsupportedRestoreCredentialClient
import org.bigblackowl.debttracker.data.local.DebtTrackerDatabase
import org.bigblackowl.debttracker.data.local.buildDatabase
import org.bigblackowl.debttracker.data.repository.RoomCreditorRepository
import org.bigblackowl.debttracker.data.repository.RoomDebtorRepository
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.koin.core.module.Module
import org.koin.dsl.module

/** iOS: Room-backed local storage + [SyncCoordinator] for Account+Sync — identical wiring to Android. */
@OptIn(ExperimentalSettingsImplementation::class)
actual fun platformDataModule(): Module = module {
    single<DebtTrackerDatabase> { buildDatabase() }
    single { get<DebtTrackerDatabase>().debtorDao() }
    single { get<DebtTrackerDatabase>().debtTransactionDao() }
    single { get<DebtTrackerDatabase>().creditorDao() }
    single { get<DebtTrackerDatabase>().creditorTransactionDao() }
    single<DebtorRepository> { RoomDebtorRepository(get(), get(), get(), get()) }
    single<CreditorRepository> { RoomCreditorRepository(get(), get(), get(), get()) }
    single { SyncCoordinator(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<SyncStatusProvider> { get<SyncCoordinator>() }
    single<LocalNotifier> { IosLocalNotifier() }
    single<ReminderScheduler> { IosReminderScheduler() }
    single<RestoreCredentialClient> { UnsupportedRestoreCredentialClient }
    single<GoogleSignInLauncher> { IosGoogleSignInLauncher(get()) }
    // Keychain-backed Supabase session (JWT + refresh token) — Keychain items are already
    // OS-encrypted (tied to the device passcode/Secure Enclave), no extra cipher layer needed.
    single<SessionManager> { SettingsSessionManager(settings = KeychainSettings("org.bigblackowl.debttracker.session")) }
}

actual val requiresRemoteAuthGate: Boolean = false
