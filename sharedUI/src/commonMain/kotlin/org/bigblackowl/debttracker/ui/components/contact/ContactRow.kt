package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.validation.formatUkrainianPhone
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.EntityAvatar
import org.bigblackowl.debttracker.ui.components.button.IconButton
import org.bigblackowl.debttracker.ui.components.card.SemanticOutlinedCard
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.bigblackowl.debttracker.ui.components.text.TitleText
import org.koin.compose.koinInject

/** One row of a contact list: avatar + name/phone, balance, and an overflow-menu delete. */
@Composable
fun ContactRow(
    id: String,
    name: String,
    phone: String?,
    avatarUrl: String?,
    balanceText: String,
    deleteLabel: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val appSettings = koinInject<AppSettings>()
    val haptics = LocalHapticFeedback.current
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    SemanticOutlinedCard(
        borderColor = MaterialTheme.colorScheme.primary,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = Dimens.Spacing.md, vertical = Dimens.Spacing.xs),
        borderWidth = Dimens.Border.thin,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Dimens.Spacing.lg, top = Dimens.Spacing.lg, bottom = Dimens.Spacing.lg, end = Dimens.Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntityAvatar(id = id, name = name, avatarUrl = avatarUrl)
                Spacer(Modifier.width(Dimens.Spacing.md))
                Column {
                    TitleText(name, style = MaterialTheme.typography.bodyLarge)
                    formatUkrainianPhone(phone)?.let { BodyText(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TitleText(balanceText, color = MaterialTheme.debtAccentColors.debt, style = MaterialTheme.typography.bodyLarge)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(deleteLabel) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = strings.deleteContactConfirmTitle,
            text = strings.deleteContactConfirmText(name),
            confirmLabel = strings.delete,
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                confirmDelete = false
                if (appSettings.hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Preview
@Composable
private fun ContactRowWithPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ContactRow(id = "1", name = "Олена Коваль", phone = "+380 67 123 4567", avatarUrl = null, balanceText = "1 200 ₴", deleteLabel = "Delete", onClick = {}, onDelete = {})
}

@Preview
@Composable
private fun ContactRowNoPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ContactRow(id = "2", name = "Іван Петренко", phone = null, avatarUrl = null, balanceText = "450 ₴", deleteLabel = "Delete", onClick = {}, onDelete = {})
}

@Preview
@Composable
private fun ContactRowLongNamePreview() = DebtTrackerPreview(darkTheme = false) {
    ContactRow(id = "3", name = "Олександр Костянтинович Верещагін-Приходько", phone = "+380 50 987 6543", avatarUrl = null, balanceText = "125 000 ₴", deleteLabel = "Delete", onClick = {}, onDelete = {})
}

@Preview
@Composable
private fun ContactRowDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    ContactRow(id = "1", name = "Олена Коваль", phone = "+380 67 123 4567", avatarUrl = null, balanceText = "1 200 ₴", deleteLabel = "Delete", onClick = {}, onDelete = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ContactRowDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ContactRow(id = "1", name = "Олена Коваль", phone = "+380 67 123 4567", avatarUrl = null, balanceText = "1 200 ₴", deleteLabel = "Delete", onClick = {}, onDelete = {})
}
