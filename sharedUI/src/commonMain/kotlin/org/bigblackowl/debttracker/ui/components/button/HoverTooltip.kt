package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

/** Wraps [content] in a plain tooltip: shown on mouse hover (desktop/web) and long-press (touch). No-op when [text] is null/blank. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoverTooltip(text: String?, content: @Composable () -> Unit) {
    if (text.isNullOrBlank()) {
        content()
        return
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        content = content,
    )
}
