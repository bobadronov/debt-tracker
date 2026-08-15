package org.bigblackowl.debttracker.core.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import qrgenerator.qrkitpainter.rememberQrKitPainter

@Composable
actual fun rememberContactQrPainter(data: String): Painter? {
    if (data.isBlank()) return null
    val colors = rememberContactQrColors()
    return rememberQrKitPainter(data, colors) { this.colors = colors }
}
