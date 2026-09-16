package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

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

@Preview
@Composable
private fun ButtonLightPreview() = DebtTrackerPreview(darkTheme = false) { Button(onClick = {}) { Text("Save") } }

@Preview
@Composable
private fun ButtonDisabledDarkPreview() = DebtTrackerPreview(darkTheme = true) { Button(onClick = {}, enabled = false) { Text("Save") } }

/** All six button kinds together — the taxonomy from this file's doc comment, side by side for comparison. */
@Composable
private fun ButtonTaxonomySample() {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.sm)) {
        Button(onClick = {}) { Text("Button") }
        OutlinedButton(onClick = {}) { Text("Outlined") }
        TextButton(onClick = {}) { Text("Text") }
        IconButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = null) }
        FilledIconButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = null) }
        FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = null) }
    }
}

@Preview
@Composable
private fun ButtonTaxonomyLightPreview() = DebtTrackerPreview(darkTheme = false) { ButtonTaxonomySample() }

@Preview
@Composable
private fun ButtonTaxonomyDarkPreview() = DebtTrackerPreview(darkTheme = true) { ButtonTaxonomySample() }