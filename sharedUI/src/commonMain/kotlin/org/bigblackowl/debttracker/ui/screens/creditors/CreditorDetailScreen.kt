package org.bigblackowl.debttracker.ui.screens.creditors

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
import androidx.compose.ui.tooling.preview.Devices
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

/** Profile + transaction history for one [org.bigblackowl.debttracker.domain.model.Creditor], with "Return"/"Borrow more" actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditorDetailScreen(
    creditorId: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
    viewModel: CreditorDetailViewModel = koinViewModel { parametersOf(creditorId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showReturnSheet by remember { mutableStateOf(false) }
    var showBorrowSheet by remember { mutableStateOf(false) }
    val strings = LocalStrings.current
    val currency = state.creditor?.currency ?: Currency.UAH

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreditorDetailEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ContactDetailScaffold(
        id = creditorId,
        title = state.creditor?.fullName ?: strings.creditorDetailTitleFallback,
        avatarUrl = state.creditor?.avatarUrl,
        onBack = onBack,
        exportLabel = strings.creditorDetailExport,
        onExport = onExport,
        snackbarHostState = snackbarHostState,
        phone = state.creditor?.phone,
        comment = state.creditor?.comment,
        balanceText = strings.creditorDetailBalance(state.balance.formatMoney(currency)),
        primaryLabel = strings.creditorDetailReturn,
        onPrimary = { showReturnSheet = true },
        secondaryLabel = strings.creditorDetailBorrowMore,
        onSecondary = { showBorrowSheet = true },
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.onIntent(CreditorDetailIntent.Refresh) },
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

    if (showReturnSheet) {
        AmountBottomSheet(
            title = strings.creditorDetailReturnSheetTitle,
            prefillAmount = if (state.balance > BigDecimal.ZERO) state.balance.toStringExpanded() else "",
            currency = currency,
            onDismiss = { showReturnSheet = false },
            onConfirm = { amount, method ->
                viewModel.onIntent(CreditorDetailIntent.Return(amount, method))
                showReturnSheet = false
            },
        )
    }

    if (showBorrowSheet) {
        AmountBottomSheet(
            title = strings.creditorDetailBorrowSheetTitle,
            currency = currency,
            onDismiss = { showBorrowSheet = false },
            onConfirm = { amount, method ->
                viewModel.onIntent(CreditorDetailIntent.BorrowMore(amount, method))
                showBorrowSheet = false
            },
        )
    }
}

@Preview
@Composable
private fun CreditorDetailScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    CreditorDetailScreen(creditorId = PreviewIds.CREDITOR, onBack = {}, onExport = {})
}

@Preview
@Composable
private fun CreditorDetailScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    CreditorDetailScreen(creditorId = PreviewIds.CREDITOR, onBack = {}, onExport = {})
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun CreditorDetailScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    CreditorDetailScreen(creditorId = PreviewIds.CREDITOR, onBack = {}, onExport = {})
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun CreditorDetailScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    CreditorDetailScreen(creditorId = PreviewIds.CREDITOR, onBack = {}, onExport = {})
}
