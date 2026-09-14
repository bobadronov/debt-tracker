package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.button.IconButton
import org.bigblackowl.debttracker.ui.components.button.TextButton
import org.bigblackowl.debttracker.ui.components.card.TonalCard
import org.bigblackowl.debttracker.ui.components.text.CaptionText
import org.bigblackowl.debttracker.ui.components.text.TitleText

/**
 * Autofill banner: shown when the email entered in the debtor/creditor form
 * matches a registered app user (§ProfileLookup).
 */
@Composable
fun ProfileSuggestionCard(
    suggestion: ProfileSuggestion,
    onUse: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    TonalCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(Dimens.Radius.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(Dimens.IconSize.md).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (suggestion.avatarUrl != null) {
                    AsyncImage(
                        model = suggestion.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSize.md).clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(Dimens.Spacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.xs)) {
                CaptionText(
                    strings.contactSuggestionFound,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                suggestion.displayName?.takeIf(String::isNotBlank)?.let {
                    TitleText(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            TextButton(onClick = onUse) { Text(strings.contactSuggestionUse) }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = strings.cancel)
            }
        }
    }
}

@Preview
@Composable
private fun ProfileSuggestionCardLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ProfileSuggestionCard(
        suggestion = ProfileSuggestion(displayName = "Олена Ковальчук", avatarUrl = null),
        onUse = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun ProfileSuggestionCardDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    ProfileSuggestionCard(
        suggestion = ProfileSuggestion(displayName = "Олена Ковальчук", avatarUrl = null),
        onUse = {},
        onDismiss = {},
    )
}

@Preview(device = DESKTOP)
@Composable
private fun ProfileSuggestionCardLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ProfileSuggestionCard(
        suggestion = ProfileSuggestion(displayName = "Олена Ковальчук", avatarUrl = null),
        onUse = {},
        onDismiss = {},
    )
}

@Preview(device = DESKTOP)
@Composable
private fun ProfileSuggestionCardDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    ProfileSuggestionCard(
        suggestion = ProfileSuggestion(displayName = "Олена Ковальчук", avatarUrl = null),
        onUse = {},
        onDismiss = {},
    )
}
