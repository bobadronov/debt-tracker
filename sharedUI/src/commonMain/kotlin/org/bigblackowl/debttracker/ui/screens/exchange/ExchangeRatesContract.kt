package org.bigblackowl.debttracker.ui.screens.exchange

import kotlinx.datetime.LocalDate
import org.bigblackowl.debttracker.domain.model.ExchangeRate
import org.bigblackowl.debttracker.domain.model.FiatCurrencies
import org.bigblackowl.debttracker.domain.model.FiatCurrency
import org.bigblackowl.debttracker.domain.model.RateSource

/**
 * State of [ExchangeRatesScreen]. [rates] may come from the cache (then [isRefreshing] is true while
 * the network request is in flight) or be fresh. [error] — when there's nothing to show; [stale] —
 * when a refresh failed but a previous snapshot remains. [base] — the quoting currency (for banks it's
 * their home currency, for [RateSource.arbitraryBase] it's chosen by the user). [pinned] — pinned
 * codes (shown at the top, globally). [query] — search filter. [amount] — the converter amount
 * (each rate is multiplied by it; empty/invalid = 1).
 */
data class ExchangeRatesState(
    val source: RateSource = RateSource.PRIVATBANK,
    val base: FiatCurrency = FiatCurrencies.of("USD"),
    val rates: List<ExchangeRate> = emptyList(),
    val pinned: Set<String> = emptySet(),
    val query: String = "",
    val amount: String = "1",
    val date: LocalDate? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: Boolean = false,
    val stale: Boolean = false,
) {
    /** [amount] as a multiplier: an empty field or garbage → 1.0. */
    val amountFactor: Double get() = amount.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0

    /** Whether the base can be changed (only sources with an arbitrary base). */
    val baseSelectable: Boolean get() = source.arbitraryBase
}
