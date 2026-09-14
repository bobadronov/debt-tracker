package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Re-click-guarded drop-in replacement for [androidx.compose.material3.FloatingActionButton] — see [rememberOnceClick]. */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(onClick = rememberOnceClick(onClick = onClick), modifier = modifier, content = content)
}