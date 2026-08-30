package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Type-ahead list of previously entered debtors/creditors matching the name currently being typed
 * on the Add debtor/creditor forms — picking a row also carries over its phone/email/comment.
 */
@Composable
fun ContactSuggestionsList(
    suggestions: List<ContactSuggestion>,
    onSelect: (ContactSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.space16),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(suggestion) }
                        .padding(horizontal = Dimens.space12, vertical = Dimens.space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EntityAvatar(id = suggestion.fullName, name = suggestion.fullName, avatarUrl = suggestion.avatarUrl, size = Dimens.space28)
                    Spacer(Modifier.width(Dimens.space12))
                    Column {
                        Text(
                            suggestion.fullName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        suggestion.phone?.takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                if (index != suggestions.lastIndex) HorizontalDivider()
            }
        }
    }
}

private val previewSuggestions = listOf(
    ContactSuggestion(fullName = "Олена Ковальчук", phone = "+380671234567", email = null, comment = null),
    ContactSuggestion(fullName = "Олег Коваль", phone = null, email = "oleg@example.com", comment = null),
)

@Preview
@Composable
private fun ContactSuggestionsListLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ContactSuggestionsList(suggestions = previewSuggestions, onSelect = {})
}

@Preview
@Composable
private fun ContactSuggestionsListDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    ContactSuggestionsList(suggestions = previewSuggestions, onSelect = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ContactSuggestionsListLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ContactSuggestionsList(suggestions = previewSuggestions, onSelect = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ContactSuggestionsListDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    ContactSuggestionsList(suggestions = previewSuggestions, onSelect = {})
}
