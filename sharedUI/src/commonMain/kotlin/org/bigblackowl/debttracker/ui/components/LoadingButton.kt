package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            CircularWavyProgressIndicator(modifier = Modifier.padding(end = Dimens.space8))
        } else {
            leadingIcon?.invoke()
            label()
        }
    }
}