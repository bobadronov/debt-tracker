package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/** [androidx.compose.material3.Button] that swaps its content for a spinner while [isLoading], used by every form's submit action. */
@Composable
fun LoadingButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
) {

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.animateContentSize(),
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(modifier = Modifier.padding(end = Dimens.Spacing.sm))
        } else {
            leadingIcon?.invoke()
            label()
        }
    }
}

@Preview
@Composable
private fun LoadingButtonIdlePreview() = DebtTrackerPreview(darkTheme = false) {
    LoadingButton(onClick = {}, isLoading = false, label = { Text("Save") })
}

@Preview
@Composable
private fun LoadingButtonWithIconPreview() = DebtTrackerPreview(darkTheme = false) {
    LoadingButton(
        onClick = {},
        isLoading = false,
        leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null) },
        label = { Text("Save") },
    )
}

@Preview
@Composable
private fun LoadingButtonLoadingPreview() = DebtTrackerPreview(darkTheme = false) {
    LoadingButton(onClick = {}, isLoading = true, label = { Text("Save") })
}

@Preview
@Composable
private fun LoadingButtonDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    LoadingButton(onClick = {}, isLoading = true, label = { Text("Save") })
}
