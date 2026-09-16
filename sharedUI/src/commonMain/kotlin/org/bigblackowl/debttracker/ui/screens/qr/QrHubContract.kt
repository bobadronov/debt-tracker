package org.bigblackowl.debttracker.ui.screens.qr

import org.bigblackowl.debttracker.domain.model.ScannedContact

/** MVI contract for [QrHubScreen] — share your own contact card as a QR code, or scan someone
 * else's (via [org.bigblackowl.debttracker.ui.components.contact.ContactQrScanOverlay]). Editing
 * your own card is [EditContactCardScreen], a separate screen. */
data class QrHubState(
    val isAuthenticated: Boolean = false,
    /** null while the card (local or account) has no name yet — nothing to encode into a QR. */
    val qrPayload: String? = null,
    /** Non-null pops the "add as debtor or creditor?" chooser dialog. */
    val scannedContact: ScannedContact? = null,
)

sealed interface QrHubIntent {
    data class ScanResult(val contact: ScannedContact) : QrHubIntent
    data class ConfirmScannedContact(val asDebtor: Boolean) : QrHubIntent
    data object DismissScannedContact : QrHubIntent
}

sealed interface QrHubEffect {
    data class NavigateToAddDebtor(val contact: ScannedContact) : QrHubEffect
    data class NavigateToAddCreditor(val contact: ScannedContact) : QrHubEffect
}
