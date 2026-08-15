package org.bigblackowl.debttracker.core.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Renders [data] (see [org.bigblackowl.debttracker.domain.model.ContactQrPayload]) as a QR code
 * image via QRKit (`qrMain` source set — Android/iOS/Desktop only). null on Web, where QRKit
 * publishes no working scanner (see [ContactQrScanner]) so the whole QR feature is hidden from
 * the UI — this actual exists only to satisfy the compiler, never invoked there.
 */
@Composable
expect fun rememberContactQrPainter(data: String): Painter?
