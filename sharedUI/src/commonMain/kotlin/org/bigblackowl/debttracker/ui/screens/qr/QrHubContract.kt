package org.bigblackowl.debttracker.ui.screens.qr

import org.bigblackowl.debttracker.domain.model.ScannedContact

enum class QrHubMode { SHARE, SCAN }

/** MVI contract for [QrHubScreen] — share your own contact card as a QR code, or (Android/iOS only) scan someone else's. */
data class QrHubState(
    val mode: QrHubMode = QrHubMode.SHARE,
    val isAuthenticated: Boolean = false,
    /** Name/phone/email fields are only ever shown for a signed-out user (signed-in reads from the account, nothing to edit here) — this toggles them via the Edit button. */
    val fieldsExpanded: Boolean = false,
    val myName: String = "",
    val myPhone: String = "",
    val myEmail: String = "",
    /** null while [myName] is blank — nothing to encode yet. */
    val qrPayload: String? = null,
    val cameraPermissionDenied: Boolean = false,
    /** Non-null pops the "add as debtor or creditor?" chooser dialog. */
    val scannedContact: ScannedContact? = null,
)

sealed interface QrHubIntent {
    data object SwitchToScan : QrHubIntent
    data object SwitchToShare : QrHubIntent
    data object EditClicked : QrHubIntent
    data class MyNameChanged(val value: String) : QrHubIntent
    data class MyPhoneChanged(val value: String) : QrHubIntent
    data class MyEmailChanged(val value: String) : QrHubIntent
    data class ScanResult(val rawPayload: String) : QrHubIntent
    data object CameraPermissionDenied : QrHubIntent
    data class ConfirmScannedContact(val asDebtor: Boolean) : QrHubIntent
    data object DismissScannedContact : QrHubIntent
}

sealed interface QrHubEffect {
    data class NavigateToAddDebtor(val contact: ScannedContact) : QrHubEffect
    data class NavigateToAddCreditor(val contact: ScannedContact) : QrHubEffect
}
