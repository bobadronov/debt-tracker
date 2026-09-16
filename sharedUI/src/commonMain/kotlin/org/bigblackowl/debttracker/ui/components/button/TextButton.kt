package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview

/** Re-click-guarded drop-in replacement for [androidx.compose.material3.TextButton] — see [rememberOnceClick]. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(onClick = rememberOnceClick(onClick = onClick), modifier = modifier, enabled = enabled, content = content)
}

@Preview
@Composable
private fun TextButtonLightPreview() = DebtTrackerPreview(darkTheme = false) { TextButton(onClick = {}) { Text("Skip") } }

@Preview
@Composable
private fun TextButtonDarkPreview() = DebtTrackerPreview(darkTheme = true) { TextButton(onClick = {}) { Text("Skip") } }