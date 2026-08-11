package org.bigblackowl.debttracker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.bigblackowl.debttracker.core.i18n.LocalStrings

/**
 * Title + text + confirm/cancel [AlertDialog], shared by every "are you sure?" prompt
 * (sign-out, delete-all-data double confirm, session revoke) — only the strings and the
 * confirm action differ between call sites.
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = LocalStrings.current.cancel,
    confirmColor: Color = Color.Unspecified,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = confirmColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}
