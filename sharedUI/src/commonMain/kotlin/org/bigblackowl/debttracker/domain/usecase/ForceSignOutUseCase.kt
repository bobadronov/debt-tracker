package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.repository.AuthRepository

/**
 * Shared by [org.bigblackowl.debttracker.ui.screens.settings.SettingsViewModel] (user-initiated sign
 * out) and [org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph] (remote sign-out via
 * [org.bigblackowl.debttracker.domain.repository.SessionRepository.revokedElsewhere]) — local cache
 * belongs to whichever account is signed in, so it must go before the next account can sign in on
 * this device. Uses [ClearLocalCacheUseCase], not [DeleteAllDataUseCase] — signing out must never
 * delete the account's actual data (that would be catastrophic on Web, which has no local cache and
 * would otherwise route straight to a real Supabase delete).
 */
class ForceSignOutUseCase(
    private val clearLocalCache: ClearLocalCacheUseCase,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        clearLocalCache()
        authRepository.signOut()
    }
}
