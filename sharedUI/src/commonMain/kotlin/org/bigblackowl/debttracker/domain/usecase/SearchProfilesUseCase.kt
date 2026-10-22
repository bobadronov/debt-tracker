package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.domain.repository.ProfileLookupRepository

/** Finds registered users by name/phone as they're typed in the AddEdit form, for autofill suggestions. */
class SearchProfilesUseCase(private val repository: ProfileLookupRepository) {
    suspend operator fun invoke(name: String?, phone: String?): List<ProfileSuggestion> =
        repository.searchProfiles(name, phone)
}
