package org.bigblackowl.debttracker.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Accent colors for amounts in debt operations (spec §7): one "alarm"
 * color for a growing debt (LEND/BORROW), one "positive" color for it
 * shrinking (REPAY/RETURN). Used instead of the theme's `error`/`primary`
 * directly, so the amount's color doesn't depend on error semantics.
 */
data class DebtAccentColors(
    val debt: Color,
    val repay: Color
)

internal val LightDebtAccentColors = DebtAccentColors(
    debt = ErrorLight,
    repay = Color(0xFF2E7D32)
)

internal val DarkDebtAccentColors = DebtAccentColors(
    debt = ErrorDark,
    repay = Color(0xFF81C995)
)

internal val LocalDebtAccentColors = staticCompositionLocalOf { LightDebtAccentColors }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.debtAccentColors: DebtAccentColors
    @Composable get() = LocalDebtAccentColors.current
