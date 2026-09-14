package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Filled [Button] with the error color scheme — the irreversible-action button (delete, unlink,
 * revoke, sign-out, remove-all-data) that today each screen otherwise recolors ad hoc.
 */
@Composable
fun DestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = rememberOnceClick(onClick = onClick),
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        content = content,
    )
}