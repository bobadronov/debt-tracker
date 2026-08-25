package org.bigblackowl.debttracker.core.qr

import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

/**
 * Web has no QRKit scanner at all (see [ContactQrScanner]), so [ContactQrFilePickerContent] decodes
 * a picked image via the browser's native BarcodeDetector API instead of bundling a JS decode
 * library — as of this writing that's Chrome/Edge only (no Firefox, no Safari), hence
 * [QrDecodeResult.Unsupported] as a real, expected outcome here rather than a should-never-happen
 * case. [bytes] crosses into the JS snippet as base64 (`atob` back to a `Uint8Array` there) since
 * that's the one representation both the js() and wasmJs() Kotlin backends pass through uniformly —
 * a raw `ByteArray` doesn't share a single interop story between the two.
 */
@OptIn(ExperimentalEncodingApi::class)
private fun detectQrCodeBase64(base64: String): Promise<JsString> = js(
    """
    (function () {
        if (typeof BarcodeDetector === 'undefined') return Promise.resolve('unsupported');
        var binary = atob(base64);
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        var blob = new Blob([bytes], { type: 'image/*' });
        return createImageBitmap(blob).then(function (bitmap) {
            var detector = new BarcodeDetector({ formats: ['qr_code'] });
            return detector.detect(bitmap);
        }).then(function (codes) {
            return codes.length === 0 ? 'notfound' : 'found:' + codes[0].rawValue;
        }).catch(function (e) {
            return 'notfound';
        });
    })()
    """
)

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun decodeQrFromImage(bytes: ByteArray): QrDecodeResult {
    val base64 = Base64.encode(bytes)
    val result = runCatching { detectQrCodeBase64(base64).await().toString() }.getOrElse { return QrDecodeResult.NotFound }
    return when {
        result == "unsupported" -> QrDecodeResult.Unsupported
        result.startsWith("found:") -> QrDecodeResult.Success(result.removePrefix("found:"))
        else -> QrDecodeResult.NotFound
    }
}
