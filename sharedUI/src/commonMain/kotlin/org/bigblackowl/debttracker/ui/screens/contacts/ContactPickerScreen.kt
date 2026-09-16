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
import org.bigblackowl.debttracker.domain.validation.formatUkrainianPhone
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.EntityAvatar
import org.bigblackowl.debttracker.ui.components.appbar.BackTopAppBar
import org.bigblackowl.debttracker.ui.components.card.ClickableOutlinedRow
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.bigblackowl.debttracker.ui.components.text.CaptionText
import org.bigblackowl.debttracker.ui.components.text.TitleText
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

    ContactPickerContent(
        state = state,
        onBack = onBack,
        onNewContact = onNewContact,
        onPickContact = onPickContact,
        onSearch = { viewModel.onIntent(ContactPickerIntent.Search(it)) },
    )
}

@Composable
private fun ContactPickerContent(
    state: ContactPickerState,
    onBack: () -> Unit,
    onNewContact: () -> Unit,
    onPickContact: (ContactSuggestion) -> Unit,
    onSearch: (String) -> Unit,
) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = { BackTopAppBar(title = strings.contactPicker.title, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth).fillMaxHeight().padding(Dimens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.md),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(strings.contactPicker.searchPlaceholder) },
                )

                ClickableOutlinedRow(
                    onClick = onNewContact,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(Dimens.Spacing.md))
                    TitleText(strings.contactPicker.newContact, style = MaterialTheme.typography.bodyLarge)
                }

                if (!state.hasAnyContacts) {
                    BodyText(
                        strings.contactPicker.empty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimens.Spacing.lg),
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
            .padding(vertical = Dimens.Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityAvatar(id = contact.fullName, name = contact.fullName, avatarUrl = contact.avatarUrl)
        Spacer(Modifier.width(Dimens.Spacing.md))
        Column {
            TitleText(contact.fullName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            (formatUkrainianPhone(contact.phone) ?: contact.email?.takeIf(String::isNotBlank))?.let {
                CaptionText(it, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Preview(state: ContactPickerState) = ContactPickerContent(
    state = state,
    onBack = {},
    onNewContact = {},
    onPickContact = {},
    onSearch = {},
)

private val PREVIEW_CONTACTS = listOf(
    ContactSuggestion(fullName = "Тарас Шевченко", phone = "0501234567", email = null, comment = null),
    ContactSuggestion(fullName = "Леся Українка", phone = null, email = "lesya@example.com", comment = null),
)

@Preview
@Composable
private fun ContactPickerScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ContactPickerState(contacts = PREVIEW_CONTACTS, hasAnyContacts = true))
}

@Preview
@Composable
private fun ContactPickerScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(ContactPickerState(contacts = PREVIEW_CONTACTS, hasAnyContacts = true))
}

@Preview(device = DESKTOP)
@Composable
private fun ContactPickerScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ContactPickerState(contacts = PREVIEW_CONTACTS, hasAnyContacts = true))
}

@Preview(device = DESKTOP)
@Composable
private fun ContactPickerScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(ContactPickerState(contacts = PREVIEW_CONTACTS, hasAnyContacts = true))
}

@Preview
@Composable
private fun ContactPickerScreenEmptyPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ContactPickerState())
}

@Preview
@Composable
private fun ContactPickerScreenNoResultsPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ContactPickerState(query = "zzz", contacts = emptyList(), hasAnyContacts = true))
}
