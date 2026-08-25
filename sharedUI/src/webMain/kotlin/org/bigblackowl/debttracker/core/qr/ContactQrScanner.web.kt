package org.bigblackowl.debttracker.core.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** QRKit has no js artifact and its wasmJs `QrScanner` is an empty no-op stub that never fires
 * `onCompletion` — Web never reaches this actual (QR_SCAN_CAPABLE_PLATFORMS gates it out in favor
 * of [ContactQrFilePickerContent]'s local-file decode), so this exists only to satisfy the
 * compiler. */
@Composable
actual fun ContactQrScanner(
    description: String,
    modifier: Modifier,
    flashlightOn: Boolean,
    onResult: (String) -> Unit,
    onImageDecodeFailure: (String) -> Unit,
    permissionDeniedContent: @Composable () -> Unit,
) {
}
