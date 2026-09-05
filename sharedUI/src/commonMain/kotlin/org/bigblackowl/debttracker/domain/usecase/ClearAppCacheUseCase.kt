package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider

/**
 * Settings → Data "Очистити кеш" — wipes only this device's offline cache (Room), unlike
 * [DeleteAllDataUseCase] which really deletes Supabase data too. Safe only while signed in
 * (Account+Sync), where Supabase remains the source of truth: immediately re-downloads
 * everything via [SyncStatusProvider.refetchAll] afterwards, since the ongoing Realtime pull only
 * re-emits on the *next* remote change, not on our own local delete — without this the list would
 * just look empty until something else happened to sync.
 *
 * Not offered to local-only users (see SettingsDataScreen) — for them the local cache IS their
 * only copy of the data, so clearing it would silently be [DeleteAllDataUseCase] without its
 * double confirmation.
 */
class ClearAppCacheUseCase(
    private val debtorRepository: DebtorRepository,
    private val creditorRepository: CreditorRepository,
    private val authRepository: AuthRepository,
    private val syncStatusProvider: SyncStatusProvider,
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        debtorRepository.clearLocalCache()
        creditorRepository.clearLocalCache()
        if (authRepository.isAuthenticated.value) syncStatusProvider.refetchAll()
    }
}
