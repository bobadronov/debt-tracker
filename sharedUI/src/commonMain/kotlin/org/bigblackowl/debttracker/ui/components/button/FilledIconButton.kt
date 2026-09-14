package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.material3.FilledIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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