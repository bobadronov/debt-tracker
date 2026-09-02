package org.bigblackowl.debttracker.core.di

import org.bigblackowl.debttracker.core.auth.AndroidGoogleSignInLauncher
import org.bigblackowl.debttracker.core.auth.GoogleSignInLauncher
import org.bigblackowl.debttracker.core.notifications.AndroidLocalNotifier
import org.bigblackowl.debttracker.core.notifications.AndroidReminderScheduler
import org.bigblackowl.debttracker.core.notifications.LocalNotifier
import org.bigblackowl.debttracker.core.notifications.ReminderScheduler
import org.bigblackowl.debttracker.core.security.AndroidRestoreCredentialClient
import org.bigblackowl.debttracker.core.security.RestoreCredentialClient
import org.bigblackowl.debttracker.data.local.DebtTrackerDatabase
import org.bigblackowl.debttracker.data.local.buildDatabase
import org.bigblackowl.debttracker.data.repository.RoomCreditorRepository
import org.bigblackowl.debttracker.data.repository.RoomDebtorRepository
import org.bigblackowl.debttracker.data.sync.SyncCoordinator
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Android: Room-backed local storage + [SyncCoordinator] for Account+Sync. */
actual fun platformDataModule(): Module = module {
    single<DebtTrackerDatabase> { buildDatabase(androidContext()) }
    single { get<DebtTrackerDatabase>().debtorDao() }
    single { get<DebtTrackerDatabase>().debtTransactionDao() }
    single { get<DebtTrackerDatabase>().creditorDao() }
    single { get<DebtTrackerDatabase>().creditorTransactionDao() }
    single<DebtorRepository> { RoomDebtorRepository(get(), get(), get(), get()) }
    single<CreditorRepository> { RoomCreditorRepository(get(), get(), get(), get()) }
    single { SyncCoordinator(get(), get(), get(), get(), get(), get(), get()) }
    single<SyncStatusProvider> { get<SyncCoordinator>() }
    single<LocalNotifier> { AndroidLocalNotifier(androidContext()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single<RestoreCredentialClient> { AndroidRestoreCredentialClient(androidContext()) }
    single<GoogleSignInLauncher> { AndroidGoogleSignInLauncher(get()) }
}

actual val requiresRemoteAuthGate: Boolean = false
