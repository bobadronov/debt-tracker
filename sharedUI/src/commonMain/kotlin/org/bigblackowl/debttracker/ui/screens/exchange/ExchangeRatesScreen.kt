package org.bigblackowl.debttracker.ui.screens.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.ExchangeRate
import org.bigblackowl.debttracker.domain.model.FiatCurrencies
import org.bigblackowl.debttracker.domain.model.FiatCurrency
import org.bigblackowl.debttracker.domain.model.RateSource
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.BackTopAppBar
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToLong

/**
 * Курс валют (⋮ меню). Обираєш джерело (ПриватБанк за замовчуванням, НБУ, Monobank, NBP, ECB, ČNB,
 * ExchangeRate-API — з логотипом) та, для джерел із довільною базою, базову валюту. Показуються всі
 * валюти джерела до бази; поле суми множить курси, пошук фільтрує список, зірка закріплює валюту
 * зверху. Екран відкривається з локального кешу останнього зрізу, тоді тихо оновлюється; при помилці
 * мережі лишаються збережені дані.
 */
@Composable
fun ExchangeRatesScreen(
    onBack: () -> Unit,
    viewModel: ExchangeRatesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExchangeRatesContent(
        state = state,
        onBack = onBack,
        onSelectSource = viewModel::selectSource,
        onSelectBase = viewModel::selectBase,
        onQueryChange = viewModel::setQuery,
        onAmountChange = viewModel::setAmount,
        onTogglePin = viewModel::togglePin,
        onRefresh = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExchangeRatesContent(
    state: ExchangeRatesState,
    onBack: () -> Unit,
    onSelectSource: (RateSource) -> Unit,
    onSelectBase: (FiatCurrency) -> Unit,
    onQueryChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val strings = LocalStrings.current.exchangeRates

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = { BackTopAppBar(title = strings.menuTitle, onBack = onBack) }) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight().width(Dimens.contentMaxWidth).padding(Dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space16),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SelectorRow(strings.sourceLabel) {
                        SourceSelector(selected = state.source, onSelect = onSelectSource)
                    }
                    SelectorRow(strings.baseLabel) {
                        BaseSelector(
                            selected = state.base,
                            enabled = state.baseSelectable,
                            onSelect = onSelectBase,
                        )
                    }

                    Text(
                        strings.quotedIn("${state.base.code} ${state.base.symbol}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    when {
                        state.isLoading -> Box(Modifier.fillMaxSize().padding(Dimens.space40), Alignment.Center) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space60))
                        }

                        state.error -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.space12, Alignment.CenterVertically),
                            modifier = Modifier.fillMaxSize().padding(Dimens.space24),
                        ) {
                            Text(strings.error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRefresh) { Text(strings.refresh) }
                        }

                        else -> {
                            state.date?.let {
                                Text(
                                    strings.updated(it.format()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.stale) {
                                Text(
                                    strings.stale,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }

                            OutlinedTextField(
                                value = state.amount,
                                onValueChange = onAmountChange,
                                label = { Text(strings.amountLabel) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                label = { Text(strings.searchHint) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            val q = state.query.trim()
                            val visible = state.rates.filter { rate ->
                                q.isEmpty() ||
                                        rate.currency.code.contains(q, ignoreCase = true) ||
                                        rate.currency.name.contains(q, ignoreCase = true)
                            }
                            val pinned = visible.filter { it.currency.code in state.pinned }
                            val rest = visible.filterNot { it.currency.code in state.pinned }

                            if (visible.isEmpty()) {
                                Text(
                                    strings.noResults,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(Dimens.space24),
                                )
                            }

                            if (pinned.isNotEmpty()) {
                                SectionLabel(strings.pinned)
                                pinned.forEach { rate ->
                                    RateRow(rate, state.amountFactor, pinned = true) { onTogglePin(rate.currency.code) }
                                }
                                if (rest.isNotEmpty()) Spacer(Modifier.size(Dimens.space8))
                            }
                            rest.forEach { rate ->
                                RateRow(rate, state.amountFactor, pinned = false) { onTogglePin(rate.currency.code) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        control()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SourceSelector(selected: RateSource, onSelect: (RateSource) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            SourceIcon(selected, Modifier.size(Dimens.space20))
            Spacer(Modifier.width(Dimens.space8))
            Text(selected.displayName)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RateSource.entries.forEach { source ->
                DropdownMenuItem(
                    text = { Text(source.displayName) },
                    leadingIcon = { SourceIcon(source, Modifier.size(Dimens.space24)) },
                    trailingIcon = if (source == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(source)
                    },
                )
            }
        }
    }
}

@Composable
private fun BaseSelector(selected: FiatCurrency, enabled: Boolean, onSelect: (FiatCurrency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
            CurrencyFlag(selected, Modifier.size(Dimens.space20))
            Spacer(Modifier.width(Dimens.space8))
            Text("${selected.code} ${selected.symbol}")
            if (enabled) Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FiatCurrencies.catalog.forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.code} — ${currency.name}") },
                    leadingIcon = { CurrencyFlag(currency, Modifier.size(Dimens.space24)) },
                    trailingIcon = if (currency.code == selected.code) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(currency)
                    },
                )
            }
        }
    }
}

/** Логотип банку — фавікон з його офіційного сайту (Coil кешує локально, тож після першого разу є й офлайн). */
@Composable
private fun SourceIcon(source: RateSource, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = "https://www.google.com/s2/favicons?sz=128&domain=${source.domain}",
        contentDescription = source.displayName,
        contentScale = ContentScale.Inside,
        modifier = Modifier.size(Dimens.space30),
        loading = { CircularWavyProgressIndicator() },
        error = { Icon(Icons.Filled.AccountBalance, contentDescription = null, modifier = modifier) },
    )
}

/** Прапор країни валюти з flagcdn.com (Coil кешує → офлайн після першого разу); запасна — загальна іконка. */
@Composable
private fun CurrencyFlag(currency: FiatCurrency, modifier: Modifier = Modifier) {
    val url = currency.flagUrl()
    val fallback: @Composable () -> Unit = {
        Icon(Icons.Filled.Payments, contentDescription = null, modifier = modifier)
    }
    if (url == null) {
        fallback()
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = currency.code,
            contentScale = ContentScale.Inside,
            loading = { CircularWavyProgressIndicator() },
            error = { fallback() },
        )
    }
}

