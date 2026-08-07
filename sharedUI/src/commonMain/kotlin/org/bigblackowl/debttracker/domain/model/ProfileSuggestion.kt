package org.bigblackowl.debttracker.domain.model

/** Публічні дані зареєстрованого користувача, знайденого за email — для автозаповнення форми боржника/кредитора. */
data class ProfileSuggestion(
    val displayName: String?,
    val avatarUrl: String?,
)
