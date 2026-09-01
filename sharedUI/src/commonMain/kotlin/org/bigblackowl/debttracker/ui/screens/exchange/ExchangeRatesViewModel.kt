package org.bigblackowl.debttracker.ui.screens.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.domain.model.RateSource
import org.bigblackowl.debttracker.domain.repository.ExchangeRatesRepository

/**
 * [ExchangeRatesScreen]: показує курси обраного [RateSource] (за замовчуванням ПриватБанк).
 * Відкривається з кешу, потім тихо оновлюється з мережі; зміна джерела чи pull-to-refresh —
 * той самий шлях [load].
 */
class ExchangeRatesViewModel(
    private val repository: ExchangeRatesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExchangeRatesState())
    val state: StateFlow<ExchangeRatesState> = _state.asStateFlow()

    init {
        load(RateSource.PRIVATBANK)
    }

    fun selectSource(source: RateSource) {
        if (source != _state.value.source) load(source)
    }

    fun refresh() = load(_state.value.source)

    private fun load(source: RateSource) {
        val cached = repository.cached(source)
        _state.value = ExchangeRatesState(
            source = source,
            rates = cached?.rates.orEmpty(),
            date = cached?.date,
            isLoading = cached == null,
            isRefreshing = cached != null,
        )
        viewModelScope.launch {
            runCatching { repository.refresh(source) }
                .onSuccess { snapshot ->
                    _state.value = _state.value.copy(
                        rates = snapshot.rates,
                        date = snapshot.date,
                        isLoading = false,
                        isRefreshing = false,
                        error = false,
                        stale = false,
                    )
                }
                .onFailure { error ->
                    Napier.w(tag = "ExchangeRates", throwable = error) { "refresh($source) failed" }
                    val hasData = _state.value.rates.isNotEmpty()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = !hasData,
                        stale = hasData,
                    )
                }
        }
    }
}
