package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Shared building blocks for DebtorDetailScreen/CreditorDetailScreen: same profile-card +
 * repay/borrow buttons + transaction history shell over a different domain model, mirroring
 * how [ContactListScaffold] does it for the list screens.
 */

/** Full screen shell: top bar with export action, profile card, primary/secondary action row, and a pull-to-refresh transaction list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScaffold(
    id: String,
    title: String,
    avatarUrl: String?,
    onBack: () -> Unit,
    exportLabel: String,
    onExport: () -> Unit,
    snackbarHostState: SnackbarHostState,
    phone: String?,
    comment: String?,
    balanceText: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    transactions: LazyListScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopAppBar(
                title = title,
                onBack = onBack,
                actions = { TextButton(onClick = onExport) { Text(exportLabel) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(modifier = Modifier.fillMaxWidth().padding(Dimens.space16)) {
                    Row(modifier = Modifier.padding(Dimens.space16), verticalAlignment = Alignment.CenterVertically) {
                        EntityAvatar(id = id, name = title, avatarUrl = avatarUrl, size = Dimens.space56)
                        Spacer(Modifier.width(Dimens.space12))
                        Column {
                            phone?.let { Text(it) }
                            comment?.let { Text(it) }
                            Spacer(Modifier.height(Dimens.space8))
                            Text(
                                balanceText,
                                color = MaterialTheme.debtAccentColors.debt,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space16),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                ) {
                    Button(onClick = onPrimary, modifier = Modifier.weight(1f)) { Text(primaryLabel) }
                    OutlinedButton(onClick = onSecondary, modifier = Modifier.weight(1f)) { Text(secondaryLabel) }
                }

                Spacer(Modifier.height(Dimens.space8))

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.space16),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space8),
                        content = transactions,
                    )
                }
            }
        }
    }
}

/** One transaction row: signed amount, optional comment, method + date — shared by debtor/creditor histories. */
@Composable
fun TransactionRow(
    amount: BigDecimal,
    method: PaymentMethod,
    comment: String?,
    createdAt: Instant,
    currency: Currency,
) {
    val color = if (amount.signum() > 0) {
        MaterialTheme.debtAccentColors.repay
    } else {
        MaterialTheme.debtAccentColors.debt
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.space16),
        border = BorderStroke(Dimens.space2, color.copy(alpha = .4f)),
    ) {
        Column(modifier = Modifier.padding(Dimens.space20).fillMaxSize()) {
            Text(amount.formatMoney(currency), color = color)
            comment?.let { Text(it) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(method.name)
                Text(createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.let {
                    "${it.day.toString().padStart(2, '0')}.${it.month.number.toString().padStart(2, '0')}.${it.year}"
                })
            }
        }
    }
}

private data class SampleTransaction(
    val id: String,
    val amount: BigDecimal,
    val method: PaymentMethod,
    val comment: String?,
    val createdAt: Instant,
)

private val sampleTransactions = listOf(
    SampleTransaction("1", BigDecimal.parseString("500"), PaymentMethod.CASH, "Repaid half", Clock.System.now()),
    SampleTransaction("2", BigDecimal.parseString("-1200"), PaymentMethod.CARD, null, Clock.System.now()),
)

@Composable
private fun ContactDetailComponentsSample() {
    val snackbarHostState = remember { SnackbarHostState() }
    ContactDetailScaffold(
        id = "1",
        title = "Олена Коваль",
        avatarUrl = null,
        onBack = {},
        exportLabel = "Export",
        onExport = {},
        snackbarHostState = snackbarHostState,
        phone = "+380 67 123 4567",
        comment = "Lent for repairs",
        balanceText = "Owes: 700 ₴",
        primaryLabel = "Repay",
        onPrimary = {},
        secondaryLabel = "Lend more",
        onSecondary = {},
        isRefreshing = false,
        onRefresh = {},
    ) {
        items(sampleTransactions, key = { it.id }) { tx ->
            TransactionRow(
                amount = tx.amount,
                method = tx.method,
                comment = tx.comment,
                createdAt = tx.createdAt,
                currency = Currency.UAH,
            )
        }
    }
}

@Preview
@Composable
private fun ContactDetailComponentsLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { ContactDetailComponentsSample() }

@Preview
@Composable
private fun ContactDetailComponentsDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { ContactDetailComponentsSample() }

@Preview(device = DESKTOP)
@Composable
private fun ContactDetailComponentsLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { ContactDetailComponentsSample() }

@Preview(device = DESKTOP)
@Composable
private fun ContactDetailComponentsDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { ContactDetailComponentsSample() }
