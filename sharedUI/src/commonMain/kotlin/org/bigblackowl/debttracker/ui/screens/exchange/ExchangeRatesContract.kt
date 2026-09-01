package org.bigblackowl.debttracker.ui.screens.exchange

import kotlinx.datetime.LocalDate
import org.bigblackowl.debttracker.domain.model.ExchangeRate
import org.bigblackowl.debttracker.domain.model.RateSource

/**
 * Стан [ExchangeRatesScreen]. [rates] можуть бути з кешу (тоді [isRefreshing] поки триває
 * мережевий запит) або свіжі. [error] — коли показати нема чого; [stale] — коли оновлення
 * впало, але лишився попередній зріз.
 */
data class ExchangeRatesState(
    val source: RateSource = RateSource.PRIVATBANK,
    val rates: List<ExchangeRate> = emptyList(),
    val date: LocalDate? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: Boolean = false,
    val stale: Boolean = false,
)
