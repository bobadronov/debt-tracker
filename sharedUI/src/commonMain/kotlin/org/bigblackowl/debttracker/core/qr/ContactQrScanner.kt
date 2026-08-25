package org.bigblackowl.debttracker.core.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Camera QR scanner (QRKit's `QrScanner`, `qrMain` source set — Android/iOS/Desktop only).
 * Camera-permission request, rationale and settings-redirect are handled internally by the
 * underlying platform scanner; [permissionDeniedContent] only overrides what's shown while
 * denied. [onResult] fires once per decoded code (from the live camera feed or, since QRKit's
 * `openImagePicker` is enabled, a picked gallery image); [onImageDecodeFailure] fires only for the
 * latter path, when the picked image doesn't contain a decodable code.
 * No-op stub on Web — see [rememberContactQrPainter].
 */
@Composable
expect fun ContactQrScanner(
    description: String,
    modifier: Modifier,
    flashlightOn: Boolean,
    onResult: (String) -> Unit,
    onImageDecodeFailure: (String) -> Unit,
    permissionDeniedContent: @Composable () -> Unit,
)
