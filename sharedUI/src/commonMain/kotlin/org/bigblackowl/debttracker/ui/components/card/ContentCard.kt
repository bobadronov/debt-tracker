package org.bigblackowl.debttracker.ui.components.card

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.text.BodyText

/** Plain static container card — no elevation/shape overrides, just groups content (debt summary header, totals bar). */
@Composable
fun ContentCard(modifier: Modifier = Modifier.Companion, content: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) { content() }
}

@Preview
@Composable
private fun ContentCardLightPreview() = DebtTrackerPreview(darkTheme = false) {
    ContentCard { BodyText("Content card", modifier = Modifier.padding(Dimens.Spacing.lg)) }
}

@Preview
@Composable
private fun ContentCardDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    ContentCard { BodyText("Content card", modifier = Modifier.padding(Dimens.Spacing.lg)) }
}