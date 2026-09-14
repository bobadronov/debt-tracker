package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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