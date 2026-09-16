package org.bigblackowl.debttracker.ui.screens.exchange

import kotlinx.datetime.LocalDate
import org.bigblackowl.debttracker.domain.model.ExchangeRate
import org.bigblackowl.debttracker.domain.model.FiatCurrencies
import org.bigblackowl.debttracker.domain.model.FiatCurrency
import org.bigblackowl.debttracker.domain.model.RateSource

/** Row order for the rate list, on top of pinned-first. [DEFAULT] keeps the source's own order. */
enum class ExchangeRatesSort {
    DEFAULT,
    NAME,
}

/**
 * State of [ExchangeRatesScreen]. [rates] may come from the cache (then [isRefreshing] is true while
 * the network request is in flight) or be fresh. [error] — when there's nothing to show; [stale] —
 * when a refresh failed but a previous snapshot remains. [base] — the quoting currency (for banks it's
 * their home currency, for [RateSource.arbitraryBase] it's chosen by the user). [pinned] — pinned
 * codes (shown at the top, globally). [query] — search filter. [amount] — the converter amount
 * (each rate is multiplied by it, or divided when [invert]; empty/invalid = 1). [previousRates] — the
 * snapshot shown right before the current [rates] (the prior cache, or the prior refresh), used only
 * to draw a trend arrow; matched by currency code, never persisted.
 */
data class ExchangeRatesState(
    val source: RateSource = RateSource.PRIVATBANK,
    val base: FiatCurrency = FiatCurrencies.of("USD"),
    val rates: List<ExchangeRate> = emptyList(),
    val previousRates: List<ExchangeRate> = emptyList(),
    val pinned: Set<String> = emptySet(),
    val query: String = "",
    val amount: String = "1",
    val invert: Boolean = false,
    val sort: ExchangeRatesSort = ExchangeRatesSort.DEFAULT,
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

    /** [rate] converted by [amountFactor], in the direction picked by [invert]. */
    fun convert(rate: Double): Double = if (invert) {
        if (rate == 0.0) 0.0 else amountFactor / rate
    } else {
        amountFactor * rate
    }
}
