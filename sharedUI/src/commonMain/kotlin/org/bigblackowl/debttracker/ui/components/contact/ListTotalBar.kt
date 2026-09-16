package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.button.FloatingActionButton
import org.bigblackowl.debttracker.ui.components.card.ContentCard
import org.bigblackowl.debttracker.ui.components.text.TitleText

/** Running total + add button, pinned under the list. */
@Composable
fun ListTotalBar(label: String, totalText: String, onAdd: () -> Unit) {
    ContentCard(modifier = Modifier.padding(Dimens.Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TitleText(label, style = MaterialTheme.typography.bodyLarge)
                TitleText(totalText, color = MaterialTheme.debtAccentColors.debt, style = MaterialTheme.typography.bodyLarge)
            }
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.padding(start = Dimens.Spacing.md),
            ) { Icon(Icons.Default.Add, null) }
        }
    }
}

@Preview
@Composable
private fun ListTotalBarPreview() = DebtTrackerPreview(darkTheme = false) {
    ListTotalBar(label = "Total", totalText = "4 650 ₴", onAdd = {})
}

@Preview
@Composable
private fun ListTotalBarMultiCurrencyPreview() = DebtTrackerPreview(darkTheme = false) {
    ListTotalBar(label = "Total", totalText = "4 650 ₴ · 120 \$ · 80 €", onAdd = {})
}

@Preview
@Composable
private fun ListTotalBarDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    ListTotalBar(label = "Total", totalText = "4 650 ₴", onAdd = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ListTotalBarDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ListTotalBar(label = "Total", totalText = "4 650 ₴", onAdd = {})
}
