package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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