package org.bigblackowl.debttracker.core.qr

/** Android scans with the camera (see [QR_SCAN_CAPABLE_PLATFORMS]) — never actually invoked. */
actual suspend fun decodeQrFromImage(bytes: ByteArray): QrDecodeResult = QrDecodeResult.Unsupported
