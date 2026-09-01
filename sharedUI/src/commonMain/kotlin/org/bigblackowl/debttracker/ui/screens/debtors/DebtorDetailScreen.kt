package org.bigblackowl.debttracker.ui.screens.debtors

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.preview.PreviewIds
import org.bigblackowl.debttracker.ui.components.AmountBottomSheet
import org.bigblackowl.debttracker.ui.components.ContactDetailScaffold
import org.bigblackowl.debttracker.ui.components.TransactionRow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Profile + transaction history for one [org.bigblackowl.debttracker.domain.model.Debtor], with "Repay"/"Lend more" actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtorDetailScreen(
    debtorId: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit = {},
    viewModel: DebtorDetailViewModel = koinViewModel { parametersOf(debtorId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRepaySheet by remember { mutableStateOf(false) }
    var showLendSheet by remember { mutableStateOf(false) }
    val strings = LocalStrings.current
    val currency = state.debtor?.currency ?: Currency.UAH

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DebtorDetailEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ContactDetailScaffold(
        id = debtorId,
        title = state.debtor?.fullName ?: strings.debtorDetailTitleFallback,
        avatarUrl = state.debtor?.avatarUrl,
        onBack = onBack,
        exportLabel = strings.debtorDetailExport,
        onExport = onExport,
        onEdit = onEdit.takeIf { state.debtor != null },
        snackbarHostState = snackbarHostState,
        phone = state.debtor?.phone,
        comment = state.debtor?.comment,
        balanceText = strings.debtorDetailBalance(state.balance.formatMoney(currency)),
        primaryLabel = strings.debtorDetailRepay,
        onPrimary = { showRepaySheet = true },
        secondaryLabel = strings.debtorDetailLendMore,
        onSecondary = { showLendSheet = true },
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.onIntent(DebtorDetailIntent.Refresh) },
    ) {
        items(state.transactions, key = { it.id }) { transaction ->
            TransactionRow(
                amount = transaction.amount,
                method = transaction.method,
                comment = transaction.comment,
                createdAt = transaction.createdAt,
                currency = currency,
            )
        }
    }

    if (showRepaySheet) {
        AmountBottomSheet(
            title = strings.debtorDetailRepaySheetTitle,
            prefillAmount = if (state.balance > BigDecimal.ZERO) state.balance.toStringExpanded() else "",
            currency = currency,
            onDismiss = { showRepaySheet = false },
            onConfirm = { amount, method ->
                viewModel.onIntent(DebtorDetailIntent.Repay(amount, method))
                showRepaySheet = false
            },
        )
    }

    if (showLendSheet) {
        AmountBottomSheet(
            title = strings.debtorDetailLendSheetTitle,
            currency = currency,
            onDismiss = { showLendSheet = false },
            onConfirm = { amount, method ->
                viewModel.onIntent(DebtorDetailIntent.LendMore(amount, method))
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
