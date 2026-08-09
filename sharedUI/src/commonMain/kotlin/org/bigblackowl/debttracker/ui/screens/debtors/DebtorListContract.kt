package org.bigblackowl.debttracker.ui.screens.debtors

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance
import org.bigblackowl.debttracker.domain.model.sumByCurrency

/** MVI contract for [DebtorListScreen] — searchable/sortable/filterable debtor list. */
enum class DebtorSortOrder { NAME_ASC, BALANCE_DESC, RECENT }
enum class DebtorStatusFilter { ALL, ACTIVE, CLOSED }

data class DebtorListState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val sortOrder: DebtorSortOrder = DebtorSortOrder.NAME_ASC,
    val statusFilter: DebtorStatusFilter = DebtorStatusFilter.ACTIVE,
    val debtors: List<DebtorWithBalance> = emptyList(),
) {
    /** Немає курсів обміну — тотал рахується окремо на кожну валюту, що трапляється серед боржників. */
    val totalsByCurrency: Map<Currency, BigDecimal>
        get() = debtors.sumByCurrency({ it.debtor.currency }, { it.balance })
}

sealed interface DebtorListIntent {
    data class Search(val query: String) : DebtorListIntent
    data class ChangeSort(val order: DebtorSortOrder) : DebtorListIntent
    data class ChangeStatusFilter(val filter: DebtorStatusFilter) : DebtorListIntent
    data class Delete(val debtorId: String) : DebtorListIntent
    data object Refresh : DebtorListIntent
}

sealed interface DebtorListEffect {
    data class Error(val message: String) : DebtorListEffect
}
