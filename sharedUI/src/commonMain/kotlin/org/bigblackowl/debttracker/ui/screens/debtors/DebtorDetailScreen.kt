package org.bigblackowl.debttracker.ui.screens.debtors

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.TransactionType
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.preview.PreviewIds
import org.bigblackowl.debttracker.ui.components.contact.ContactDetailContent
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

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DebtorDetailEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    DebtorDetailContent(
        debtorId = debtorId,
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onExport = onExport,
        onEdit = onEdit,
        onRefresh = { viewModel.onIntent(DebtorDetailIntent.Refresh) },
        onEditTransaction = { id, amount, method, comment, date -> viewModel.onIntent(DebtorDetailIntent.EditTransaction(id, amount, method, comment, date)) },
        onDeleteTransaction = { viewModel.onIntent(DebtorDetailIntent.DeleteTransaction(it)) },
        onRepay = { amount, method -> viewModel.onIntent(DebtorDetailIntent.Repay(amount, method)) },
        onLendMore = { amount, method -> viewModel.onIntent(DebtorDetailIntent.LendMore(amount, method)) },
    )
}

@Composable
private fun DebtorDetailContent(
    debtorId: String,
    state: DebtorDetailState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onEditTransaction: (String, BigDecimal, PaymentMethod, String?, kotlin.time.Instant) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onRepay: (BigDecimal, PaymentMethod) -> Unit,
    onLendMore: (BigDecimal, PaymentMethod) -> Unit,
) {
    val strings = LocalStrings.current
    val currency = state.debtor?.currency ?: Currency.UAH

    ContactDetailContent(
        id = debtorId,
        title = state.debtor?.fullName ?: strings.debtorDetail.titleFallback,
        avatarUrl = state.debtor?.avatarUrl,
        phone = state.debtor?.phone,
        comment = state.debtor?.comment,
        isLoading = state.isLoading,
        isRefreshing = state.isRefreshing,
        snackbarHostState = snackbarHostState,
        balanceText = strings.debtorDetail.balance(state.balance.formatMoney(currency)),
        currency = currency,
        primaryLabel = strings.debtorDetail.repay,
        primarySheetTitle = strings.debtorDetail.repaySheetTitle,
        primaryPrefillAmount = if (state.balance > BigDecimal.ZERO) state.balance.toStringExpanded() else "",
        secondaryLabel = strings.debtorDetail.lendMore,
        secondarySheetTitle = strings.debtorDetail.lendSheetTitle,
        transactions = state.transactions,
        transactionKey = { it.id },
        transactionAmount = { it.amount },
        transactionMethod = { it.method },
        transactionComment = { it.comment },
        transactionDate = { it.date },
        onBack = onBack,
        onExport = onExport,
        onEdit = onEdit,
        onRefresh = onRefresh,
        onEditTransaction = { tx, amount, method, comment, date -> onEditTransaction(tx.id, amount, method, comment, date) },
        onDeleteTransaction = { onDeleteTransaction(it.id) },
        onPrimaryConfirm = onRepay,
        onSecondaryConfirm = onLendMore,
    )
}

private val PREVIEW_NOW = kotlin.time.Instant.parse("2026-08-15T00:00:00Z")

private val PREVIEW_DEBTOR = Debtor(
    id = PreviewIds.DEBTOR,
    fullName = "Тарас Шевченко",
    phone = "0501234567",
    email = null,
    avatarUrl = null,
    comment = null,
    createdAt = PREVIEW_NOW,
    updatedAt = PREVIEW_NOW,
    status = DebtStatus.ACTIVE,
    syncStatus = SyncStatus.SYNCED,
)

private val PREVIEW_TRANSACTIONS = listOf(
    DebtTransaction(
        id = "tx1",
        debtorId = PreviewIds.DEBTOR,
        amount = BigDecimal.parseString("-1000"),
        type = TransactionType.LEND,
        method = PaymentMethod.CASH,
        date = PREVIEW_NOW,
        comment = null,
        createdAt = PREVIEW_NOW,
        updatedAt = PREVIEW_NOW,
        syncStatus = SyncStatus.SYNCED,
    ),
    DebtTransaction(
        id = "tx2",
        debtorId = PreviewIds.DEBTOR,
        amount = BigDecimal.parseString("300"),
        type = TransactionType.REPAY,
        method = PaymentMethod.CARD,
        date = PREVIEW_NOW,
        comment = "Часткове погашення",
        createdAt = PREVIEW_NOW,
        updatedAt = PREVIEW_NOW,
        syncStatus = SyncStatus.SYNCED,
    ),
)

@Composable
private fun Preview(state: DebtorDetailState) = DebtorDetailContent(
    debtorId = PreviewIds.DEBTOR,
    state = state,
    snackbarHostState = remember { SnackbarHostState() },
    onBack = {},
    onExport = {},
    onEdit = {},
    onRefresh = {},
    onEditTransaction = { _, _, _, _, _ -> },
    onDeleteTransaction = {},
    onRepay = { _, _ -> },
    onLendMore = { _, _ -> },
)

private val PREVIEW_STATE = DebtorDetailState(isLoading = false, debtor = PREVIEW_DEBTOR, transactions = PREVIEW_TRANSACTIONS)

@Preview
@Composable
private fun DebtorDetailScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun DebtorDetailScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun DebtorDetailScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun DebtorDetailScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun DebtorDetailScreenLoadingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(DebtorDetailState(isLoading = true))
}

@Preview
@Composable
private fun DebtorDetailScreenNoTransactionsPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(DebtorDetailState(isLoading = false, debtor = PREVIEW_DEBTOR, transactions = emptyList()))
}

@Preview
@Composable
private fun DebtorDetailScreenRefreshingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE.copy(isRefreshing = true))
}
