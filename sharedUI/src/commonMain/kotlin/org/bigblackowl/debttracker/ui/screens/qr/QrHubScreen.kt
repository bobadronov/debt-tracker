package org.bigblackowl.debttracker.ui.screens.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.qr.rememberContactQrPainter
import org.bigblackowl.debttracker.domain.model.ContactQrPayload
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.appbar.BackTopAppBar
import org.bigblackowl.debttracker.ui.components.button.Button
import org.bigblackowl.debttracker.ui.components.button.OutlinedButton
import org.bigblackowl.debttracker.ui.components.contact.ContactQrScanOverlay
import org.bigblackowl.debttracker.ui.components.contact.ScannedContactDialog
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.koin.compose.viewmodel.koinViewModel

/**
 * QR contact-exchange hub (Home top bar → QR icon). Shows your own contact card as a QR code —
 * autofill from your account once signed in (no fields to edit here), or a locally-saved card you
 * fill in on the separate [EditContactCardScreen] while signed out. Scanning someone else's code
 * opens [ContactQrScanOverlay] (the same full-screen camera/file-pick component used from
 * AddEditContactForm) as an in-place overlay rather than another nav destination — nothing here
 * needs to survive a process/back-stack round trip once it's done. A valid scan asks whether to add
 * the person as a debtor or creditor before navigating to the matching pre-filled form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrHubScreen(
    onBack: () -> Unit,
    onEditCard: () -> Unit,
    onNavigateToAddDebtor: (ScannedContact) -> Unit,
    onNavigateToAddCreditor: (ScannedContact) -> Unit,
    viewModel: QrHubViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QrHubEffect.NavigateToAddDebtor -> onNavigateToAddDebtor(effect.contact)
                is QrHubEffect.NavigateToAddCreditor -> onNavigateToAddCreditor(effect.contact)
            }
        }
    }

    Scaffold(topBar = { BackTopAppBar(title = "", onBack = onBack) }) { padding ->
        ShareContent(
            state = state,
            onEditCard = onEditCard,
            onScanClick = { showScanner = true },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }

    state.scannedContact?.let { contact ->
        ScannedContactDialog(
            contact = contact,
            onDismiss = { viewModel.onIntent(QrHubIntent.DismissScannedContact) },
            onAddAsDebtor = { viewModel.onIntent(QrHubIntent.ConfirmScannedContact(asDebtor = true)) },
            onAddAsCreditor = { viewModel.onIntent(QrHubIntent.ConfirmScannedContact(asDebtor = false)) },
        )
    }

    if (showScanner) {
        ContactQrScanOverlay(
            onScanned = { contact ->
                showScanner = false
                viewModel.onIntent(QrHubIntent.ScanResult(contact))
            },
            onClose = { showScanner = false },
        )
    }
}

@Composable
private fun ShareContent(
    state: QrHubState,
    onEditCard: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.lg)) {
            state.qrPayload?.let { payload ->
                val painter = rememberContactQrPainter(payload)
                painter?.let {
                    Image(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth().aspectRatio(1f),
                    )
                    BodyText(strings.qr.hubDescription, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            } ?: BodyText(strings.qr.hubMyCardHint, style = MaterialTheme.typography.bodyMedium)
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.sm)) {
            // Signed-in users' card comes straight from the account — nothing local to edit here.
            if (!state.isAuthenticated) {
                OutlinedButton(
                    onClick = onEditCard,
                    modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth(),
                ) { Text(strings.accountInfoEdit) }
            }
            Button(
                onClick = onScanClick,
                modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth(),
            ) { Text(strings.qr.hubScanTab) }
        }
    }
}

@Composable
private fun QrHubScreenPreviewContent() {
    QrHubScreen(onBack = {}, onEditCard = {}, onNavigateToAddDebtor = {}, onNavigateToAddCreditor = {})
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
        ShareContent(state = state, onEditCard = {}, onScanClick = {}, modifier = Modifier.fillMaxSize().padding(padding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrHubScannedDialogPreviewContent() {
    QrHubSharePreviewContent(PREVIEW_STATE_SCANNED_DIALOG)
    PREVIEW_STATE_SCANNED_DIALOG.scannedContact?.let { contact ->
        ScannedContactDialog(contact = contact, onDismiss = {}, onAddAsDebtor = {}, onAddAsCreditor = {})
    }
}

/** Signed-in: card is auto-filled from the account, no fields to edit, QR always shown. */
private val PREVIEW_STATE_SIGNED_IN = QrHubState(isAuthenticated = true, qrPayload = ContactQrPayload.encode(PREVIEW_SCANNED_CONTACT))

@Preview
@Composable
private fun QrHubShareSignedInLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_IN) }

@Preview(device = DESKTOP)
@Composable
private fun QrHubShareSignedInDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_IN) }

/** Signed-out, nothing filled in yet: hint text instead of a QR code. */
private val PREVIEW_STATE_SIGNED_OUT_EMPTY = QrHubState(isAuthenticated = false, qrPayload = null)

@Preview
@Composable
private fun QrHubShareSignedOutEmptyLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_OUT_EMPTY) }

@Preview
@Composable
private fun QrHubShareSignedOutEmptyDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_OUT_EMPTY) }

/** Signed-out, card already filled in: QR code shown, Edit button available underneath. */
private val PREVIEW_STATE_SIGNED_OUT_FILLED = QrHubState(
    isAuthenticated = false,
    qrPayload = ContactQrPayload.encode(PREVIEW_SCANNED_CONTACT),
)

@Preview
@Composable
private fun QrHubShareSignedOutFilledLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_OUT_FILLED) }

@Preview
@Composable
private fun QrHubShareSignedOutFilledDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubSharePreviewContent(PREVIEW_STATE_SIGNED_OUT_FILLED) }

/** A completed scan: the "add as debtor or creditor?" chooser dialog on top of the share card. */
private val PREVIEW_STATE_SCANNED_DIALOG = PREVIEW_STATE_SIGNED_IN.copy(scannedContact = PREVIEW_SCANNED_CONTACT)

@Preview
@Composable
private fun QrHubScannedDialogLightPreview() = DebtTrackerPreview(darkTheme = false) { QrHubScannedDialogPreviewContent() }

@Preview
@Composable
private fun QrHubScannedDialogDarkPreview() = DebtTrackerPreview(darkTheme = true) { QrHubScannedDialogPreviewContent() }
