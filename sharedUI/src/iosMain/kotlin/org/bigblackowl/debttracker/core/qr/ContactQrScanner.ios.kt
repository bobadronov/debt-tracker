package org.bigblackowl.debttracker.core.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import qrscanner.CameraLens
import qrscanner.QrScanner

@Composable
actual fun ContactQrScanner(
    modifier: Modifier,
    flashlightOn: Boolean,
    onResult: (String) -> Unit,
    onImageDecodeFailure: (String) -> Unit,
    permissionDeniedContent: @Composable () -> Unit,
) {
    QrScanner(
        modifier = modifier,
        flashlightOn = flashlightOn,
        cameraLens = CameraLens.Back,
        // false: QRKit's image-picker sheet (network.chaintech:cmp-image-pick-n-crop) calls
        // Material3's ModalBottomSheet with a signature from an older material3 than this
        // project pins (libs.versions.toml) — NoSuchMethodError at runtime if ever composed.
        // Camera-only scanning avoids that code path entirely.
        openImagePicker = false,
        onCompletion = onResult,
        imagePickerHandler = {},
        onFailure = onImageDecodeFailure,
        permissionDeniedView = { permissionDeniedContent() },
    )
}
