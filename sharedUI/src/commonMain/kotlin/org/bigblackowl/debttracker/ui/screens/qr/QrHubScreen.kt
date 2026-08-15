package org.bigblackowl.debttracker.ui.screens.qr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.qr.ContactQrScanner
import org.bigblackowl.debttracker.core.qr.rememberContactQrPainter
import org.bigblackowl.debttracker.domain.model.ContactQrPayload
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.domain.validation.isPhonePasteRelevant
import org.bigblackowl.debttracker.domain.validation.isValidEmail
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.BackTopAppBar
import org.bigblackowl.debttracker.ui.components.PasteableOutlinedTextField
import org.bigblackowl.debttracker.ui.components.ScannedContactDialog
import org.bigblackowl.debttracker.ui.components.rememberClipboardText
import org.koin.compose.viewmodel.koinViewModel

/** Camera scanning needs a working QRKit scanner — Web has none at all (icon hidden entirely in
 * HomeScreen) and Desktop's only wired up for the "show my QR" half (see core/qr's self-review). */
private val QR_SCAN_CAPABLE_PLATFORMS = setOf(AppPlatform.ANDROID, AppPlatform.IOS)

/**
 * QR contact-exchange hub (Home top bar → QR icon). Shows your own contact card as a QR code —
 * auto-filled from your account once signed in (no fields to edit here), or a locally-saved card
 * you fill in via Edit while signed out. On Android/iOS a Scan button switches the same screen
 * into camera mode; on Desktop/Web there's no Scan button at all (see [QR_SCAN_CAPABLE_PLATFORMS]).
 * A valid scan asks whether to add the person as a debtor or creditor before navigating to the
 * matching pre-filled form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrHubScreen(
    onBack: () -> Unit,
    onNavigateToAddDebtor: (ScannedContact) -> Unit,
    onNavigateToAddCreditor: (ScannedContact) -> Unit,
    viewModel: QrHubViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QrHubEffect.NavigateToAddDebtor -> onNavigateToAddDebtor(effect.contact)
                is QrHubEffect.NavigateToAddCreditor -> onNavigateToAddCreditor(effect.contact)
            }
        }
    }

    Scaffold(
        topBar = {
            BackTopAppBar(
                title = "",
                // In Scan mode, back returns to the QR card instead of popping the screen.
                onBack = { if (state.mode == QrHubMode.SCAN) viewModel.onIntent(QrHubIntent.SwitchToShare) else onBack() },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.mode) {
                QrHubMode.SHARE -> ShareContent(state = state, onIntent = viewModel::onIntent)
                QrHubMode.SCAN -> ScanContent(state = state, onIntent = viewModel::onIntent)
            }
        }
    }

    state.scannedContact?.let { contact ->
        ScannedContactDialog(
            contact = contact,
            onDismiss = { viewModel.onIntent(QrHubIntent.DismissScannedContact) },
            onAddAsDebtor = { viewModel.onIntent(QrHubIntent.ConfirmScannedContact(asDebtor = true)) },
            onAddAsCreditor = { viewModel.onIntent(QrHubIntent.ConfirmScannedContact(asDebtor = false)) },
        )
    }
}

@Composable
private fun ShareContent(state: QrHubState, onIntent: (QrHubIntent) -> Unit) {
    val strings = LocalStrings.current
    val clipboardText by rememberClipboardText()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.space16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // The QR code and the edit fields are mutually exclusive — editing invalidates whatever's
        // currently encoded on screen as you type, so showing both at once is just confusing.
        // AnimatedVisibility crossfades between them in place instead of a hard cut.
        AnimatedVisibility(
            visible = !state.fieldsExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            state.qrPayload?.let { payload ->
                val painter = rememberContactQrPainter(payload)
                painter?.let {
                    Image(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth().aspectRatio(1f),
                    )
                }
            } ?: Text(strings.qrHubMyCardHint, style = MaterialTheme.typography.bodyMedium)
        }
        Column (
            verticalArrangement = Arrangement.spacedBy(Dimens.space8),

            ){


            if (currentPlatform in QR_SCAN_CAPABLE_PLATFORMS) {
                Button(
                    onClick = { onIntent(QrHubIntent.SwitchToScan) },
                    modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth(),
                ) { Text(strings.qrHubScanTab) }
            }

            // Signed-in users' card comes straight from the account — nothing local to edit here.
            if (!state.isAuthenticated) {
                OutlinedButton(
                    onClick = { onIntent(QrHubIntent.EditClicked) },
                    modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth(),
                ) { Text(if (state.fieldsExpanded) strings.save else strings.accountInfoEdit) }

                AnimatedVisibility(
                    visible = state.fieldsExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                    ) {
                        PasteableOutlinedTextField(
                            value = state.myName,
                            onValueChange = { onIntent(QrHubIntent.MyNameChanged(it)) },
                            label = strings.fullName,
                            clipboardText = clipboardText,
                            isPasteRelevant = ::isValidFullName,
                        )
                        PasteableOutlinedTextField(
                            value = state.myPhone,
                            onValueChange = { onIntent(QrHubIntent.MyPhoneChanged(it)) },
                            label = strings.phone,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            clipboardText = clipboardText,
                            isPasteRelevant = ::isPhonePasteRelevant,
                        )
                        PasteableOutlinedTextField(
                            value = state.myEmail,
                            onValueChange = { onIntent(QrHubIntent.MyEmailChanged(it)) },
                            label = strings.email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            clipboardText = clipboardText,
                            isPasteRelevant = ::isValidEmail,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanContent(state: QrHubState, onIntent: (QrHubIntent) -> Unit) {
    val strings = LocalStrings.current

    if (state.cameraPermissionDenied) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(strings.qrHubCameraPermissionRationale, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onIntent(QrHubIntent.SwitchToScan) }) {
                Text(strings.qrHubCameraPermissionRetry)
            }
        }
    } else {
        ContactQrScanner(
            modifier = Modifier.fillMaxSize(),
            flashlightOn = false,
            onResult = { payload -> onIntent(QrHubIntent.ScanResult(payload)) },
            onImageDecodeFailure = {},
            permissionDeniedContent = {
                LaunchedEffect(Unit) { onIntent(QrHubIntent.CameraPermissionDenied) }
            },
        )
    }
}

@Composable
private fun QrHubScreenPreviewContent() {
    QrHubScreen(onBack = {}, onNavigateToAddDebtor = {}, onNavigateToAddCreditor = {})
}

@Preview
@Composable
private fun QrHubScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { QrHubScreenPreviewContent() }

@Preview
@Composable
private fun QrHubScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { QrHubScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun QrHubScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { QrHubScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun QrHubScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { QrHubScreenPreviewContent() }

// --- Explicit-state previews below (bypass the ViewModel entirely so each QrHubState variant
// renders deterministically) — the default previews above only ever show whatever state the
// Koin-fake-backed QrHubViewModel happens to produce (preview/PreviewModule.kt). ---

private val PREVIEW_SCANNED_CONTACT = ScannedContact(fullName = "Olena Kovalenko", phone = "+380501234567", email = "olena@example.com")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrHubSharePreviewContent(state: QrHubState) {
    Scaffold(topBar = { BackTopAppBar(title = "", onBack = {}) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ShareContent(state = state, onIntent = {})
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrHubScanPreviewContent(state: QrHubState) {
    Scaffold(topBar = { BackTopAppBar(title = "", onBack = {}) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScanContent(state = state, onIntent = {})
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrHubScannedDialogPreviewContent(state: QrHubState) {
    QrHubSharePreviewContent(state)
    state.scannedContact?.let { contact ->
        ScannedContactDialog(contact = contact, onDismiss = {}, onAddAsDebtor = {}, onAddAsCreditor = {})
    }
}

/** Signed-in: card is auto-filled from the account, no fields to edit, QR always shown. */
private val PREVIEW_STATE_SIGNED_IN = QrHubState(isAuthenticated = true, myName = "Olena Kovalenko", qrPayload = ContactQrPayload.encode(PREVIEW_SCANNED_CONTACT))

