package org.bigblackowl.debttracker.ui.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import org.bigblackowl.debttracker.theme.Dimens

/** Outlined card whose border carries semantic meaning (debt/repay/primary accent) — session, contact and transaction rows. */
@Composable
fun SemanticOutlinedCard(
    borderColor: Color,
    modifier: Modifier = Modifier.Companion,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Dimens.Radius.sm),
    borderWidth: Dp = Dimens.Border.thick,
    content: @Composable () -> Unit,
) {
    val border = BorderStroke(borderWidth, borderColor)
    if (onClick != null) {
        OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, border = border) { content() }
    } else {
        OutlinedCard(modifier = modifier.fillMaxWidth(), shape = shape, border = border) { content() }
    }
}