package org.bigblackowl.debttracker.domain.usecase.debtor

import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/**
 * Links a just-saved debtor to a registered user by phone/email, if a match is found
 * (RPC `link_debtor_to_registered_user`, idempotent) — mirrors existing transactions
 * into that user's account and sends them a notification. Fire-and-forget from the
 * caller's side: an error or missing match must never block saving the debtor.
 */
class LinkDebtorToRegisteredUserUseCase(private val repository: DebtorRepository) {
    suspend operator fun invoke(debtorId: String): String? = repository.linkToRegisteredUser(debtorId)
}
