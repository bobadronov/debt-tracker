package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/** SettingsScreen "Видалити всі дані" — реальний DELETE, обидва напрямки (спек §9.2). */
class DeleteAllDataUseCase(
    private val debtorRepository: DebtorRepository,
    private val creditorRepository: CreditorRepository,
) {
    suspend operator fun invoke() {
        debtorRepository.deleteAllData()
        creditorRepository.deleteAllData()
    }
}
