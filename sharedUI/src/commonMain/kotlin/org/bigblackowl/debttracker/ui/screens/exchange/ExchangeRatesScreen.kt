package org.bigblackowl.debttracker.ui.screens.exchange

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import debt_tracker.sharedui.generated.resources.Res
import debt_tracker.sharedui.generated.resources.flag_czech_republic
import debt_tracker.sharedui.generated.resources.flag_poland
import debt_tracker.sharedui.generated.resources.flag_ukraine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.ExchangeRate
import org.bigblackowl.debttracker.domain.model.RateSource
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.BackTopAppBar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToLong

/**
 * Курс валют (⋮ меню). Обираєш джерело у випадному списку (ПриватБанк за замовчуванням, НБУ,
 * Monobank, NBP, ECB, ČNB — з логотипом банку) — показуються курси валют застосунку до бази цього
 * джерела ([RateSource.baseCode]). Екран відкривається з локального кешу останнього зрізу, тоді
 * тихо оновлюється; при помилці мережі лишаються збережені дані.
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
        onRefresh = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExchangeRatesContent(
    state: ExchangeRatesState,
    onBack: () -> Unit,
    onSelectSource: (RateSource) -> Unit,
    onRefresh: () -> Unit,
) {
    val strings = LocalStrings.current.exchangeRates

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = { BackTopAppBar(title = strings.menuTitle, onBack = onBack) }) { padding ->
        // Pull-to-refresh: тягнеш список донизу → onRefresh(). Індикатор також з'являється сам,
        // коли [ExchangeRatesState.isRefreshing] (зміна джерела через дропдаун).
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
                    SourceSelector(selected = state.source, onSelect = onSelectSource)

                    Text(
                        strings.quotedIn("${state.source.baseCode} ${state.source.baseSymbol}"),
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
                            state.rates.forEach { rate -> RateRow(rate) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceSelector(selected: RateSource, onSelect: (RateSource) -> Unit) {
    val strings = LocalStrings.current.exchangeRates
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(strings.sourceLabel, style = MaterialTheme.typography.bodyMedium)
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
}

/** Прапор країни джерела (спільні `flag_*` ресурси з екрана мови) — запасний варіант, поки/якщо
 *  логотип банку не завантажився. ECB — єврозона, без прапора → загальна іконка банку. */
private fun RateSource.flag(): DrawableResource? = when (this) {
    RateSource.PRIVATBANK, RateSource.NBU, RateSource.MONOBANK -> Res.drawable.flag_ukraine
    RateSource.NBP -> Res.drawable.flag_poland
    RateSource.CNB -> Res.drawable.flag_czech_republic
    RateSource.ECB -> null
}

/** Справжній логотип банку — фавікон з його офіційного сайту (той самий, що у вкладці браузера);
 *  Coil кешує його локально, тож після першого разу він є й офлайн. Поки вантажиться / якщо впав —
 *  показуємо прапор країни або загальну іконку банку. */
@Composable
private fun SourceIcon(source: RateSource, modifier: Modifier = Modifier) {
    val fallback: @Composable () -> Unit = {
        val flag = source.flag()
        if (flag != null) {
            Image(
                painter = painterResource(flag),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(CircleShape),
            )
        } else {
            Icon(Icons.Filled.AccountBalance, contentDescription = null, modifier = modifier)
        }
    }
    SubcomposeAsyncImage(
        model = "https://www.google.com/s2/favicons?sz=128&domain=${source.domain}",
        contentDescription = source.displayName,
        contentScale = ContentScale.Fit,
        modifier = modifier.clip(CircleShape),
        loading = { CircularWavyProgressIndicator() },
        error = { fallback() },
    )
}

@Composable
private fun RateRow(rate: ExchangeRate) {
    val strings = LocalStrings.current.exchangeRates
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.space16),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space16, vertical = Dimens.space14),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(rate.currency.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(rate.currency.symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (rate.isSingle) {
                RateColumn(strings.official, rate.sell)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space24)) {
                    RateColumn(strings.buy, rate.buy)
                    RateColumn(strings.sell, rate.sell)
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
    ExchangeRate(Currency.USD, buy = 41.15, sell = 41.65),
    ExchangeRate(Currency.EUR, buy = 44.30, sell = 45.10),
    ExchangeRate(Currency.PLN, buy = 10.35, sell = 10.72),
)

@Composable
private fun Preview(state: ExchangeRatesState) =
    ExchangeRatesContent(state = state, onBack = {}, onSelectSource = {}, onRefresh = {})

@Preview
@Composable
private fun ExchangeRatesLoadedLightPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ExchangeRatesState(source = RateSource.PRIVATBANK, rates = PREVIEW_RATES, date = LocalDate(2026, 9, 1)))
}

@Preview
@Composable
private fun ExchangeRatesLoadedDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(ExchangeRatesState(source = RateSource.NBU, rates = PREVIEW_RATES.map { it.copy(buy = it.sell) }, date = LocalDate(2026, 9, 1)))
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
    Preview(ExchangeRatesState(rates = PREVIEW_RATES, date = LocalDate(2026, 9, 1), stale = true))
}
