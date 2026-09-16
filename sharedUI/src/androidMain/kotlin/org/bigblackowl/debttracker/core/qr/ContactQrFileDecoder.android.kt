package org.bigblackowl.debttracker.core.qr

import android.graphics.BitmapFactory
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ZXing `core` (pure Java, no `android.graphics`/AWT dependency of its own) — same library and
 * approach as Desktop's [decodeQrFromImage] actual, just fed pixels via [android.graphics.Bitmap]
 * instead of [java.awt.image.BufferedImage]. Reached from [ContactQrFilePickerContent] as the
 * "select image" fallback next to the camera scanner (see [QR_SCAN_CAPABLE_PLATFORMS] doc).
 *
 * [DecodeHintType.TRY_HARDER] matters more than usual here: this app's own QR codes
 * ([org.bigblackowl.debttracker.core.qr.rememberContactQrPainter]) use rounded/separated pixel
 * shapes rather than plain abutting squares, which without it ZXing's default single-pass grid
 * sampling frequently misreads as noise — confirmed by hand: a same-account contact QR reliably
 * failed to decode without this hint, even tightly cropped with nothing else in frame.
 */
private val HINTS = mapOf(
    DecodeHintType.TRY_HARDER to true,
    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
)

actual suspend fun decodeQrFromImage(bytes: ByteArray): QrDecodeResult = withContext(Dispatchers.Default) {
    val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        ?: return@withContext QrDecodeResult.NotFound
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val bitmapForDecode = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))
    runCatching { MultiFormatReader().decode(bitmapForDecode, HINTS) }.getOrNull()
        ?.let { QrDecodeResult.Success(it.text) }
        ?: QrDecodeResult.NotFound
}
