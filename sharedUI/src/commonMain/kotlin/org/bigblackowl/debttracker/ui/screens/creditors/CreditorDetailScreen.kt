package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.preview.PreviewIds
import org.bigblackowl.debttracker.ui.components.AmountBottomSheet
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.ContactDetailScaffold
import org.bigblackowl.debttracker.ui.components.FullScreenLoadingIndicator
import org.bigblackowl.debttracker.ui.components.TransactionEditSheet
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
    onEdit: () -> Unit = {},
    viewModel: CreditorDetailViewModel = koinViewModel { parametersOf(creditorId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showReturnSheet by remember { mutableStateOf(false) }
    var showBorrowSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<CreditorTransaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<CreditorTransaction?>(null) }
    val strings = LocalStrings.current
    val currency = state.creditor?.currency ?: Currency.UAH

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreditorDetailEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    if (state.isLoading) {
        FullScreenLoadingIndicator()
        return
    }

    ContactDetailScaffold(
        id = creditorId,
        title = state.creditor?.fullName ?: strings.creditorDetail.titleFallback,
        avatarUrl = state.creditor?.avatarUrl,
        onBack = onBack,
        exportLabel = strings.creditorDetail.export,
        onExport = onExport,
        onEdit = onEdit,
        snackbarHostState = snackbarHostState,
        phone = state.creditor?.phone,
        comment = state.creditor?.comment,
        balanceText = strings.creditorDetail.balance(state.balance.formatMoney(currency)),
        primaryLabel = strings.creditorDetail.returnLabel,
        onPrimary = { showReturnSheet = true },
        secondaryLabel = strings.creditorDetail.borrowMore,
        onSecondary = { showBorrowSheet = true },
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.onIntent(CreditorDetailIntent.Refresh) },
    ) {
        items(state.transactions, key = { it.id }) { transaction ->
            TransactionRow(
                amount = transaction.amount,
                method = transaction.method,
                comment = transaction.comment,
                date = transaction.date,
                currency = currency,
                onEdit = { editingTransaction = transaction },
                onDelete = { deletingTransaction = transaction },
            )
        }
    }

    editingTransaction?.let { tx ->
        TransactionEditSheet(
            initialAmount = tx.amount,
            initialMethod = tx.method,
            initialComment = tx.comment,
            initialDate = tx.date,
            currency = currency,
            onDismiss = { editingTransaction = null },
            onConfirm = { amount, method, comment, date ->
                viewModel.onIntent(CreditorDetailIntent.EditTransaction(tx.id, amount, method, comment, date))
                editingTransaction = null
            },
        )
    }

    deletingTransaction?.let { tx ->
        ConfirmDialog(
            title = strings.transactionEdit.deleteConfirmTitle,
            text = strings.transactionEdit.deleteConfirmText,
            confirmLabel = strings.delete,
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                viewModel.onIntent(CreditorDetailIntent.DeleteTransaction(tx.id))
                deletingTransaction = null
            },
            onDismiss = { deletingTransaction = null },
        )
    }

    if (showReturnSheet) {
        AmountBottomSheet(
            title = strings.creditorDetail.returnSheetTitle,
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
            title = strings.creditorDetail.borrowSheetTitle,
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
