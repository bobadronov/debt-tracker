package org.bigblackowl.debttracker.core.di

import org.bigblackowl.debttracker.core.auth.DesktopGoogleSignInLauncher
import org.bigblackowl.debttracker.core.auth.GoogleSignInLauncher
import org.bigblackowl.debttracker.core.notifications.DesktopLocalNotifier
import org.bigblackowl.debttracker.core.notifications.InProcessReminderScheduler
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

/** Desktop: Room-backed local storage + [SyncCoordinator] for Account+Sync — identical wiring to Android/iOS. */
actual fun platformDataModule(): Module = module {
    single<DebtTrackerDatabase> { buildDatabase() }
    single { get<DebtTrackerDatabase>().debtorDao() }
    single { get<DebtTrackerDatabase>().debtTransactionDao() }
    single { get<DebtTrackerDatabase>().creditorDao() }
    single { get<DebtTrackerDatabase>().creditorTransactionDao() }
    single<DebtorRepository> { RoomDebtorRepository(get(), get(), get(), get()) }
    single<CreditorRepository> { RoomCreditorRepository(get(), get(), get(), get()) }
    single { SyncCoordinator(get(), get(), get(), get(), get(), get(), get()) }
    single<SyncStatusProvider> { get<SyncCoordinator>() }
    single<LocalNotifier> { DesktopLocalNotifier() }
    single<ReminderScheduler> { InProcessReminderScheduler(get(), get()) }
    single<RestoreCredentialClient> { UnsupportedRestoreCredentialClient }
    single<GoogleSignInLauncher> { DesktopGoogleSignInLauncher(get()) }
}

actual val requiresRemoteAuthGate: Boolean = false
