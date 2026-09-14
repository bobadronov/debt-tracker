package org.bigblackowl.debttracker.ui.components.card

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Plain static container card — no elevation/shape overrides, just groups content (debt summary header, totals bar). */
@Composable
fun ContentCard(modifier: Modifier = Modifier.Companion, content: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) { content() }
}