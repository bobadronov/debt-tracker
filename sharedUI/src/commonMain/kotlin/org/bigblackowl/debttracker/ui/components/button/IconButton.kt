package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview

/** Re-click-guarded drop-in replacement for [androidx.compose.material3.IconButton] — see [rememberOnceClick]. */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = rememberOnceClick(onClick = onClick), modifier = modifier, enabled = enabled, content = content)
}

@Preview
@Composable
private fun IconButtonLightPreview() = DebtTrackerPreview(darkTheme = false) {
    IconButton(onClick = {}) { Icon(Icons.Filled.Close, contentDescription = null) }
}

@Preview
@Composable
private fun IconButtonDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    IconButton(onClick = {}) { Icon(Icons.Filled.Close, contentDescription = null) }
}