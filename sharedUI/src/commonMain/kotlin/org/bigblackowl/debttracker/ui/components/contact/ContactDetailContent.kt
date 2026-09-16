package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.transaction.AmountBottomSheet
import org.bigblackowl.debttracker.ui.components.transaction.TransactionEditSheet
import kotlin.time.Instant

/**
 * Full DebtorDetailScreen/CreditorDetailScreen body: [ContactDetailScaffold] + transaction history
 * + edit/delete dialogs + the two amount bottom sheets (repay/return, lend/borrow more). Each
 * screen only supplies its own strings, per-transaction field extractors, and intent callbacks —
 * the flow (which sheet is open, which row is being edited/deleted) lives here once.
 */
@Composable
fun <T> ContactDetailContent(
    id: String,
    title: String,
    avatarUrl: String?,
    phone: String?,
    comment: String?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    snackbarHostState: SnackbarHostState,
    balanceText: String,
    currency: Currency,
    primaryLabel: String,
    primarySheetTitle: String,
    primaryPrefillAmount: String,
    secondaryLabel: String,
    secondarySheetTitle: String,
    transactions: List<T>,
    transactionKey: (T) -> Any,
    transactionAmount: (T) -> BigDecimal,
    transactionMethod: (T) -> PaymentMethod,
    transactionComment: (T) -> String?,
    transactionDate: (T) -> Instant,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onEditTransaction: (T, BigDecimal, PaymentMethod, String?, Instant) -> Unit,
    onDeleteTransaction: (T) -> Unit,
    onPrimaryConfirm: (BigDecimal, PaymentMethod) -> Unit,
    onSecondaryConfirm: (BigDecimal, PaymentMethod) -> Unit,
) {
    var showPrimarySheet by remember { mutableStateOf(false) }
    var showSecondarySheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<T?>(null) }
    var deletingTransaction by remember { mutableStateOf<T?>(null) }
    val strings = LocalStrings.current

    ContactDetailScaffold(
        id = id,
        title = title,
        avatarUrl = avatarUrl,
        onBack = onBack,
        onExport = onExport,
        onEdit = onEdit,
        isLoading = isLoading,
        snackbarHostState = snackbarHostState,
        phone = phone,
        comment = comment,
        balanceText = balanceText,
        primaryLabel = primaryLabel,
        onPrimary = { showPrimarySheet = true },
        secondaryLabel = secondaryLabel,
        onSecondary = { showSecondarySheet = true },
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        items(transactions, key = transactionKey) { transaction ->
            TransactionRow(
                amount = transactionAmount(transaction),
                method = transactionMethod(transaction),
                comment = transactionComment(transaction),
                date = transactionDate(transaction),
                currency = currency,
                onEdit = { editingTransaction = transaction },
                onDelete = { deletingTransaction = transaction },
            )
        }
    }

    editingTransaction?.let { tx ->
        TransactionEditSheet(
            initialAmount = transactionAmount(tx),
            initialMethod = transactionMethod(tx),
            initialComment = transactionComment(tx),
            initialDate = transactionDate(tx),
            currency = currency,
            onDismiss = { editingTransaction = null },
            onConfirm = { amount, method, editedComment, date ->
                onEditTransaction(tx, amount, method, editedComment, date)
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
                onDeleteTransaction(tx)
                deletingTransaction = null
            },
            onDismiss = { deletingTransaction = null },
        )
    }

    if (showPrimarySheet) {
        AmountBottomSheet(
            title = primarySheetTitle,
            prefillAmount = primaryPrefillAmount,
            currency = currency,
            onDismiss = { showPrimarySheet = false },
            onConfirm = { amount, method ->
                onPrimaryConfirm(amount, method)
                showPrimarySheet = false
            },
        )
    }

    if (showSecondarySheet) {
        AmountBottomSheet(
            title = secondarySheetTitle,
            currency = currency,
            onDismiss = { showSecondarySheet = false },
            onConfirm = { amount, method ->
                onSecondaryConfirm(amount, method)
                showSecondarySheet = false
            },
        )
    }
}

@Composable
private fun ContactDetailContentSample(isLoading: Boolean = false, transactions: List<SampleTransaction> = sampleTransactions) {
    ContactDetailContent(
        id = "1",
        title = "Олена Коваль",
        avatarUrl = null,
        phone = "+380 67 123 4567",
        comment = "Позика на ремонт",
        isLoading = isLoading,
        isRefreshing = false,
        snackbarHostState = remember { SnackbarHostState() },
        balanceText = "700 ₴",
        currency = Currency.UAH,
        primaryLabel = "Repay",
        primarySheetTitle = "Repay",
        primaryPrefillAmount = "700",
        secondaryLabel = "Lend more",
        secondarySheetTitle = "Lend more",
        transactions = transactions,
        transactionKey = { it.id },
        transactionAmount = { it.amount },
        transactionMethod = { it.method },
        transactionComment = { it.comment },
        transactionDate = { it.createdAt },
        onBack = {},
        onExport = {},
        onEdit = {},
        onRefresh = {},
        onEditTransaction = { _, _, _, _, _ -> },
        onDeleteTransaction = {},
        onPrimaryConfirm = { _, _ -> },
        onSecondaryConfirm = { _, _ -> },
    )
}

@Preview
@Composable
private fun ContactDetailContentLoadingPreview() = DebtTrackerPreview(darkTheme = false) { ContactDetailContentSample(isLoading = true) }

@Preview
@Composable
private fun ContactDetailContentEmptyPreview() = DebtTrackerPreview(darkTheme = false) { ContactDetailContentSample(transactions = emptyList()) }

@Preview(device = DESKTOP)
@Composable
private fun ContactDetailContentDesktopPreview() = DebtTrackerPreview(darkTheme = false) { ContactDetailContentSample() }
