package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Reads the system clipboard once per screen visit. Pass the result into every
 * [ClipboardPasteHint] on that screen instead of letting each field read the clipboard
 * itself — repeated reads trigger Android's "pasted from clipboard" system notice per call.
 */
@Composable
fun rememberClipboardText(): State<String?> {
    val clipboardManager = LocalClipboardManager.current
    val clipboardText = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        clipboardText.value = clipboardManager.getText()?.text?.trim()?.takeIf(String::isNotEmpty)
    }
    return clipboardText
}

/**
 * Dismissible chip suggesting to paste [clipboardText] into a currently-blank field, shown
 * only while the field is focused (so it doesn't clutter screens with several empty fields)
 * and only when [isRelevant] accepts the clipboard content. Mirrors [ProfileSuggestionCard]'s
 * use/dismiss interaction for visual consistency.
 */
@Composable
fun ClipboardPasteHint(
    clipboardText: String?,
    fieldValue: String,
    isFieldFocused: Boolean,
    isRelevant: (String) -> Boolean,
    onPaste: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (clipboardText.isNullOrBlank()) return
    if (!isFieldFocused || fieldValue.isNotBlank() || !isRelevant(clipboardText)) return
    var dismissed by remember(clipboardText) { mutableStateOf(false) }
    if (dismissed) return

    val strings = LocalStrings.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.space16),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.ContentPaste,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(Dimens.space20),
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = Dimens.space12),
                verticalArrangement = Arrangement.spacedBy(Dimens.space2),
            ) {
                Text(
                    strings.clipboardPasteFound,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    clipboardText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { onPaste(clipboardText); dismissed = true }) {
                Text(strings.clipboardPasteUse)
            }
            IconButton(onClick = { dismissed = true }) {
                Icon(Icons.Default.Close, contentDescription = strings.cancel)
            }
        }
    }
}

@Preview
@Composable
private fun ClipboardPasteHintLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ClipboardPasteHint(
        clipboardText = "1234.56",
        fieldValue = "",
        isFieldFocused = true,
        isRelevant = { true },
        onPaste = {},
    )
}

@Preview
@Composable
private fun ClipboardPasteHintDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    ClipboardPasteHint(
        clipboardText = "1234.56",
        fieldValue = "",
        isFieldFocused = true,
        isRelevant = { true },
        onPaste = {},
    )
}

@Preview(device = DESKTOP)
@Composable
private fun ClipboardPasteHintLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ClipboardPasteHint(
        clipboardText = "1234.56",
        fieldValue = "",
        isFieldFocused = true,
        isRelevant = { true },
        onPaste = {},
    )
}

@Preview(device = DESKTOP)
@Composable
private fun ClipboardPasteHintDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    ClipboardPasteHint(
        clipboardText = "1234.56",
        fieldValue = "",
        isFieldFocused = true,
        isRelevant = { true },
        onPaste = {},
    )
}
