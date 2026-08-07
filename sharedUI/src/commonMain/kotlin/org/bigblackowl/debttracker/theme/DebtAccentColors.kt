package org.bigblackowl.debttracker.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Акцентні кольори для сум у боргових операціях (спек §7): один "тривожний"
 * колір для зростання боргу (LEND/BORROW), один "позитивний" для його
 * зменшення (REPAY/RETURN). Використовуються замість `error`/`primary` з
 * теми напряму, щоб колір суми не залежав від семантики помилки.
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

val MaterialTheme.debtAccentColors: DebtAccentColors
    @Composable get() = LocalDebtAccentColors.current
