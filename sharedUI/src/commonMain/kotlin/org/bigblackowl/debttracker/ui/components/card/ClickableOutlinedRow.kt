package org.bigblackowl.debttracker.ui.components.card

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.text.BodyText

/** Clickable outlined row that behaves like a trigger/field (date pickers, "add new" rows) — default Material styling. */
@Composable
fun ClickableOutlinedRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), enabled = enabled) {
        Row(
            modifier = Modifier.padding(Dimens.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Preview
@Composable
private fun ClickableOutlinedRowLightPreview() = DebtTrackerPreview(darkTheme = false) {
    ClickableOutlinedRow(onClick = {}) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
        BodyText("Pick a date", modifier = Modifier.padding(start = Dimens.Spacing.md))
    }
}

@Preview
@Composable
private fun ClickableOutlinedRowDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    ClickableOutlinedRow(onClick = {}) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
        BodyText("Pick a date", modifier = Modifier.padding(start = Dimens.Spacing.md))
    }
}