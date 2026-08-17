package org.bigblackowl.debttracker.ui.screens.stats

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.domain.model.CreditorWithBalance
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance
import org.bigblackowl.debttracker.domain.model.sumByCurrency

/** One point on the 6-month debt/repay trend chart on [StatsScreen]. Month name is resolved at render time via [org.bigblackowl.debttracker.core.i18n.Strings]. */
data class MonthlyPoint(val month: Int, val year: Int, val amount: BigDecimal)

/**
 * Derived stats for [StatsScreen] — computed straight from the live debtor/creditor lists, no separate aggregation query.
 * Місячна динаміка (monthly*Trend) підсумовує суми напряму, без розбивки по валютах —
 * прийнятне спрощення, поки немає курсів обміну для кросвалютного графіка.
 */
data class StatsState(
    val isLoading: Boolean = true,
    val debtors: List<DebtorWithBalance> = emptyList(),
    val creditors: List<CreditorWithBalance> = emptyList(),
    val monthlyDebtTrend: List<MonthlyPoint> = emptyList(),
    val monthlyCreditorTrend: List<MonthlyPoint> = emptyList(),
) {
    // by lazy (not get()) — StatsScreen reads topDebtors/topCreditors twice each (list + lastIndex),
    // and re-sorting the full list on every access is wasted work once the state instance is stable.
    val totalDebtorsByCurrency: Map<Currency, BigDecimal> by lazy { debtors.sumByCurrency({ it.debtor.currency }, { it.balance }) }
    val totalCreditorsByCurrency: Map<Currency, BigDecimal> by lazy { creditors.sumByCurrency({ it.creditor.currency }, { it.balance }) }
    val topDebtors: List<DebtorWithBalance> by lazy { debtors.sortedWith { a, b -> b.balance.compareTo(a.balance) }.take(5) }
    val topCreditors: List<CreditorWithBalance> by lazy { creditors.sortedWith { a, b -> b.balance.compareTo(a.balance) }.take(5) }
}
