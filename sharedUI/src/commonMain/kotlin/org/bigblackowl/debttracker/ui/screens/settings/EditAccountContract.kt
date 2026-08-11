package org.bigblackowl.debttracker.ui.screens.settings

import org.bigblackowl.debttracker.core.media.PickedImage

/** MVI contract for [EditAccountScreen] — edit the signed-in user's own name/phone/avatar; email is read-only here. */
data class EditAccountState(
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val avatarUrl: String? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val fullNameError: String? = null,
    val avatarError: String? = null,
    val error: String? = null,
)

sealed interface EditAccountIntent {
    data class FullNameChanged(val value: String) : EditAccountIntent
    data class PhoneChanged(val value: String) : EditAccountIntent
    data class AvatarPicked(val picked: PickedImage) : EditAccountIntent
    data object Save : EditAccountIntent
}

sealed interface EditAccountEffect {
    data object Saved : EditAccountEffect
}
