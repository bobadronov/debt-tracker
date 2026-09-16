package org.bigblackowl.debttracker.ui.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.text.BodyText

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

@Preview
@Composable
private fun SemanticOutlinedCardDebtPreview() = DebtTrackerPreview(darkTheme = false) {
    SemanticOutlinedCard(borderColor = MaterialTheme.debtAccentColors.debt) {
        BodyText("Owes 500 UAH", modifier = Modifier.padding(Dimens.Spacing.lg))
    }
}

@Preview
@Composable
private fun SemanticOutlinedCardRepayPreview() = DebtTrackerPreview(darkTheme = false) {
    SemanticOutlinedCard(borderColor = MaterialTheme.debtAccentColors.repay, onClick = {}) {
        BodyText("Repaid 500 UAH", modifier = Modifier.padding(Dimens.Spacing.lg))
    }
}

@Preview
@Composable
private fun SemanticOutlinedCardDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    SemanticOutlinedCard(borderColor = MaterialTheme.debtAccentColors.debt) {
        BodyText("Owes 500 UAH", modifier = Modifier.padding(Dimens.Spacing.lg))
    }
}