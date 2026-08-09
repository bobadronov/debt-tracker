package org.bigblackowl.debttracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
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