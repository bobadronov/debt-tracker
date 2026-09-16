package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview

/** Re-click-guarded drop-in replacement for [androidx.compose.material3.OutlinedButton] — see [rememberOnceClick]. */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(onClick = rememberOnceClick(onClick = onClick), modifier = modifier, enabled = enabled, content = content)
}

@Preview
@Composable
private fun OutlinedButtonLightPreview() = DebtTrackerPreview(darkTheme = false) { OutlinedButton(onClick = {}) { Text("Cancel") } }

@Preview
@Composable
private fun OutlinedButtonDarkPreview() = DebtTrackerPreview(darkTheme = true) { OutlinedButton(onClick = {}) { Text("Cancel") } }