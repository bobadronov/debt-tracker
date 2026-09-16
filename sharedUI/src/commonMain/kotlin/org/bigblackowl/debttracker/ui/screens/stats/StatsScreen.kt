package org.bigblackowl.debttracker.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.domain.model.formatTotals
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.EntityAvatar
import org.bigblackowl.debttracker.ui.components.FullScreenLoadingIndicator
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.card.TonalCard
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.bigblackowl.debttracker.ui.components.text.HeadingText
import org.bigblackowl.debttracker.ui.components.text.LabelText
import org.bigblackowl.debttracker.ui.components.text.TitleText
import org.koin.compose.viewmodel.koinViewModel

/** StatsScreen: two separate KPIs (no netting), top lists, monthly trend (spec §6, item 6). */
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    onOpenCreditor: (String) -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StatsContent(
        state = state,
        onBack = onBack,
        onOpenDebtor = onOpenDebtor,
        onOpenCreditor = onOpenCreditor,
    )
}

@Composable
private fun StatsContent(
    state: StatsState,
    onBack: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    onOpenCreditor: (String) -> Unit,
) {
    val strings = LocalStrings.current

    PlaceholderScreen(title = strings.stats.title, onBack = onBack) {
        if (state.isLoading) {
            FullScreenLoadingIndicator()
            return@PlaceholderScreen
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.md)
                ) {
                    KpiCard(
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        title = strings.stats.debtors,
                        value = state.totalDebtorsByCurrency,
                        modifier = Modifier.weight(1f),
                    )

                    KpiCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = strings.stats.creditors,
                        value = state.totalCreditorsByCurrency,
                        modifier = Modifier.weight(1f),
                    )
                }

                SettingsSection(strings.stats.topDebtors) {
                    state.topDebtors.forEachIndexed { index, item ->
                        TopRow(
                            rank = index + 1,
                            id = item.debtor.id,
                            name = item.debtor.fullName,
                            avatarUrl = item.debtor.avatarUrl,
                            balance = item.balance,
                            currency = item.debtor.currency,
                            onClick = { onOpenDebtor(item.debtor.id) },
                        )
                        if (index != state.topDebtors.lastIndex) SettingsRowDivider()
                    }
                }

                SettingsSection(strings.stats.topCreditors) {
                    state.topCreditors.forEachIndexed { index, item ->
                        TopRow(
                            rank = index + 1,
                            id = item.creditor.id,
                            name = item.creditor.fullName,
                            avatarUrl = item.creditor.avatarUrl,
                            balance = item.balance,
                            currency = item.creditor.currency,
                            onClick = { onOpenCreditor(item.creditor.id) },
                        )
                        if (index != state.topCreditors.lastIndex) SettingsRowDivider()
                    }
                }

                SettingsSection(strings.stats.monthlyDebtTrend) {
                    MonthlyBars(state.monthlyDebtTrend, modifier = Modifier.padding(Dimens.Spacing.lg))
                }

                SettingsSection(strings.stats.monthlyCreditorTrend) {
                    MonthlyBars(
                        state.monthlyCreditorTrend,
                        modifier = Modifier.padding(Dimens.Spacing.lg)
                    )
                }
            }
        }
    }
}

@Composable
private fun Preview(state: StatsState) = StatsContent(
    state = state,
    onBack = {},
    onOpenDebtor = {},
    onOpenCreditor = {},
)

private val PREVIEW_TREND = listOf(
    MonthlyPoint(month = 4, year = 2026, amount = BigDecimal.parseString("1200")),
    MonthlyPoint(month = 5, year = 2026, amount = BigDecimal.parseString("-800")),
    MonthlyPoint(month = 6, year = 2026, amount = BigDecimal.parseString("400")),
)

private val PREVIEW_STATE = StatsState(isLoading = false, monthlyDebtTrend = PREVIEW_TREND, monthlyCreditorTrend = PREVIEW_TREND)

@Preview
@Composable
private fun StatsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun StatsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun StatsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun StatsScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

/** Tonal KPI card with an icon in a circle — the same visual vocabulary as SettingsRow/SettingsSection. */
@Composable
private fun KpiCard(
    icon: ImageVector,
    title: String,
    value: Map<Currency, BigDecimal>,
    modifier: Modifier = Modifier
) {
    TonalCard(modifier = modifier, shape = RoundedCornerShape(Dimens.Radius.lg)) {
        Column(modifier = Modifier.padding(Dimens.Spacing.lg)) {
            Box(
                modifier = Modifier.size(Dimens.IconSize.md).clip(CircleShape)
                    .background(MaterialTheme.debtAccentColors.debt.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.debtAccentColors.debt,
                    modifier = Modifier.size(Dimens.IconSize.sm)
                )
            }
            Spacer(Modifier.height(Dimens.Spacing.md))
            LabelText(title)
            HeadingText(value.formatTotals(), color = MaterialTheme.debtAccentColors.debt)
        }
    }
}

/** Top-list row: contact avatar with a rank badge overlaid on it instead of a bare number. */
@Composable
private fun TopRow(rank: Int, id: String, name: String, avatarUrl: String?, balance: BigDecimal, currency: Currency, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Spacing.lg, vertical = Dimens.Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                EntityAvatar(id = id, name = name, avatarUrl = avatarUrl)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(Dimens.Spacing.lg)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    LabelText(
                        "$rank",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(Dimens.Spacing.md))
            TitleText(name, style = MaterialTheme.typography.bodyLarge)
        }
        BodyText(balance.formatMoney(currency), color = MaterialTheme.debtAccentColors.debt)
    }
}

@Composable
private fun MonthlyBars(points: List<MonthlyPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val strings = LocalStrings.current
    val maxAbs = points.maxOf { it.amount.abs().toStringExpanded().toDoubleOrNull() ?: 0.0 }
        .coerceAtLeast(1.0)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.sm)) {
        points.forEach { point ->
            val amountDouble = point.amount.toStringExpanded().toDoubleOrNull() ?: 0.0
            val targetFraction = (kotlin.math.abs(amountDouble) / maxAbs).toFloat().coerceIn(0f, 1f)
            val fraction by animateFloatAsState(
                targetFraction.coerceAtLeast(0.02f),
                label = "trend-bar"
            )
            val color =
                if (amountDouble >= 0) MaterialTheme.debtAccentColors.debt else MaterialTheme.debtAccentColors.repay
            val label = "${strings.stats.monthsShort[point.month - 1]} ${point.year % 100}"

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LabelText(
                    label,
                    modifier = Modifier.width(Dimens.IconSize.lg),
                    style = MaterialTheme.typography.labelSmall
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.Spacing.lg)
                        .clip(RoundedCornerShape(Dimens.Radius.sm))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(Dimens.Radius.sm))
                            .background(color),
                    )
                }
                Spacer(Modifier.width(Dimens.Spacing.sm))
                LabelText(point.amount.toStringExpanded(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
