package org.bigblackowl.debttracker.ui.screens.qr

/** MVI contract for [EditContactCardScreen] — the locally-saved "my card" (name/phone/email)
 * shown as a QR code on [QrHubScreen] while signed out. Explicit Save (not instant-persist per
 * keystroke), guarded by [org.bigblackowl.debttracker.ui.components.UnsavedChangesGuard] so
 * leaving with a pending edit prompts to save it first. */
data class EditContactCardState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    /** True once any field differs from what was loaded — drives [org.bigblackowl.debttracker.ui.components.UnsavedChangesGuard]. */
    val hasUnsavedChanges: Boolean = false,
)

sealed interface EditContactCardIntent {
    data class NameChanged(val value: String) : EditContactCardIntent
    data class PhoneChanged(val value: String) : EditContactCardIntent
    data class EmailChanged(val value: String) : EditContactCardIntent
    data object Save : EditContactCardIntent
}

sealed interface EditContactCardEffect {
    data object Saved : EditContactCardEffect
}
