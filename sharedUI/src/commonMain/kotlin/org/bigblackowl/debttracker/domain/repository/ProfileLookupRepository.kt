package org.bigblackowl.debttracker.domain.repository

import org.bigblackowl.debttracker.domain.model.ProfileSuggestion

/** Пошук зареєстрованого користувача за email (AddEdit-форма боржника/кредитора → автозаповнення). */
interface ProfileLookupRepository {
    /** null якщо збігів немає, немає активної сесії (Local-only) або пошук не вдався — ніколи не кидає виняток. */
    suspend fun findProfileByEmail(email: String): ProfileSuggestion?
}
