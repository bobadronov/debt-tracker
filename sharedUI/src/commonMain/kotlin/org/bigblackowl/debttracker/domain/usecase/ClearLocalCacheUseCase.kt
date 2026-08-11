package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/**
 * Sign-out cleanup — wipes only this device's local cache, unlike [DeleteAllDataUseCase] which
 * really deletes Supabase data too. On Room platforms that's the offline cache (so the next
 * account signing in on this device doesn't see stale data); on Web there's no local cache, so
 * it's a no-op there and Supabase data survives sign-out as expected.
 */
class ClearLocalCacheUseCase(
    private val debtorRepository: DebtorRepository,
    private val creditorRepository: CreditorRepository,
) {
    suspend operator fun invoke() {
        debtorRepository.clearLocalCache()
        creditorRepository.clearLocalCache()
    }
}
