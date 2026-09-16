package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.MyDebtTransactionType
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.preview.PreviewIds
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.contact.ContactDetailScaffold
import org.bigblackowl.debttracker.ui.components.contact.TransactionRow
import org.bigblackowl.debttracker.ui.components.transaction.AmountBottomSheet
import org.bigblackowl.debttracker.ui.components.transaction.TransactionEditSheet
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

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreditorDetailEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    CreditorDetailContent(
        creditorId = creditorId,
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onExport = onExport,
        onEdit = onEdit,
        onRefresh = { viewModel.onIntent(CreditorDetailIntent.Refresh) },
        onEditTransaction = { id, amount, method, comment, date -> viewModel.onIntent(CreditorDetailIntent.EditTransaction(id, amount, method, comment, date)) },
        onDeleteTransaction = { viewModel.onIntent(CreditorDetailIntent.DeleteTransaction(it)) },
        onReturn = { amount, method -> viewModel.onIntent(CreditorDetailIntent.Return(amount, method)) },
        onBorrowMore = { amount, method -> viewModel.onIntent(CreditorDetailIntent.BorrowMore(amount, method)) },
    )
}

@Composable
private fun CreditorDetailContent(
    creditorId: String,
    state: CreditorDetailState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onEditTransaction: (String, BigDecimal, PaymentMethod, String?, kotlin.time.Instant) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onReturn: (BigDecimal, PaymentMethod) -> Unit,
    onBorrowMore: (BigDecimal, PaymentMethod) -> Unit,
) {
    var showReturnSheet by remember { mutableStateOf(false) }
    var showBorrowSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<CreditorTransaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<CreditorTransaction?>(null) }
    val strings = LocalStrings.current
    val currency = state.creditor?.currency ?: Currency.UAH

    ContactDetailScaffold(
        id = creditorId,
        title = state.creditor?.fullName ?: strings.creditorDetail.titleFallback,
        avatarUrl = state.creditor?.avatarUrl,
        onBack = onBack,
        onExport = onExport,
        onEdit = onEdit,
        isLoading = state.isLoading,
        snackbarHostState = snackbarHostState,
        phone = state.creditor?.phone,
        comment = state.creditor?.comment,
        balanceText = strings.creditorDetail.balance(state.balance.formatMoney(currency)),
        primaryLabel = strings.creditorDetail.returnLabel,
        onPrimary = { showReturnSheet = true },
        secondaryLabel = strings.creditorDetail.borrowMore,
        onSecondary = { showBorrowSheet = true },
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
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
                onEditTransaction(tx.id, amount, method, comment, date)
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
                onDeleteTransaction(tx.id)
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
                onReturn(amount, method)
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
                onBorrowMore(amount, method)
                showBorrowSheet = false
            },
        )
    }
}

private val PREVIEW_NOW = kotlin.time.Instant.parse("2026-08-15T00:00:00Z")

private val PREVIEW_CREDITOR = Creditor(
    id = PreviewIds.CREDITOR,
    fullName = "Марія Шевченко",
    phone = "0671112233",
    email = null,
    avatarUrl = null,
    comment = null,
    createdAt = PREVIEW_NOW,
    updatedAt = PREVIEW_NOW,
    status = DebtStatus.ACTIVE,
    syncStatus = SyncStatus.SYNCED,
)

private val PREVIEW_TRANSACTIONS = listOf(
    CreditorTransaction(
        id = "tx1",
        creditorId = PreviewIds.CREDITOR,
        amount = BigDecimal.parseString("-3000"),
        type = MyDebtTransactionType.BORROW,
        method = PaymentMethod.CARD,
        date = PREVIEW_NOW,
        comment = "Позика на авто",
        createdAt = PREVIEW_NOW,
        updatedAt = PREVIEW_NOW,
        syncStatus = SyncStatus.SYNCED,
    ),
    CreditorTransaction(
        id = "tx2",
        creditorId = PreviewIds.CREDITOR,
        amount = BigDecimal.parseString("1000"),
        type = MyDebtTransactionType.RETURN,
        method = PaymentMethod.CASH,
        date = PREVIEW_NOW,
        comment = "Часткове повернення",
        createdAt = PREVIEW_NOW,
        updatedAt = PREVIEW_NOW,
        syncStatus = SyncStatus.SYNCED,
    ),
)

@Composable
private fun Preview(state: CreditorDetailState) = CreditorDetailContent(
    creditorId = PreviewIds.CREDITOR,
    state = state,
    snackbarHostState = remember { SnackbarHostState() },
    onBack = {},
    onExport = {},
    onEdit = {},
    onRefresh = {},
    onEditTransaction = { _, _, _, _, _ -> },
    onDeleteTransaction = {},
    onReturn = { _, _ -> },
    onBorrowMore = { _, _ -> },
)

private val PREVIEW_STATE = CreditorDetailState(isLoading = false, creditor = PREVIEW_CREDITOR, transactions = PREVIEW_TRANSACTIONS)

@Preview
@Composable
private fun CreditorDetailScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun CreditorDetailScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun CreditorDetailScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun CreditorDetailScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}
