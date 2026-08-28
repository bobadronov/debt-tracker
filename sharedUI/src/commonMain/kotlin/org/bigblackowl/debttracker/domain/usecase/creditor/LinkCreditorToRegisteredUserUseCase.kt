package org.bigblackowl.debttracker.domain.usecase.creditor

import org.bigblackowl.debttracker.domain.repository.CreditorRepository

/** Дзеркало [org.bigblackowl.debttracker.domain.usecase.debtor.LinkDebtorToRegisteredUserUseCase] для кредиторів. */
class LinkCreditorToRegisteredUserUseCase(private val repository: CreditorRepository) {
    suspend operator fun invoke(creditorId: String): String? = repository.linkToRegisteredUser(creditorId)
}
