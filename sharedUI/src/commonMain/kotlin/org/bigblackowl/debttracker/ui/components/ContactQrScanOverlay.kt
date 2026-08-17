package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.qr.ContactQrScanner
import org.bigblackowl.debttracker.domain.model.ContactQrPayload
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Full-screen camera scanner for a Debt Tracker contact-card QR code, with its own top bar
 * (close button) and the same permission-denied rationale/retry as [org.bigblackowl.debttracker.ui.screens.qr.QrHubScreen]'s
 * scan mode. Reused wherever a screen wants an inline "scan a contact" entry point (QrHubScreen,
 * AddEditContactForm) instead of routing through the QR hub. A decoded code that isn't a Debt
 * Tracker contact card is silently ignored (see [ContactQrPayload.decode]) — the camera just keeps
 * scanning, no error shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactQrScanOverlay(onScanned: (ScannedContact) -> Unit, onClose: () -> Unit) {
    val strings = LocalStrings.current
    var permissionDenied by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopAppBar(title = "", onBack = onClose) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (permissionDenied) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(Dimens.space16),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(strings.qrHubCameraPermissionRationale, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { permissionDenied = false }) {
                        Text(strings.qrHubCameraPermissionRetry)
                    }
                }
            } else {
                ContactQrScanner(
                    modifier = Modifier.fillMaxSize(),
                    flashlightOn = false,
                    onResult = { raw -> ContactQrPayload.decode(raw)?.let(onScanned) },
                    onImageDecodeFailure = {},
                    permissionDeniedContent = {
                        LaunchedEffect(Unit) { permissionDenied = true }
                    },
                )
            }
        }
    }
}
