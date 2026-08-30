package org.bigblackowl.debttracker.ui.screens.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.BackTopAppBar
import org.bigblackowl.debttracker.ui.components.EntityAvatar
import org.koin.compose.viewmodel.koinViewModel

/**
 * Step shown before [AddEditContactScreen]: search the list of people entered before and tap one to
 * pre-fill the form, or tap "New contact" to start from a blank form.
 */
@Composable
fun ContactPickerScreen(
    onBack: () -> Unit,
    onNewContact: () -> Unit,
    onPickContact: (ContactSuggestion) -> Unit,
    viewModel: ContactPickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    Scaffold(
        topBar = { BackTopAppBar(title = strings.contactPickerTitle, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth).fillMaxHeight().padding(Dimens.space12),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onIntent(ContactPickerIntent.Search(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(strings.contactPickerSearchPlaceholder) },
                )

                OutlinedCard(
                    onClick = onNewContact,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(Dimens.space12))
                        Text(strings.contactPickerNewContact, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (!state.hasAnyContacts) {
                    Text(
                        strings.contactPickerEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimens.space16),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(state.contacts, key = { it.contactKey() }) { contact ->
                            ContactPickerRow(contact = contact, onClick = { onPickContact(contact) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun ContactSuggestion.contactKey(): String = "$fullName|$phone|$email"

@Composable
private fun ContactPickerRow(contact: ContactSuggestion, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityAvatar(id = contact.fullName, name = contact.fullName, avatarUrl = contact.avatarUrl)
        Spacer(Modifier.width(Dimens.space12))
        Column {
            Text(contact.fullName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            (contact.phone?.takeIf(String::isNotBlank) ?: contact.email?.takeIf(String::isNotBlank))?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ContactPickerScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ContactPickerScreen(onBack = {}, onNewContact = {}, onPickContact = {})
}

@Preview
@Composable
private fun ContactPickerScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    ContactPickerScreen(onBack = {}, onNewContact = {}, onPickContact = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ContactPickerScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ContactPickerScreen(onBack = {}, onNewContact = {}, onPickContact = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ContactPickerScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    ContactPickerScreen(onBack = {}, onNewContact = {}, onPickContact = {})
}
