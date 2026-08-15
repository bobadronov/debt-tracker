package org.bigblackowl.debttracker.core.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

/** QRKit has no js artifact and its wasmJs `QrScanner` is an empty no-op stub that never fires
 * `onCompletion` — the QR feature is hidden from Web's UI entirely (see currentPlatform gate in
 * HomeScreen), so these actuals exist only to satisfy the compiler and are never invoked. */
@Composable
actual fun rememberContactQrPainter(data: String): Painter? = null

@Composable
actual fun ContactQrScanner(
    modifier: Modifier,
    flashlightOn: Boolean,
    onResult: (String) -> Unit,
    onImageDecodeFailure: (String) -> Unit,
    permissionDeniedContent: @Composable () -> Unit,
) {
}
