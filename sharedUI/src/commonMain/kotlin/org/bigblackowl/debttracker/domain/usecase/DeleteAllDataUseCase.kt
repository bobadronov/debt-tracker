package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** SettingsScreen "Видалити всі дані" — реальний DELETE, обидва напрямки (спек §9.2).
 *
 * Not atomic across the two repositories (they may be backed by separate network calls on Web) —
 * if the debtor delete succeeds and the creditor delete then fails, the account is left partially
 * cleared. [Result.failure] surfaces that to the caller instead of throwing uncaught, so the app
 * doesn't crash and the user can retry rather than being left in a silently inconsistent state. */
class DeleteAllDataUseCase(
    private val debtorRepository: DebtorRepository,
    private val creditorRepository: CreditorRepository,
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        debtorRepository.deleteAllData()
        creditorRepository.deleteAllData()
    }
}
