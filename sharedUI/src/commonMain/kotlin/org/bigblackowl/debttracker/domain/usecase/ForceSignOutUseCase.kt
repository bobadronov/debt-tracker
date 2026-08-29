package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.RestoreCredentialGateway

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
    private val restoreCredentials: RestoreCredentialGateway,
) {
    suspend operator fun invoke() {
        clearLocalCache()
        // Drop this device's OS restore key too — otherwise the next cold start would silently
        // sign the same account back in, defeating the sign-out.
        restoreCredentials.clear()
        authRepository.signOut()
    }
}
