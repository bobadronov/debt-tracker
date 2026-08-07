package org.bigblackowl.debttracker.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase
import kotlin.time.Clock

/** StatsScreen: два окремих KPI, топ боржників/кредиторів, динаміка за місяць (спек §6, п.6). */
class StatsViewModel(
    private val observeDebtors: ObserveDebtorsUseCase,
    private val observeCreditors: ObserveCreditorsUseCase,
    private val observeDebtorTransactions: ObserveDebtorTransactionsUseCase,
    private val observeCreditorTransactions: ObserveCreditorTransactionsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeDebtors(), observeCreditors()) { debtors, creditors -> debtors to creditors }
                .collect { (debtors, creditors) ->
                    _state.value = _state.value.copy(isLoading = false, debtors = debtors, creditors = creditors)
                    loadMonthlyTrend(debtors.map { it.debtor.id }, creditors.map { it.creditor.id })
                }
        }
    }

    private suspend fun loadMonthlyTrend(debtorIds: List<String>, creditorIds: List<String>) {
        val timeZone = TimeZone.currentSystemDefault()
        val nowDate = Clock.System.now().toLocalDateTime(timeZone).date
        val months = (5 downTo 0).map { offset -> nowDate.minus(DatePeriod(months = offset)) }

        val debtSums = MutableList(months.size) { BigDecimal.ZERO }
        debtorIds.forEach { id ->
            observeDebtorTransactions(id).first().forEach { tx ->
                val txMonth = tx.date.toLocalDateTime(timeZone).date
                val index = months.indexOfFirst { it.year == txMonth.year && it.month.number == txMonth.month.number }
                if (index >= 0) debtSums[index] += tx.amount.negate()
            }
        }
        val creditorSums = MutableList(months.size) { BigDecimal.ZERO }
        creditorIds.forEach { id ->
            observeCreditorTransactions(id).first().forEach { tx ->
                val txMonth = tx.date.toLocalDateTime(timeZone).date
                val index = months.indexOfFirst { it.year == txMonth.year && it.month.number == txMonth.month.number }
                if (index >= 0) creditorSums[index] += tx.amount.negate()
            }
        }

        _state.value = _state.value.copy(
            monthlyDebtTrend = months.mapIndexed { i, date -> MonthlyPoint("${date.month.number}.${date.year}", debtSums[i]) },
            monthlyCreditorTrend = months.mapIndexed { i, date -> MonthlyPoint("${date.month.number}.${date.year}", creditorSums[i]) },
        )
    }
}
