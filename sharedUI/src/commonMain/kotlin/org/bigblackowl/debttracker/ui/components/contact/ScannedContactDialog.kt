package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.button.TextButton

/** "Add as debtor or creditor?" chooser shown after decoding a contact QR — shared by
 * [org.bigblackowl.debttracker.ui.screens.qr.QrHubScreen] (in-app camera scan) and
 * [org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph] (an external scanner opening a
 * `debttracker://contact` deep link — see [org.bigblackowl.debttracker.core.qr.ContactDeepLinks]). */
@Composable
fun ScannedContactDialog(
    contact: ScannedContact,
    onDismiss: () -> Unit,
    onAddAsDebtor: () -> Unit,
    onAddAsCreditor: () -> Unit,
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.qr.hubScannedDialogTitle) },
        text = { Text(strings.qr.hubScannedDialogMessage(contact.fullName)) },
        confirmButton = { TextButton(onClick = onAddAsDebtor) { Text(strings.qr.hubScannedAsDebtor) } },
        dismissButton = { TextButton(onClick = onAddAsCreditor) { Text(strings.qr.hubScannedAsCreditor) } },
    )
}

private val PREVIEW_CONTACT = ScannedContact(fullName = "Тарас Шевченко", phone = "0501234567", email = "taras@example.com")

@Preview
@Composable
private fun ScannedContactDialogLightPreview() = DebtTrackerPreview(darkTheme = false) {
    ScannedContactDialog(contact = PREVIEW_CONTACT, onDismiss = {}, onAddAsDebtor = {}, onAddAsCreditor = {})
}

@Preview
@Composable
private fun ScannedContactDialogDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    ScannedContactDialog(contact = PREVIEW_CONTACT, onDismiss = {}, onAddAsDebtor = {}, onAddAsCreditor = {})
}
