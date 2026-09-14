package org.bigblackowl.debttracker.ui.components.contact

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.validation.formatUkrainianPhone
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.EntityAvatar
import org.bigblackowl.debttracker.ui.components.card.TonalCard
import org.bigblackowl.debttracker.ui.components.text.CaptionText
import org.bigblackowl.debttracker.ui.components.text.TitleText

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
    TonalCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(Dimens.Radius.sm),
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(suggestion) }
                        .padding(horizontal = Dimens.Spacing.md, vertical = Dimens.Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EntityAvatar(id = suggestion.fullName, name = suggestion.fullName, avatarUrl = suggestion.avatarUrl, size = Dimens.IconSize.sm)
                    Spacer(Modifier.width(Dimens.Spacing.md))
                    Column {
                        TitleText(
                            suggestion.fullName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        formatUkrainianPhone(suggestion.phone)?.let {
                            CaptionText(
                                it,
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
