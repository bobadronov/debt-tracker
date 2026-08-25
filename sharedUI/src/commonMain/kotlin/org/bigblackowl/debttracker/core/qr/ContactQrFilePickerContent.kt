package org.bigblackowl.debttracker.core.qr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.media.rememberImagePicker
import org.bigblackowl.debttracker.theme.Dimens

/** [ContactQrFilePickerContent]'s pick-and-decode logic, exposed standalone so callers that want
 * their own trigger (e.g. QrHubScreen's single share-screen button) can launch the OS file picker
 * directly instead of going through the hint-text-plus-button layout. */
class ContactQrImagePicker(val pick: () -> Unit, val errorMessage: String?)

@Composable
fun rememberContactQrImagePicker(onResult: (String) -> Unit): ContactQrImagePicker {
    val strings = LocalStrings.current
    val imagePicker = rememberImagePicker()
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pick: () -> Unit = {
        imagePicker.pickImage { picked ->
            if (picked == null) return@pickImage
            errorMessage = null
            scope.launch {
                when (val result = decodeQrFromImage(picked.bytes)) {
                    is QrDecodeResult.Success -> onResult(result.payload)
                    QrDecodeResult.NotFound -> errorMessage = strings.qrHubPickFileNotFound
                    QrDecodeResult.Unsupported -> errorMessage = strings.qrHubPickFileUnsupported
                }
            }
        }
    }
    return ContactQrImagePicker(pick = pick, errorMessage = errorMessage)
}

/**
 * Desktop/Web's stand-in for the camera on [QR_SCAN_CAPABLE_PLATFORMS] (see [ContactQrScanner]):
 * pick a local image via [rememberImagePicker] and decode it with [decodeQrFromImage] instead of
 * scanning live. Used by [org.bigblackowl.debttracker.ui.components.ContactQrScanOverlay]
 * — [onResult] fires with the raw decoded payload, same as [ContactQrScanner]'s onResult, so
 * callers don't need to know which path produced it.
 */
@Composable
fun ContactQrFilePickerContent(modifier: Modifier = Modifier, onResult: (String) -> Unit) {
    val strings = LocalStrings.current
    val picker = rememberContactQrImagePicker(onResult)

    Column(
        modifier = modifier.padding(Dimens.space16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(strings.qrHubPickFileHint, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            Button(
                modifier = Modifier.padding(top = Dimens.space12),
                onClick = picker.pick,
            ) { Text(strings.qrHubPickFileButton) }

            picker.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
}
