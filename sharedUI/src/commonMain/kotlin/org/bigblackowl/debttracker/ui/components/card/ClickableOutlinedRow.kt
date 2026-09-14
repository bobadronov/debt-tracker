package org.bigblackowl.debttracker.ui.components.card

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.bigblackowl.debttracker.theme.Dimens

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