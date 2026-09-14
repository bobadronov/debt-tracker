package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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