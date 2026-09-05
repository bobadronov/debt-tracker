package org.bigblackowl.debttracker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.ScannedContact

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
