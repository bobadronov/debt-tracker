package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview


@Composable
fun BackButton(onClick: () -> Unit = {}) =
    IconButton(onClick = onClick) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            null
        )
    }

/** [TopAppBar] with a title and an optional [BackButton] — the shape repeated across nearly every detail/form screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { onBack?.let { BackButton(onClick = it) } },
        actions = actions,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun BackButtonLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    BackButton{}
}

@Preview
@Composable
private fun BackButtonDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    BackButton{}
}

@Preview(device = DESKTOP)
@Composable
private fun BackButtonLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    BackButton{}
}

@Preview(device = DESKTOP)
@Composable
private fun BackButtonDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    BackButton{}
}