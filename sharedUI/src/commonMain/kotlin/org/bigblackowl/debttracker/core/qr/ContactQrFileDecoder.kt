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
 * live camera feed on [QR_SCAN_CAPABLE_PLATFORMS]. Desktop decodes via ZXing; Web via the
 * browser's native BarcodeDetector API (Chrome/Edge only as of this writing — see the Web actual
 * for the unsupported-browser fallback). Android/iOS never call this (they scan with the camera
 * instead), so their actuals only exist to satisfy the compiler.
 */
expect suspend fun decodeQrFromImage(bytes: ByteArray): QrDecodeResult
