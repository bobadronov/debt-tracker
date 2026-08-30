package org.bigblackowl.debttracker.core.qr

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import debt_tracker.sharedui.generated.resources.Res
import debt_tracker.sharedui.generated.resources.ic_app_logo
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrLogoPadding
import io.github.alexzhirkevich.qrose.options.QrLogoShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.painterResource

/**
 * Renders [data] (see [org.bigblackowl.debttracker.domain.model.ContactQrPayload]) as a QR code
 * image via qrose — pure Kotlin, so unlike QRKit (used for the camera scanner, see
 * [ContactQrScanner]) it needs no expect/actual: this one implementation covers every platform,
 * Web included.
 *
 * QrColors' default light module is transparent — the screen background shows through, so on a
 * dark background the default black dark-module color becomes unreadable. The module color flips
 * to white on a dark background to keep the code scannable in both themes. The app icon sits
 * centered on top; QrErrorCorrectionLevel.Auto (the default) bumps the correction level to
 * compensate for the obscured modules once it sees a non-empty logo, so the code stays scannable.
 */
@Composable
fun rememberContactQrPainter(data: String): Painter? {
    if (data.isBlank()) return null
    val moduleColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black
    val brush = QrBrush.solid(moduleColor)
    val logoPainter = painterResource(Res.drawable.ic_app_logo)
    return rememberQrCodePainter(data = data) {
        logo {
            painter = logoPainter
            padding = QrLogoPadding.Natural(.15f)
            shape = QrLogoShape.roundCorners(.25f)
            size = 0.15f
        }
        shapes(centralSymmetry = true) {
            ball = QrBallShape.roundCorners(.1f, bottomRight = false)
            darkPixel = QrPixelShape.roundCorners()
            frame = QrFrameShape.roundCorners(.1f, bottomRight = false)
        }
        colors {
            dark = brush
            ball = brush
            frame = brush
        }
    }
}
