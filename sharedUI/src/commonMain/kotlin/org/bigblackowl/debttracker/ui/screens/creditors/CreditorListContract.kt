package org.bigblackowl.debttracker.ui.screens.creditors

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.CreditorWithBalance
import org.bigblackowl.debttracker.domain.model.sumByCurrency

/** MVI contract for [CreditorListScreen] — searchable/sortable/filterable creditor list. */
enum class CreditorSortOrder { NAME_ASC, BALANCE_DESC, RECENT }
enum class CreditorStatusFilter { ALL, ACTIVE, CLOSED }

data class CreditorListState(
    val isLoading: Boolean = true,
    val query: String = "",
    val sortOrder: CreditorSortOrder = CreditorSortOrder.NAME_ASC,
    val statusFilter: CreditorStatusFilter = CreditorStatusFilter.ACTIVE,
    val creditors: List<CreditorWithBalance> = emptyList(),
) {
    /** Немає курсів обміну — тотал рахується окремо на кожну валюту, що трапляється серед кредиторів. */
    val totalsByCurrency: Map<Currency, BigDecimal>
        get() = creditors.sumByCurrency({ it.creditor.currency }, { it.balance })
}

sealed interface CreditorListIntent {
    data class Search(val query: String) : CreditorListIntent
    data class ChangeSort(val order: CreditorSortOrder) : CreditorListIntent
    data class ChangeStatusFilter(val filter: CreditorStatusFilter) : CreditorListIntent
    data class Delete(val creditorId: String) : CreditorListIntent
}

sealed interface CreditorListEffect {
    data class Error(val message: String) : CreditorListEffect
}
