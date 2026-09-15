package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Task-based button taxonomy for the whole app — six kinds, each a re-click-guarded drop-in
 * replacement for its Material 3 counterpart (see [rememberOnceClick]):
 * [Button] (primary/filled CTA), [OutlinedButton] (secondary), [TextButton] (tertiary),
 * [IconButton]/[FilledIconButton] (icon-only), [FloatingActionButton] (FAB).
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(onClick = rememberOnceClick(onClick = onClick), modifier = modifier, enabled = enabled, content = content)
}