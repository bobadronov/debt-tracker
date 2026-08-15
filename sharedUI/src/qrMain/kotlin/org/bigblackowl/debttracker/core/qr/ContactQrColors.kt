package org.bigblackowl.debttracker.core.qr

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import qrgenerator.qrkitpainter.QrKitBrush
import qrgenerator.qrkitpainter.QrKitColors
import qrgenerator.qrkitpainter.solidBrush

/**
 * QRKit's default dark module color is opaque black with a *transparent* light module (see
 * qr-kit's `QrKitColors` defaults) — the screen background shows through the light modules, so on
 * a dark background the black modules (data pixels + the three corner eyes) become unreadable.
 * Flips the module color to white on dark backgrounds so the code stays scannable in both themes.
 */
@Composable
internal fun rememberContactQrColors(): QrKitColors {
    val moduleColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black
    return QrKitColors(
        darkBrush = QrKitBrush.solidBrush(moduleColor),
        ballBrush = QrKitBrush.solidBrush(moduleColor),
        frameBrush = QrKitBrush.solidBrush(moduleColor),
    )
}
