package org.bigblackowl.debttracker.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Валюта на екрані курсів ([org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen]) —
 * окремий довідник, ширший за [Currency] (той — лише 4 валюти боргів застосунку). [code] — ISO-4217
 * літерний, [flagCc] — ISO-3166 alpha-2 для прапора з flagcdn.com (`null` для наднаціональних/
 * металевих кодів на кшталт XAU/XDR/XOF).
 */
data class FiatCurrency(
    val code: String,
    val symbol: String,
    val name: String,
    val flagCc: String?,
) {
    /** PNG-прапор країни валюти (Coil кешує → після першого разу є й офлайн); `null` — показуємо запасну іконку. */
    fun flagUrl(): String? = flagCc?.let { "https://flagcdn.com/w80/$it.png" }
}

/**
 * Довідник валют для показу. [catalog] — курований список поширених валют із назвою/символом/прапором;
 * [of] повертає його елемент або синтезує запис для будь-якого іншого ISO-коду, що прийшов з API
 * (назва = символ = сам код, прапор — за першими двома літерами коду, часто це і є код країни).
 */
object FiatCurrencies {

    val catalog: List<FiatCurrency> = listOf(
        FiatCurrency("USD", "$", "US Dollar", "us"),
        FiatCurrency("EUR", "€", "Euro", "eu"),
        FiatCurrency("GBP", "£", "British Pound", "gb"),
        FiatCurrency("UAH", "₴", "Ukrainian Hryvnia", "ua"),
        FiatCurrency("PLN", "zł", "Polish Złoty", "pl"),
        FiatCurrency("CHF", "Fr", "Swiss Franc", "ch"),
        FiatCurrency("JPY", "¥", "Japanese Yen", "jp"),
        FiatCurrency("CZK", "Kč", "Czech Koruna", "cz"),
        FiatCurrency("CAD", "$", "Canadian Dollar", "ca"),
        FiatCurrency("AUD", "$", "Australian Dollar", "au"),
        FiatCurrency("NZD", "$", "New Zealand Dollar", "nz"),
        FiatCurrency("SEK", "kr", "Swedish Krona", "se"),
        FiatCurrency("NOK", "kr", "Norwegian Krone", "no"),
        FiatCurrency("DKK", "kr", "Danish Krone", "dk"),
        FiatCurrency("CNY", "¥", "Chinese Yuan", "cn"),
        FiatCurrency("HKD", "$", "Hong Kong Dollar", "hk"),
        FiatCurrency("SGD", "$", "Singapore Dollar", "sg"),
        FiatCurrency("TRY", "₺", "Turkish Lira", "tr"),
        FiatCurrency("INR", "₹", "Indian Rupee", "in"),
        FiatCurrency("BRL", "R$", "Brazilian Real", "br"),
        FiatCurrency("ZAR", "R", "South African Rand", "za"),
        FiatCurrency("MXN", "$", "Mexican Peso", "mx"),
        FiatCurrency("KRW", "₩", "South Korean Won", "kr"),
        FiatCurrency("AED", "د.إ", "UAE Dirham", "ae"),
        FiatCurrency("SAR", "﷼", "Saudi Riyal", "sa"),
        FiatCurrency("ILS", "₪", "Israeli Shekel", "il"),
        FiatCurrency("HUF", "Ft", "Hungarian Forint", "hu"),
        FiatCurrency("RON", "lei", "Romanian Leu", "ro"),
        FiatCurrency("BGN", "лв", "Bulgarian Lev", "bg"),
        FiatCurrency("ISK", "kr", "Icelandic Króna", "is"),
        FiatCurrency("THB", "฿", "Thai Baht", "th"),
        FiatCurrency("IDR", "Rp", "Indonesian Rupiah", "id"),
        FiatCurrency("MYR", "RM", "Malaysian Ringgit", "my"),
        FiatCurrency("PHP", "₱", "Philippine Peso", "ph"),
        FiatCurrency("VND", "₫", "Vietnamese Đồng", "vn"),
        FiatCurrency("EGP", "£", "Egyptian Pound", "eg"),
        FiatCurrency("GEL", "₾", "Georgian Lari", "ge"),
        FiatCurrency("MDL", "L", "Moldovan Leu", "md"),
        FiatCurrency("KZT", "₸", "Kazakhstani Tenge", "kz"),
        FiatCurrency("RSD", "дин", "Serbian Dinar", "rs"),
        FiatCurrency("HRK", "kn", "Croatian Kuna", "hr"),
        FiatCurrency("ARS", "$", "Argentine Peso", "ar"),
        FiatCurrency("CLP", "$", "Chilean Peso", "cl"),
        FiatCurrency("COP", "$", "Colombian Peso", "co"),
        FiatCurrency("XAU", "oz", "Gold (ounce)", null),
        FiatCurrency("XDR", "SDR", "IMF Special Drawing Rights", null),
    )

