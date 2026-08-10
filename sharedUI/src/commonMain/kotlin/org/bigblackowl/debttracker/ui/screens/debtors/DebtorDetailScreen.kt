package org.bigblackowl.debttracker.ui.screens.debtors

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.preview.PreviewIds
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.AmountBottomSheet
import org.bigblackowl.debttracker.ui.components.BackButton
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Profile + transaction history for one [org.bigblackowl.debttracker.domain.model.Debtor], with "Repay"/"Lend more" actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtorDetailScreen(
    debtorId: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
    viewModel: DebtorDetailViewModel = koinViewModel { parametersOf(debtorId) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRepaySheet by remember { mutableStateOf(false) }
    var showLendSheet by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DebtorDetailEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.debtor?.fullName ?: strings.debtorDetailTitleFallback) },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = { TextButton(onClick = onExport) { Text(strings.debtorDetailExport) } },
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
                    Column(modifier = Modifier.padding(Dimens.space16)) {
                        state.debtor?.phone?.let { Text(it) }
                        state.debtor?.comment?.let { Text(it) }
                        Spacer(Modifier.height(Dimens.space8))
                        Text(
                            strings.debtorDetailBalance(
                                state.balance.formatMoney(
                                    state.debtor?.currency ?: Currency.UAH
                                )
                            ),
                            color = MaterialTheme.debtAccentColors.debt,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space16),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                ) {
                    Button(
                        onClick = { showRepaySheet = true },
                        modifier = Modifier.weight(1f)
                    ) { Text(strings.debtorDetailRepay) }

                    OutlinedButton(
                        onClick = { showLendSheet = true },
                        modifier = Modifier.weight(1f)
                    ) { Text(strings.debtorDetailLendMore) }
                }

                Spacer(Modifier.height(Dimens.space8))

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.onIntent(DebtorDetailIntent.Refresh) },
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.space16),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space8),
                    ) {
                        items(state.transactions, key = { it.id }) { transaction ->
                            DebtTransactionRow(transaction, state.debtor?.currency ?: Currency.UAH)
                        }
                    }
                }
            }
        }
    }

    if (showRepaySheet) {
        AmountBottomSheet(
            title = strings.debtorDetailRepaySheetTitle,
            prefillAmount = if (state.balance > BigDecimal.ZERO) state.balance.toStringExpanded() else "",
            currency = state.debtor?.currency ?: Currency.UAH,
            onDismiss = { showRepaySheet = false },
            onConfirm = { amount, method, cardDigits ->
                viewModel.onIntent(DebtorDetailIntent.Repay(amount, method, cardDigits))
                showRepaySheet = false
            },
        )
    }

    if (showLendSheet) {
        AmountBottomSheet(
            title = strings.debtorDetailLendSheetTitle,
            currency = state.debtor?.currency ?: Currency.UAH,
            onDismiss = { showLendSheet = false },
            onConfirm = { amount, method, cardDigits ->
                viewModel.onIntent(DebtorDetailIntent.LendMore(amount, method, cardDigits))
                showLendSheet = false
            },
        )
    }
}

@Preview
@Composable
private fun DebtorDetailScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    DebtorDetailScreen(debtorId = PreviewIds.DEBTOR, onBack = {}, onExport = {})
}

@Preview
@Composable
private fun DebtorDetailScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    DebtorDetailScreen(debtorId = PreviewIds.DEBTOR, onBack = {}, onExport = {})
}

@Preview(device = DESKTOP)
@Composable
private fun DebtorDetailScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    DebtorDetailScreen(debtorId = PreviewIds.DEBTOR, onBack = {}, onExport = {})
}

@Preview(device = DESKTOP)
@Composable
private fun DebtorDetailScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    DebtorDetailScreen(debtorId = PreviewIds.DEBTOR, onBack = {}, onExport = {})
}

@Composable
private fun DebtTransactionRow(transaction: DebtTransaction, currency: Currency) {
    val color = if (transaction.amount.signum() > 0) {
        MaterialTheme.debtAccentColors.repay
    } else {
        MaterialTheme.debtAccentColors.debt
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.space16),
        border = BorderStroke(Dimens.space1, color.copy(alpha = .4f)),
    ) {
        ListItem(
            headlineContent = { Text(transaction.amount.formatMoney(currency), color = color) },
            supportingContent = { Text(transaction.comment ?: transaction.method.name) },
        )
    }
}
