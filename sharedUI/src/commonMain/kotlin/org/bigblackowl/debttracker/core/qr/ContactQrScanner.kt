package org.bigblackowl.debttracker.core.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Camera QR scanner (QRKit's `QrScanner`, `qrMain` source set — Android/iOS/Desktop only).
 * Camera-permission request, rationale and settings-redirect are handled internally by the
 * underlying platform scanner; [permissionDeniedContent] only overrides what's shown while
 * denied. [onResult] fires once per decoded code from the live camera feed; QRKit's own
 * `openImagePicker` is disabled everywhere (its image-picker sheet crashes against this project's
 * pinned Material3 version — see the `openImagePicker = false` actuals), so [onImageDecodeFailure]
 * never actually fires. Only ever composed on [QR_SCAN_CAPABLE_PLATFORMS] — Desktop/Web use
 * [ContactQrFilePickerContent] (a locally-picked image, decoded without a camera) instead; this
 * expect still needs a Desktop/Web actual to satisfy the compiler, but neither is ever invoked.
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
