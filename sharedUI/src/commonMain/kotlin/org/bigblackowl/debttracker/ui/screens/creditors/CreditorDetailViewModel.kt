package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.toCreditorTransactionType
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.bigblackowl.debttracker.domain.usecase.creditor.AddCreditorTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.DeleteCreditorTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.UpdateCreditorTransactionUseCase

/** Combines the creditor's profile and transaction stream into [CreditorDetailState]; records new transactions on intent. */
@OptIn(ExperimentalUuidApi::class)
class CreditorDetailViewModel(
    private val creditorId: String,
    observeCreditor: ObserveCreditorUseCase,
    observeTransactions: ObserveCreditorTransactionsUseCase,
    private val addTransaction: AddCreditorTransactionUseCase,
    private val updateTransaction: UpdateCreditorTransactionUseCase,
    private val deleteTransaction: DeleteCreditorTransactionUseCase,
    private val syncStatusProvider: SyncStatusProvider,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val effectsChannel = Channel<CreditorDetailEffect>()
    val effects = effectsChannel.receiveAsFlow()

    val state: StateFlow<CreditorDetailState> = combine(
        observeCreditor(creditorId), observeTransactions(creditorId), isRefreshing,
    ) { creditor, transactions, refreshing ->
        CreditorDetailState(
            isLoading = false,
            isRefreshing = refreshing,
            creditor = creditor,
            transactions = transactions.sortedByDescending { it.date },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreditorDetailState())

    fun onIntent(intent: CreditorDetailIntent) {
        when (intent) {
            is CreditorDetailIntent.Return -> record(intent.amount, intent.method)
            is CreditorDetailIntent.BorrowMore -> record(intent.amount.negate(), intent.method)
            is CreditorDetailIntent.EditTransaction -> editTransaction(intent)
            is CreditorDetailIntent.DeleteTransaction -> removeTransaction(intent.transactionId)
            CreditorDetailIntent.Refresh -> refresh()
        }
    }

    private fun editTransaction(intent: CreditorDetailIntent.EditTransaction) {
        val original = state.value.transactions.find { it.id == intent.transactionId } ?: return
        // Keep the borrow/return direction of the original; only its magnitude is edited.
        val signed = if (original.amount.signum() > 0) intent.amount else intent.amount.negate()
        viewModelScope.launch {
            runCatching {
                updateTransaction(
                    original.copy(
                        amount = signed,
                        type = signed.toCreditorTransactionType(),
                        method = intent.method,
                        comment = intent.comment?.trim()?.ifBlank { null },
                        date = intent.date,
                        updatedAt = Clock.System.now(),
                        syncStatus = SyncStatus.PENDING,
                    )
                )
            }.onFailure {
                effectsChannel.send(CreditorDetailEffect.Error(resolveStrings(appSettings.locale).saveError))
            }
        }
    }

    private fun removeTransaction(id: String) {
        viewModelScope.launch {
            runCatching { deleteTransaction(id) }.onFailure {
                effectsChannel.send(CreditorDetailEffect.Error(resolveStrings(appSettings.locale).deleteError))
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            runCatching { syncStatusProvider.refreshNow() }
            delay(300)
            isRefreshing.value = false
        }
    }

    private fun record(signedAmount: BigDecimal, method: PaymentMethod) {
        viewModelScope.launch {
            runCatching {
                val now = Clock.System.now()
                addTransaction(
                    CreditorTransaction(
                        id = Uuid.random().toString(),
                        creditorId = creditorId,
                        amount = signedAmount,
                        type = signedAmount.toCreditorTransactionType(),
                        method = method,
                        date = now,
                        comment = null,
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING,
                    )
                )
            }.onFailure {
                effectsChannel.send(CreditorDetailEffect.Error(resolveStrings(appSettings.locale).saveError))
            }
        }
    }
}
