package org.bigblackowl.debttracker.ui.components.card

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.bigblackowl.debttracker.theme.Dimens

/** Elevated banner-style card, width-capped like the rest of the app's centered content (update banners, prominent notices). */
@Composable
fun ElevatedInfoCard(modifier: Modifier = Modifier.Companion, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.widthIn(max = Dimens.contentMaxWidth),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.Spacing.sm),
    ) {
        content()
    }
}