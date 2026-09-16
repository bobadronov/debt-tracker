package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.qr.ContactQrFilePickerContent
import org.bigblackowl.debttracker.core.qr.ContactQrScanner
import org.bigblackowl.debttracker.core.qr.QR_SCAN_CAPABLE_PLATFORMS
import org.bigblackowl.debttracker.core.qr.ScanOverlayActionButton
import org.bigblackowl.debttracker.domain.model.ContactQrPayload
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.appbar.BackTopAppBar
import org.bigblackowl.debttracker.ui.components.button.TextButton
import org.bigblackowl.debttracker.ui.components.text.BodyText

/**
 * Full-screen QR scanner for a Debt Tracker contact-card code, with its own top bar (close
 * button). On [QR_SCAN_CAPABLE_PLATFORMS] (Android/iOS) the live camera is primary, with a "select
 * image" text action underneath to switch to picking a local image instead — for a QR code that
 * isn't easy to point the camera at (e.g. one shown on another device's screen, or a photo already
 * in the gallery). Desktop/Web have no camera scanner at all, so they open straight into the file
 * picker (see [QR_SCAN_CAPABLE_PLATFORMS] and [ContactQrFileDecoder.kt][decodeQrFromImage]).
 * Reused wherever a screen wants an inline "scan a contact" entry point (QrHubScreen,
 * AddEditContactForm) instead of routing through the QR hub. A decoded code that isn't a Debt
 * Tracker contact card is silently ignored (see [ContactQrPayload.decode]) — the camera just keeps
 * scanning / the file picker shows no error, no error shown either way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactQrScanOverlay(onScanned: (ScannedContact) -> Unit, onClose: () -> Unit) {
    val strings = LocalStrings.current
    val canScanWithCamera = currentPlatform in QR_SCAN_CAPABLE_PLATFORMS
    var permissionDenied by remember { mutableStateOf(false) }
    // Desktop/Web start (and stay) in file-pick mode — they have no camera at all, so there's
    // nothing to switch back to.
    var showFilePicker by remember { mutableStateOf(!canScanWithCamera) }

    Scaffold(topBar = { BackTopAppBar(title = "", onBack = onClose) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                showFilePicker -> Column(modifier = Modifier.fillMaxSize()) {
                    ContactQrFilePickerContent(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onResult = { raw -> ContactQrPayload.decode(raw)?.let(onScanned) },
                    )
                    if (canScanWithCamera) {
                        TextButton(
                            onClick = { showFilePicker = false },
                            modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.Spacing.lg),
                        ) { Text(strings.qr.hubScanTab) }
                    }
                }

                permissionDenied -> Column(
                    modifier = Modifier.fillMaxSize().padding(Dimens.Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BodyText(strings.qr.hubCameraPermissionRationale, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { permissionDenied = false }) {
                        Text(strings.qr.hubCameraPermissionRetry)
                    }
                }

                else -> Box(modifier = Modifier.fillMaxSize()) {
                    ContactQrScanner(
                        description = "",
                        modifier = Modifier.fillMaxSize(),
                        flashlightOn = false,
                        onResult = { raw -> ContactQrPayload.decode(raw)?.let(onScanned) },
                        onImageDecodeFailure = {},
                        permissionDeniedContent = {
                            LaunchedEffect(Unit) { permissionDenied = true }
                        },
                    )
                    ScanOverlayActionButton(strings.qr.hubSelectImageTab) { showFilePicker = true }
                }
            }
        }
    }
}
