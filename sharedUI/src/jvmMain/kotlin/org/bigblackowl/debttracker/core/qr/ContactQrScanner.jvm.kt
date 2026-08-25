package org.bigblackowl.debttracker.core.qr

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import qrscanner.CameraLens
import qrscanner.QrScanner

@Composable
actual fun ContactQrScanner(
    description: String,
    modifier: Modifier,
    flashlightOn: Boolean,
    onResult: (String) -> Unit,
    onImageDecodeFailure: (String) -> Unit,
    permissionDeniedContent: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QrScanner(
            modifier = modifier,
            flashlightOn = flashlightOn,
            cameraLens = CameraLens.Front,
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
        Text(description)
    }
}