@Preview
@Composable
private fun QrHubShareSignedInLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_IN) }

@Preview
@Composable
private fun QrHubShareSignedInDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_IN) }

/** Signed-out, nothing filled in yet: hint text instead of a QR code, Edit button collapsed. */
private val PREVIEW_STATE_SIGNED_OUT_EMPTY = QrHubState(isAuthenticated = false, fieldsExpanded = false, qrPayload = null)

@Preview
@Composable
private fun QrHubShareSignedOutEmptyLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_OUT_EMPTY) }

@Preview
@Composable
private fun QrHubShareSignedOutEmptyDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_OUT_EMPTY) }

/** Signed-out, Edit expanded and filled in: QR code hidden, editable name/phone/email fields
 * shown instead — the Edit button reads "Save" in this state; tapping it flips back. */
private val PREVIEW_STATE_SIGNED_OUT_EXPANDED = QrHubState(
    isAuthenticated = false,
    fieldsExpanded = true,
    myName = "Olena Kovalenko",
    myPhone = "+380501234567",
    myEmail = "olena@example.com",
    qrPayload = ContactQrPayload.encode(PREVIEW_SCANNED_CONTACT),
)

@Preview
@Composable
private fun QrHubShareSignedOutExpandedLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_OUT_EXPANDED) }

@Preview
@Composable
private fun QrHubShareSignedOutExpandedDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_OUT_EXPANDED) }

/** Scan mode, camera permission denied: rationale text + retry, the only Scan-mode sub-state that
 * renders meaningfully outside a real device (the granted-permission camera view needs actual
 * hardware — see QR_SCAN_CAPABLE_PLATFORMS, Desktop/Web never reach it in the real app either). */
private val PREVIEW_STATE_SCAN_DENIED = QrHubState(mode = QrHubMode.SCAN, cameraPermissionDenied = true)

@Preview
@Composable
private fun QrHubScanPermissionDeniedLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubScanPreviewContent(PREVIEW_STATE_SCAN_DENIED) }

@Preview
@Composable
private fun QrHubScanPermissionDeniedDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubScanPreviewContent(PREVIEW_STATE_SCAN_DENIED) }

/** A completed scan: the "add as debtor or creditor?" chooser dialog on top of the share card. */
private val PREVIEW_STATE_SCANNED_DIALOG = PREVIEW_STATE_SIGNED_IN.copy(scannedContact = PREVIEW_SCANNED_CONTACT)

@Preview
@Composable
private fun QrHubScannedDialogLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubScannedDialogPreviewContent(PREVIEW_STATE_SCANNED_DIALOG) }

@Preview
@Composable
private fun QrHubScannedDialogDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubScannedDialogPreviewContent(PREVIEW_STATE_SCANNED_DIALOG) }