    private val byCode: Map<String, FiatCurrency> = catalog.associateBy { it.code }

    /** Каталожна валюта, або синтезований запис для будь-якого іншого ISO-коду з відповіді API. */
    fun of(code: String): FiatCurrency =
        byCode[code] ?: FiatCurrency(code, code, code, code.take(2).lowercase().takeIf { code.length >= 2 })
}

/**
 * Джерело курсів валют для [org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen].
 * [displayName] — власна назва (не перекладається). [baseCode]/[baseSymbol] — валюта, у якій джерело
 * котирує решту: банки — до своєї домашньої валюти (фіксовано), а [arbitraryBase]-джерела (ECB через
 * frankfurter, ExchangeRate-API) приймають будь-яку базу, обрану користувачем, — тоді [baseCode] лише
 * дефолт. Екран показує кожну валюту джерела окрім самої бази, виражену в базі. [domain] —
 * офіційний сайт: з нього тягнеться логотип для іконки в списку джерел.
 */
enum class RateSource(
    val displayName: String,
    val baseCode: String,
    val baseSymbol: String,
    val domain: String,
    val arbitraryBase: Boolean = false,
) {
    PRIVATBANK("ПриватБанк", "UAH", "₴", "privatbank.ua"),
    NBU("НБУ", "UAH", "₴", "bank.gov.ua"),
    MONOBANK("Monobank", "UAH", "₴", "monobank.ua"),
    NBP("NBP", "PLN", "zł", "nbp.pl"),
    ECB("ECB", "EUR", "€", "ecb.europa.eu", arbitraryBase = true),
    CNB("ČNB", "CZK", "Kč", "cnb.cz"),
    EXCHANGERATE_API("ExchangeRate-API", "USD", "$", "exchangerate-api.com", arbitraryBase = true),
    ;

    /** Домашня валюта джерела як [FiatCurrency] — база для банків, дефолтна база для решти. */
    val homeCurrency: FiatCurrency get() = FiatCurrencies.of(baseCode)
}

/**
 * Курс однієї валюти до бази зрізу. Банки дають два боки ([buy] / [sell]); центробанки та агрегатори —
 * один курс, тоді [buy] == [sell] і UI показує одне число ([isSingle]).
 * Значення — [Double], а не BigDecimal: це довідкові дані для показу, не грошові суми, які мусять
 * точно округлюватись (пор. коментар у `sumByCurrency`).
 */
data class ExchangeRate(
    val currency: FiatCurrency,
    val buy: Double,
    val sell: Double,
) {
    val isSingle: Boolean get() = buy == sell
}

/** Один успішний зріз курсів: звідки, у якій базі, коли отримано та на яку дату його опублікувало джерело. */
data class ExchangeRatesSnapshot(
    val source: RateSource,
    val base: FiatCurrency,
    val rates: List<ExchangeRate>,
    /** Дата, на яку джерело опублікувало курс (НБУ `exchangedate`, ПБ `date`); `null` — якщо не віддає. */
    val date: LocalDate?,
    val fetchedAt: Instant,
)