@Composable
private fun RateRow(rate: ExchangeRate, factor: Double, pinned: Boolean, onTogglePin: () -> Unit) {
    val strings = LocalStrings.current.exchangeRates
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.space16),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Dimens.space8, end = Dimens.space16, top = Dimens.space8, bottom = Dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        if (pinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = strings.pinned,
                        tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CurrencyFlag(rate.currency, Modifier.size(Dimens.space24))
                Spacer(Modifier.width(Dimens.space12))
                Column {
                    Text(rate.currency.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${rate.currency.name} · ${rate.currency.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (rate.isSingle) {
                RateColumn(strings.official, rate.sell * factor)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space16)) {
                    RateColumn(strings.buy, rate.buy * factor)
                    RateColumn(strings.sell, rate.sell * factor)
                }
            }
        }
    }
}

@Composable
private fun RateColumn(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.formatRate(), style = MaterialTheme.typography.titleMedium)
    }
}

/** Округлення до 4 знаків, обрізаємо хвостові нулі, але лишаємо щонайменше два (11.5 → "11.50"). */
private fun Double.formatRate(): String {
    val rounded = (this * 10_000).roundToLong() / 10_000.0
    val raw = rounded.toString()
    val dot = raw.indexOf('.')
    if (dot < 0) return "$raw.00"
    val trimmed = raw.trimEnd('0')
    val decimals = trimmed.length - trimmed.indexOf('.') - 1
    return when {
        trimmed.endsWith('.') -> trimmed + "00"
        decimals == 1 -> trimmed + "0"
        else -> trimmed
    }
}

private fun LocalDate.format(): String {
    val d = day.toString().padStart(2, '0')
    val m = month.number.toString().padStart(2, '0')
    return "$d.$m.$year"
}

// --- previews (explicit state, no ViewModel) --------------------------------------------------

private val PREVIEW_RATES = listOf(
    ExchangeRate(FiatCurrencies.of("USD"), buy = 41.15, sell = 41.65),
    ExchangeRate(FiatCurrencies.of("EUR"), buy = 44.30, sell = 45.10),
    ExchangeRate(FiatCurrencies.of("GBP"), buy = 52.10, sell = 52.90),
    ExchangeRate(FiatCurrencies.of("PLN"), buy = 10.35, sell = 10.72),
    ExchangeRate(FiatCurrencies.of("CHF"), buy = 46.20, sell = 46.80),
)

@Composable
private fun Preview(state: ExchangeRatesState) = ExchangeRatesContent(
    state = state,
    onBack = {},
    onSelectSource = {},
    onSelectBase = {},
    onQueryChange = {},
    onAmountChange = {},
    onTogglePin = {},
    onRefresh = {},
)

@Preview
@Composable
private fun ExchangeRatesLoadedLightPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(
        ExchangeRatesState(
            source = RateSource.PRIVATBANK,
            base = FiatCurrencies.of("UAH"),
            rates = PREVIEW_RATES,
            pinned = setOf("GBP"),
            date = LocalDate(2026, 9, 1),
        ),
    )
}

@Preview
@Composable
private fun ExchangeRatesLoadedDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(
        ExchangeRatesState(
            source = RateSource.EXCHANGERATE_API,
            base = FiatCurrencies.of("USD"),
            rates = PREVIEW_RATES.map { it.copy(buy = it.sell) },
            amount = "100",
            date = LocalDate(2026, 9, 1),
        ),
    )
}

@Preview
@Composable
private fun ExchangeRatesLoadingLightPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ExchangeRatesState(isLoading = true))
}

@Preview
@Composable
private fun ExchangeRatesErrorLightPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ExchangeRatesState(error = true))
}

@Preview(device = DESKTOP)
@Composable
private fun ExchangeRatesLoadedDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(
        ExchangeRatesState(
            base = FiatCurrencies.of("EUR"),
            rates = PREVIEW_RATES,
            date = LocalDate(2026, 9, 1),
            stale = true,
        ),
    )
}
