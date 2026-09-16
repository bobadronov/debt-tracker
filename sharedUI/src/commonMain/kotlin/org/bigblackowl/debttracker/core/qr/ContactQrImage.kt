package org.bigblackowl.debttracker.core.qr

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import debt_tracker.sharedui.generated.resources.Res
import debt_tracker.sharedui.generated.resources.ic_app_logo
import org.jetbrains.compose.resources.painterResource
import qrgenerator.qrkitpainter.QrKitBallShape
import qrgenerator.qrkitpainter.QrKitBrush
import qrgenerator.qrkitpainter.QrKitColors
import qrgenerator.qrkitpainter.QrKitFrameShape
import qrgenerator.qrkitpainter.QrKitLogo
import qrgenerator.qrkitpainter.QrKitLogoKitShape
import qrgenerator.qrkitpainter.QrKitLogoPadding
import qrgenerator.qrkitpainter.QrKitPixelShape
import qrgenerator.qrkitpainter.QrKitShapes
import qrgenerator.qrkitpainter.createRoundCorners
import qrgenerator.qrkitpainter.rememberQrKitPainter
import qrgenerator.qrkitpainter.solidBrush

/**
 * Renders [data] (see [org.bigblackowl.debttracker.domain.model.ContactQrPayload]) as a QR code
 * image via QRKit's `qrkitpainter` — the same library already used for the camera scanner (see
 * [ContactQrScanner]), so this needs no second QR dependency. It's pure Compose vector drawing (no
 * platform bitmap APIs, unlike QRKit's scanner or its older bitmap-based `QRCodeImage`), so one
 * implementation covers every platform, Web included, with no expect/actual.
 *
 * This config is verified end-to-end against ZXing (`DecodeHintType.TRY_HARDER`), rendered through
 * Compose's real Skia pipeline via `runComposeUiTest` + `captureToImage()` — not just eyeballed. An
 * earlier attempt to verify this by screenshotting an Android emulator repeatedly failed to decode
 * (even a completely unstyled QRKit/qrose code did); that turned out to be the emulator's
 * SwiftShader software-rendered display corrupting fine module edges, not a library bug — the
 * headless Skia test decodes every config (styled QRKit, plain QRKit, qrose) without issue. Don't
 * re-diagnose this as a generator bug from an emulator screenshot alone; re-run it headless first.
 *
 * QrKitColors' unset brushes are transparent — the screen background shows through, so on a dark
 * background the default black dark-module color becomes unreadable. The module color flips to
 * white on a dark background to keep the code scannable in both themes. The app icon sits centered
 * on top; QrKitErrorCorrection.Auto (the default) bumps the correction level to compensate for the
 * obscured modules once it sees a non-empty logo, so the code stays scannable.
 */
@Composable
fun rememberContactQrPainter(data: String): Painter? {
    if (data.isBlank()) return null
    val moduleColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black
    val brush = QrKitBrush.solidBrush(moduleColor)
    val logoPainter = painterResource(Res.drawable.ic_app_logo)
    return rememberQrKitPainter(data = data, moduleColor) {
        logo = QrKitLogo(
            painter = logoPainter,
            size = 0.15f,
            padding = QrKitLogoPadding.Natural(.15f),
            shape = QrKitLogoKitShape.createRoundCorners(.25f),
        )
        shapes = QrKitShapes(
            darkPixelShape = QrKitPixelShape.createRoundCorners(),
            ballShape = QrKitBallShape.createRoundCorners(.1f, bottomRight = false),
            frameShape = QrKitFrameShape.createRoundCorners(.1f, bottomRight = false),
            hasCentralSymmetry = true,
        )
        colors = QrKitColors(darkBrush = brush, ballBrush = brush, frameBrush = brush)
    }
}
