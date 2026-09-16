package org.bigblackowl.debttracker.core.qr

/** Outcome of decoding a locally-picked image via [decodeQrFromImage]. */
sealed interface QrDecodeResult {
    data class Success(val payload: String) : QrDecodeResult
    /** The image decoded fine, but no QR code was found in it. */
    data object NotFound : QrDecodeResult
    /** This platform/browser has no decoder available at all (see the Web actual). */
    data object Unsupported : QrDecodeResult
}

/**
 * Decodes a QR code out of a picked image file — [ContactQrFilePickerContent]'s counterpart to the
 * live camera feed on [QR_SCAN_CAPABLE_PLATFORMS]. Desktop/Android decode via ZXing; iOS via
 * CoreImage's native [platform.CoreImage.CIDetector]; Web via the browser's native BarcodeDetector
 * API (Chrome/Edge only as of this writing — see the Web actual for the unsupported-browser
 * fallback). On Android/iOS this is a secondary "select image" action next to the camera scanner
 * (see [org.bigblackowl.debttracker.ui.components.contact.ContactQrScanOverlay]), not the primary
 * way to scan — Desktop/Web have no camera at all, so it's their only way.
 */
expect suspend fun decodeQrFromImage(bytes: ByteArray): QrDecodeResult
