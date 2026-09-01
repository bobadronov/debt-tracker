package org.bigblackowl.debttracker.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Джерело курсів валют для [org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen].
 * [displayName] — власна назва (не перекладається). [baseCode]/[baseSymbol] — валюта, у якій
 * джерело котирує решту: українські банки — до гривні, NBP — до злотого, ECB — до євро, ČNB — до
 * крони. Екран показує кожну валюту застосунку окрім [baseCode], виражену в базі. [domain] —
 * офіційний сайт: з нього тягнеться логотип банку для іконки в списку джерел.
 */
enum class RateSource(
    val displayName: String,
    val baseCode: String,
    val baseSymbol: String,
    val domain: String,
) {
    PRIVATBANK("ПриватБанк", "UAH", "₴", "privatbank.ua"),
    NBU("НБУ", "UAH", "₴", "bank.gov.ua"),
    MONOBANK("Monobank", "UAH", "₴", "monobank.ua"),
    NBP("NBP", "PLN", "zł", "nbp.pl"),
    ECB("ECB", "EUR", "€", "ecb.europa.eu"),
    CNB("ČNB", "CZK", "Kč", "cnb.cz"),
    ;

    /** База як [Currency], якщо вона входить до валют застосунку (усі, крім CZK у ČNB). */
    val base: Currency? get() = Currency.fromCode(baseCode)
}

/**
 * Курс однієї іноземної валюти до гривні. Банки дають два боки ([buy] / [sell]); НБУ — один
 * офіційний курс, тоді [buy] == [sell] і UI показує одне число ([isSingle]).
 * Значення — [Double], а не BigDecimal: це довідкові дані для показу, не грошові суми, які мусять
 * точно округлюватись (пор. коментар у `sumByCurrency`).
 */
data class ExchangeRate(
    val currency: Currency,
    val buy: Double,
    val sell: Double,
) {
    val isSingle: Boolean get() = buy == sell
}

/** Один успішний зріз курсів: звідки, коли отримано та на яку дату його опублікувало джерело. */
data class ExchangeRatesSnapshot(
    val source: RateSource,
    val rates: List<ExchangeRate>,
    /** Дата, на яку джерело опублікувало курс (НБУ `exchangedate`, ПБ `date`); `null` — якщо не віддає. */
    val date: LocalDate?,
    val fetchedAt: Instant,
)
