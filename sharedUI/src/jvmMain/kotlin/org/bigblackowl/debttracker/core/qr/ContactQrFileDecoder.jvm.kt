package org.bigblackowl.debttracker.core.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * `com.google.zxing:core` decodes from a [LuminanceSource] but has no AWT dependency, so unlike
 * ZXing's own `javase` module (which drags in an unrelated CLI-args library for its demo tool),
 * reading a [BufferedImage]'s pixels into one is done by hand here — the same fixed-point
 * RGB→luminance weights ZXing's own `BufferedImageLuminanceSource` uses.
 */
private class BufferedImageLuminanceSource(image: BufferedImage) : LuminanceSource(image.width, image.height) {
    private val pixels = IntArray(image.width * image.height).also {
        image.getRGB(0, 0, image.width, image.height, it, 0, image.width)
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val out = row?.takeIf { it.size >= width } ?: ByteArray(width)
        val offset = y * width
        for (x in 0 until width) out[x] = luminance(pixels[offset + x])
        return out
    }

    override fun getMatrix(): ByteArray = ByteArray(width * height) { luminance(pixels[it]) }

    private fun luminance(rgb: Int): Byte {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return ((r * 306 + g * 601 + b * 117) shr 10).toByte()
    }
}

actual suspend fun decodeQrFromImage(bytes: ByteArray): QrDecodeResult = withContext(Dispatchers.Default) {
    val image = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()
        ?: return@withContext QrDecodeResult.NotFound
    val bitmap = BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(image)))
    runCatching { MultiFormatReader().decode(bitmap) }.getOrNull()
        ?.let { QrDecodeResult.Success(it.text) }
        ?: QrDecodeResult.NotFound
}
