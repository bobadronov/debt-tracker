package org.bigblackowl.debttracker.ui.screens.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.FiatCurrencies
import org.bigblackowl.debttracker.domain.model.FiatCurrency
import org.bigblackowl.debttracker.domain.model.RateSource
import org.bigblackowl.debttracker.domain.repository.ExchangeRatesRepository

/**
 * [ExchangeRatesScreen]: показує курси обраного [RateSource] у обраній базі. Джерело, база й
 * закріплені валюти памʼятаються в [AppSettings]. Відкривається з кешу, потім тихо оновлюється
 * з мережі; зміна джерела/бази та pull-to-refresh — той самий шлях [load]. Пошук і сума конвертера
 * ([setQuery]/[setAmount]) — лише стан, без мережі.
 */
class ExchangeRatesViewModel(
    private val repository: ExchangeRatesRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ExchangeRatesState> = _state.asStateFlow()

    init {
        load(_state.value.source, _state.value.base)
    }

    private fun initialState(): ExchangeRatesState {
        val source = settings.exchangeRatesSource
            ?.let { name -> RateSource.entries.firstOrNull { it.name == name } }
            ?: RateSource.PRIVATBANK
        val base = if (source.arbitraryBase) {
            settings.exchangeRatesBase?.let { FiatCurrencies.of(it) } ?: source.homeCurrency
        } else {
            source.homeCurrency
        }
        return ExchangeRatesState(source = source, base = base, pinned = readPinned())
    }

    fun selectSource(source: RateSource) {
        if (source == _state.value.source) return
        settings.exchangeRatesSource = source.name
        // Для банків база фіксована. Для джерел із довільною базою беремо збережений вибір користувача
        // (або дефолт джерела) — а не перенесену базу попереднього джерела, яку нове може не підтримувати.
        val base = when {
            !source.arbitraryBase -> source.homeCurrency
            _state.value.source.arbitraryBase -> _state.value.base
            else -> settings.exchangeRatesBase?.let { FiatCurrencies.of(it) } ?: source.homeCurrency
        }
        load(source, base)
    }

    fun selectBase(base: FiatCurrency) {
        if (!_state.value.source.arbitraryBase || base.code == _state.value.base.code) return
        settings.exchangeRatesBase = base.code
        load(_state.value.source, base)
    }

    fun refresh() = load(_state.value.source, _state.value.base)

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun setAmount(amount: String) {
        _state.value = _state.value.copy(amount = amount)
    }

    fun togglePin(code: String) {
        val next = _state.value.pinned.toMutableSet().apply { if (!add(code)) remove(code) }
        settings.exchangeRatesPinnedCsv = next.joinToString(",")
        _state.value = _state.value.copy(pinned = next)
    }

    private fun readPinned(): Set<String> =
        settings.exchangeRatesPinnedCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun load(source: RateSource, base: FiatCurrency) {
        val cached = repository.cached(source, base.code)
        _state.value = _state.value.copy(
            source = source,
            base = cached?.base ?: base,
            rates = cached?.rates.orEmpty(),
            date = cached?.date,
            isLoading = cached == null,
            isRefreshing = cached != null,
            error = false,
            stale = false,
        )
        viewModelScope.launch {
            runCatching { repository.refresh(source, base.code) }
                .onSuccess { snapshot ->
                    _state.value = _state.value.copy(
                        base = snapshot.base,
                        rates = snapshot.rates,
                        date = snapshot.date,
                        isLoading = false,
                        isRefreshing = false,
                        error = false,
                        stale = false,
                    )
                }
                .onFailure { error ->
                    Napier.w(tag = "ExchangeRates", throwable = error) { "refresh($source, ${base.code}) failed" }
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
