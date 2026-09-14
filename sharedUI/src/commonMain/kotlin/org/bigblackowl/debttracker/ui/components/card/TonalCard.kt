package org.bigblackowl.debttracker.ui.components.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.bigblackowl.debttracker.theme.Dimens

/**
 * The app's card taxonomy, distilled from every real `Card`/`OutlinedCard`/card-like `Surface`
 * usage across the UI (5 recurring shapes) — reach for these instead of a bare `Card`/`Surface`
 * so a single visual tweak (elevation, radius, border) applies everywhere at once.
 */

/** Tonal "card" built on [androidx.compose.material3.Surface] (no border) — profile suggestions, KPI tiles, clipboard-paste hints. */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier.Companion,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = contentColorFor(color),
    shape: Shape = RoundedCornerShape(Dimens.Radius.lg),
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color,
        contentColor = contentColor,
        shape = shape,
        shadowElevation = shadowElevation,
    ) {
        Box { content() }
    }
}