package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview

/** Re-click-guarded drop-in replacement for [androidx.compose.material3.FilledIconButton] — see [rememberOnceClick]. */
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    FilledIconButton(onClick = rememberOnceClick(onClick = onClick), modifier = modifier, enabled = enabled, content = content)
}

@Preview
@Composable
private fun FilledIconButtonLightPreview() = DebtTrackerPreview(darkTheme = false) {
    FilledIconButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = null) }
}

@Preview
@Composable
private fun FilledIconButtonDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    FilledIconButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = null) }
}