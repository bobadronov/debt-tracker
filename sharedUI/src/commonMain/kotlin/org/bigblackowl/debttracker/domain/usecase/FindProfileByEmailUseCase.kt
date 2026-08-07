package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.domain.repository.ProfileLookupRepository

/** Looks up a registered user's public profile by email, for AddEdit-form autofill suggestions. */
class FindProfileByEmailUseCase(private val repository: ProfileLookupRepository) {
    suspend operator fun invoke(email: String): ProfileSuggestion? = repository.findProfileByEmail(email)
}
