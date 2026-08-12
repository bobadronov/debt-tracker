package org.bigblackowl.debttracker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview

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

@Preview
@Composable
private fun ConfirmDialogLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Scaffold {
        ConfirmDialog(
            title = "Delete debtor?",
            text = "This action cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmDialogDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Scaffold {
        ConfirmDialog(
            title = "Delete debtor?",
            text = "This action cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(device = DESKTOP)
@Composable
private fun ConfirmDialogLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Scaffold {
        ConfirmDialog(
            title = "Delete debtor?",
            text = "This action cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(device = DESKTOP)
@Composable
private fun ConfirmDialogDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Scaffold {
        ConfirmDialog(
            title = "Delete debtor?",
            text = "This action cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
