package org.bigblackowl.debttracker.domain.model

/** Contact card decoded from a scanned QR code (see [ContactQrPayload]) — feeds a debtor/creditor Add form. */
data class ScannedContact(
    val fullName: String,
    val phone: String?,
    val email: String?,
)
