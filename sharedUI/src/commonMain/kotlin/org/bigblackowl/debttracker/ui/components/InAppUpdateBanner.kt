package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.update.inAppUpdateSupported
import org.bigblackowl.debttracker.core.update.rememberInAppUpdateLauncher
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Play's own in-app update flow (Android only, see [inAppUpdateSupported]): checks once at app
 * start — Play downloads a flexible update silently in the background if one exists — and shows
 * this banner only once it's actually ready to install, since that's the one point where the user
 * needs to make a call (when to restart).
 */
@Composable
fun BoxScope.InAppUpdateBanner() {
    if (!inAppUpdateSupported) return

    val launcher = rememberInAppUpdateLauncher()
    val readyToInstall by launcher.updateReadyToInstall.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { launcher.checkForUpdate() }

    AnimatedVisibility(
        visible = readyToInstall, modifier = Modifier.align(
            Alignment.BottomCenter
        )
    ) {
        InAppUpdateBannerCard(onRestart = { launcher.completeUpdate() })
    }
}

/** The card's visuals, pulled out of [InAppUpdateBanner] so @Preview can render it without a real [rememberInAppUpdateLauncher]. */
@Composable
private fun InAppUpdateBannerCard(onRestart: () -> Unit) {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.space8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.updateReadyToInstall,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRestart) { Text(strings.updateRestartNow) }
            }
        }
    }
}

@Preview
@Composable
private fun InAppUpdateBannerLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    InAppUpdateBannerCard(onRestart = {})
}

@Preview
@Composable
private fun InAppUpdateBannerDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    InAppUpdateBannerCard(onRestart = {})
}

@Preview(device = DESKTOP)
@Composable
private fun InAppUpdateBannerLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    InAppUpdateBannerCard(onRestart = {})
}

@Preview(device = DESKTOP)
@Composable
private fun InAppUpdateBannerDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    InAppUpdateBannerCard(onRestart = {})
}
