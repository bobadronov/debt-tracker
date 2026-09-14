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
    val sortAscending: Boolean = true,
    val statusFilter: DebtorStatusFilter = DebtorStatusFilter.ACTIVE,
    val debtors: List<DebtorWithBalance> = emptyList(),
) {
    /** No exchange rates — the total is calculated separately for each currency found among debtors. */
    val totalsByCurrency: Map<Currency, BigDecimal>
        get() = debtors.sumByCurrency({ it.debtor.currency }, { it.balance })
}

sealed interface DebtorListIntent {
    data class Search(val query: String) : DebtorListIntent
    data class ChangeSort(val order: DebtorSortOrder) : DebtorListIntent
    data object ToggleSortDirection : DebtorListIntent
    data class ChangeStatusFilter(val filter: DebtorStatusFilter) : DebtorListIntent
    data class Delete(val debtorId: String) : DebtorListIntent
    data object Refresh : DebtorListIntent
}

sealed interface DebtorListEffect {
    data class Error(val message: String) : DebtorListEffect
}
