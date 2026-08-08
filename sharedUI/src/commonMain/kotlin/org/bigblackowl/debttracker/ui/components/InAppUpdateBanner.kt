package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.update.inAppUpdateSupported
import org.bigblackowl.debttracker.core.update.rememberInAppUpdateLauncher
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Play's own in-app update flow (Android only, see [inAppUpdateSupported]): checks once at app
 * start — Play downloads a flexible update silently in the background if one exists — and shows
 * this banner only once it's actually ready to install, since that's the one point where the user
 * needs to make a call (when to restart).
 */
@Composable
fun InAppUpdateBanner() {
    if (!inAppUpdateSupported) return

    val strings = LocalStrings.current
    val launcher = rememberInAppUpdateLauncher()
    val readyToInstall by launcher.updateReadyToInstall.collectAsState()

    LaunchedEffect(Unit) { launcher.checkForUpdate() }

    AnimatedVisibility(visible = readyToInstall) {
        Box(modifier = Modifier.fillMaxWidth().padding(Dimens.space16), contentAlignment = Alignment.BottomCenter) {
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
                    TextButton(onClick = { launcher.completeUpdate() }) { Text(strings.updateRestartNow) }
                }
            }
        }
    }
}
