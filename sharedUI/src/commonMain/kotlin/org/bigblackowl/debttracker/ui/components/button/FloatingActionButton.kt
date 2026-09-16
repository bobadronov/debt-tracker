package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview

/** Re-click-guarded drop-in replacement for [androidx.compose.material3.FloatingActionButton] — see [rememberOnceClick]. */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(onClick = rememberOnceClick(onClick = onClick), modifier = modifier, content = content)
}

@Preview
@Composable
private fun FloatingActionButtonLightPreview() = DebtTrackerPreview(darkTheme = false) {
    FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = null) }
}

@Preview
@Composable
private fun FloatingActionButtonDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = null) }
}