package org.bigblackowl.debttracker.ui.screens.debtors

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
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.toDebtTransactionType
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.bigblackowl.debttracker.domain.usecase.debtor.AddDebtTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorUseCase

/** Combines the debtor's profile and transaction stream into [DebtorDetailState]; records new transactions on intent. */
@OptIn(ExperimentalUuidApi::class)
class DebtorDetailViewModel(
    private val debtorId: String,
    observeDebtor: ObserveDebtorUseCase,
    observeTransactions: ObserveDebtorTransactionsUseCase,
    private val addTransaction: AddDebtTransactionUseCase,
    private val syncStatusProvider: SyncStatusProvider,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val effectsChannel = Channel<DebtorDetailEffect>()
    val effects = effectsChannel.receiveAsFlow()

    val state: StateFlow<DebtorDetailState> = combine(
        observeDebtor(debtorId), observeTransactions(debtorId), isRefreshing,
    ) { debtor, transactions, refreshing ->
        DebtorDetailState(
            isLoading = false,
            isRefreshing = refreshing,
            debtor = debtor,
            transactions = transactions.sortedByDescending { it.date },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtorDetailState())

    fun onIntent(intent: DebtorDetailIntent) {
        when (intent) {
            is DebtorDetailIntent.Repay -> record(intent.amount, intent.method, intent.cardLastDigits)
            is DebtorDetailIntent.LendMore -> record(intent.amount.negate(), intent.method, intent.cardLastDigits)
            DebtorDetailIntent.Refresh -> refresh()
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

    private fun record(signedAmount: BigDecimal, method: PaymentMethod, cardLastDigits: String?) {
        viewModelScope.launch {
            runCatching {
                val now = Clock.System.now()
                addTransaction(
                    DebtTransaction(
                        id = Uuid.random().toString(),
                        debtorId = debtorId,
                        amount = signedAmount,
                        type = signedAmount.toDebtTransactionType(),
                        method = method,
                        cardLastDigits = cardLastDigits,
                        date = now,
                        comment = null,
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING,
                    )
                )
            }.onFailure {
                effectsChannel.send(DebtorDetailEffect.Error(resolveStrings(appSettings.locale).saveError))
            }
        }
    }
}
