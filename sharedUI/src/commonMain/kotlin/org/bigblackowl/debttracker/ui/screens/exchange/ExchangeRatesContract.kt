package org.bigblackowl.debttracker.ui.screens.exchange

import kotlinx.datetime.LocalDate
import org.bigblackowl.debttracker.domain.model.ExchangeRate
import org.bigblackowl.debttracker.domain.model.FiatCurrencies
import org.bigblackowl.debttracker.domain.model.FiatCurrency
import org.bigblackowl.debttracker.domain.model.RateSource

/**
 * Стан [ExchangeRatesScreen]. [rates] можуть бути з кешу (тоді [isRefreshing] поки триває мережевий
 * запит) або свіжі. [error] — коли показати нема чого; [stale] — коли оновлення впало, але лишився
 * попередній зріз. [base] — валюта котирування (для банків = їх домашня, для [RateSource.arbitraryBase]
 * — обрана користувачем). [pinned] — закріплені коди (показуються зверху, глобально). [query] —
 * фільтр пошуку. [amount] — сума конвертера (кожен курс множиться на неї; порожнє/невалідне = 1).
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
    /** [amount] як множник: порожнє поле чи сміття → 1.0. */
    val amountFactor: Double get() = amount.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0

    /** Чи можна міняти базу (лише джерела з довільною базою). */
    val baseSelectable: Boolean get() = source.arbitraryBase
}
