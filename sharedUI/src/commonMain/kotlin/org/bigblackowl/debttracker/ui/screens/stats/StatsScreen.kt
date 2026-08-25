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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.viewmodel.koinViewModel

/** StatsScreen: два окремих KPI (без взаємозаліку), топи, динаміка за місяць (спек §6, п.6). */
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    onOpenCreditor: (String) -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    PlaceholderScreen(title = strings.statsTitle, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space12)
                ) {
                    KpiCard(
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        title = strings.statsDebtors,
                        value = state.totalDebtorsByCurrency,
                        modifier = Modifier.weight(1f),
                    )

                    KpiCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = strings.statsCreditors,
                        value = state.totalCreditorsByCurrency,
                        modifier = Modifier.weight(1f),
                    )
                }

                SettingsSection(strings.statsTopDebtors) {
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

                SettingsSection(strings.statsTopCreditors) {
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

                SettingsSection(strings.statsMonthlyDebtTrend) {
                    MonthlyBars(state.monthlyDebtTrend, modifier = Modifier.padding(Dimens.space16))
                }

                SettingsSection(strings.statsMonthlyCreditorTrend) {
                    MonthlyBars(
                        state.monthlyCreditorTrend,
                        modifier = Modifier.padding(Dimens.space16)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun StatsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    StatsScreen(onBack = {}, onOpenDebtor = {}, onOpenCreditor = {})
}

@Preview
@Composable
private fun StatsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    StatsScreen(onBack = {}, onOpenDebtor = {}, onOpenCreditor = {})
}

@Preview(device = DESKTOP)
@Composable
private fun StatsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    StatsScreen(onBack = {}, onOpenDebtor = {}, onOpenCreditor = {})
}

@Preview(device = DESKTOP)
@Composable
private fun StatsScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    StatsScreen(onBack = {}, onOpenDebtor = {}, onOpenCreditor = {})
}

/** Tonal-картка KPI з іконкою в колі — той самий візуальний словник, що й SettingsRow/SettingsSection. */
@Composable
private fun KpiCard(
    icon: ImageVector,
    title: String,
    value: Map<Currency, BigDecimal>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.space20),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(Dimens.space16)) {
            Box(
                modifier = Modifier.size(Dimens.space40).clip(CircleShape)
                    .background(MaterialTheme.debtAccentColors.debt.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.debtAccentColors.debt,
                    modifier = Modifier.size(Dimens.space20)
                )
            }
            Spacer(Modifier.height(Dimens.space12))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value.formatTotals(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.debtAccentColors.debt,
            )
        }
    }
}

/** Рядок топ-списку: аватарка контакту з бейджем місця поверх неї замість голого номера. */
@Composable
private fun TopRow(rank: Int, id: String, name: String, avatarUrl: String?, balance: BigDecimal, currency: Currency, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.space16, vertical = Dimens.space12),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                EntityAvatar(id = id, name = name, avatarUrl = avatarUrl)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(Dimens.space16)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$rank",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(Dimens.space12))
            Text(name, style = MaterialTheme.typography.bodyLarge)
        }
        Text(balance.formatMoney(currency), color = MaterialTheme.debtAccentColors.debt)
    }
}

@Composable
private fun MonthlyBars(points: List<MonthlyPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val strings = LocalStrings.current
    val maxAbs = points.maxOf { it.amount.abs().toStringExpanded().toDoubleOrNull() ?: 0.0 }
        .coerceAtLeast(1.0)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
        points.forEach { point ->
            val amountDouble = point.amount.toStringExpanded().toDoubleOrNull() ?: 0.0
            val targetFraction = (kotlin.math.abs(amountDouble) / maxAbs).toFloat().coerceIn(0f, 1f)
            val fraction by animateFloatAsState(
                targetFraction.coerceAtLeast(0.02f),
                label = "trend-bar"
            )
            val color =
                if (amountDouble >= 0) MaterialTheme.debtAccentColors.debt else MaterialTheme.debtAccentColors.repay
            val label = "${strings.monthsShort[point.month - 1]} ${point.year % 100}"

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    label,
                    modifier = Modifier.width(Dimens.space56),
                    style = MaterialTheme.typography.labelSmall
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.space14)
                        .clip(RoundedCornerShape(Dimens.space8))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(Dimens.space8))
                            .background(color),
                    )
                }
                Spacer(Modifier.width(Dimens.space8))
                Text(point.amount.toStringExpanded(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
