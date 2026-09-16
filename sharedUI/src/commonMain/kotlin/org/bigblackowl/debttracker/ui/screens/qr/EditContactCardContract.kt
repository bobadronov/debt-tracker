package org.bigblackowl.debttracker.ui.screens.qr

/** MVI contract for [EditContactCardScreen] — the locally-saved "my card" (name/phone/email)
 * shown as a QR code on [QrHubScreen] while signed out. No explicit Save: every field change
 * persists immediately to [org.bigblackowl.debttracker.core.settings.AppSettings], same as every
 * other settings screen in the app — leaving the screen (back) is the only "done" gesture needed. */
data class EditContactCardState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
)

sealed interface EditContactCardIntent {
    data class NameChanged(val value: String) : EditContactCardIntent
    data class PhoneChanged(val value: String) : EditContactCardIntent
    data class EmailChanged(val value: String) : EditContactCardIntent
}
