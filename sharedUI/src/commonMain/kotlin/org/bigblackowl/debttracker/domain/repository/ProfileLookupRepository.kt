package org.bigblackowl.debttracker.domain.repository

import org.bigblackowl.debttracker.domain.model.ProfileSuggestion

/** Looks up a registered user by email (AddEdit debtor/creditor form → autofill). */
interface ProfileLookupRepository {
    /** null if there's no match, no active session (Local-only), or the lookup fails — never throws. */
    suspend fun findProfileByEmail(email: String): ProfileSuggestion?

    /** Registered users matching a phone (exact) or a full name (2+ words); empty on no match / Local-only / failure. */
    suspend fun searchProfiles(name: String?, phone: String?): List<ProfileSuggestion>
}
