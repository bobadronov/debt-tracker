package org.bigblackowl.debttracker.core.qr

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorAccuracy
import platform.CoreImage.CIDetectorAccuracyHigh
import platform.CoreImage.CIDetectorTypeQRCode
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

/**
 * [CIDetector] (CoreImage) — no third-party dependency, unlike Desktop/Android's ZXing: Apple ships
 * a native QR detector, so there's nothing to add for this to work on iOS.
 *
 * Wrapped in [runCatching]: `CIImage(data:)`'s Kotlin binding is typed non-nullable despite the
 * underlying `-initWithData:` being able to return nil for unparsable image bytes at the Objective-C
 * level — a mismatch between the header's (incorrect) nullability annotation and its real runtime
 * behavior, not something the Kotlin type system can catch for us here.
 */
actual suspend fun decodeQrFromImage(bytes: ByteArray): QrDecodeResult = runCatching {
    val ciImage = CIImage(data = bytes.toNSData())
    val detector = CIDetector.detectorOfType(
        CIDetectorTypeQRCode,
        context = null,
        options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh),
    ) ?: return QrDecodeResult.Unsupported
    val message = detector.featuresInImage(ciImage)
        .filterIsInstance<CIQRCodeFeature>()
        .firstOrNull()
        ?.messageString
    message?.let { QrDecodeResult.Success(it) } ?: QrDecodeResult.NotFound
}.getOrDefault(QrDecodeResult.NotFound)
