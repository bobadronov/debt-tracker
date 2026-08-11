package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.repository.AuthRepository

/**
 * Shared by [org.bigblackowl.debttracker.ui.screens.settings.SettingsViewModel] (user-initiated sign
 * out) and [org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph] (remote sign-out via
 * [org.bigblackowl.debttracker.domain.repository.SessionRepository.revokedElsewhere]) — local data
 * belongs to whichever account is signed in, so it must go before the next account can sign in on
 * this device.
 */
class ForceSignOutUseCase(
    private val deleteAllData: DeleteAllDataUseCase,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        deleteAllData()
        authRepository.signOut()
    }
}
