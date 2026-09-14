package org.bigblackowl.debttracker.domain.model

import kotlinx.datetime.LocalDate
import org.bigblackowl.debttracker.domain.model.FiatCurrencies.catalog
import org.bigblackowl.debttracker.domain.model.FiatCurrencies.of
import kotlin.time.Instant

/**
 * A currency on the rates screen ([org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen]) —
 * a separate reference list, wider than [Currency] (which is just the app's 4 debt currencies).
 * [code] — ISO-4217 letter code, [flagCc] — ISO-3166 alpha-2 for the flag from flagcdn.com (`null` for
 * supranational/metal codes like XAU/XDR/XOF).
 */
data class FiatCurrency(
    val code: String,
    val symbol: String,
    val name: String,
    val flagCc: String?,
) {
    /** PNG flag of the currency's country (Coil caches it → available offline after the first time); `null` — show the fallback icon. */
    fun flagUrl(): String? = flagCc?.let { "https://flagcdn.com/w80/$it.png" }
}

/**
 * Reference list of currencies to display. [catalog] — a curated list of common currencies with
 * name/symbol/flag; [of] returns its entry or synthesizes one for any other ISO code coming from
 * the API (name = symbol = the code itself, flag from the code's first two letters, which is often
 * also the country code).
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

    /** A catalog currency, or a synthesized entry for any other ISO code from the API response. */
    fun of(code: String): FiatCurrency =
        byCode[code] ?: FiatCurrency(code, code, code, code.take(2).lowercase().takeIf { code.length >= 2 })
}

/**
 * Source of exchange rates for [org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen].
 * [displayName] — the source's own name (not translated). [baseCode]/[baseSymbol] — the currency the
 * source quotes the rest in: banks quote against their own home currency (fixed), while
 * [arbitraryBase] sources (ECB via frankfurter, ExchangeRate-API) accept any base chosen by the user —
 * then [baseCode] is just the default. The screen shows every currency of the source except the base
 * itself, expressed in the base. [domain] — the official website: its logo is fetched for the icon
 * in the source list.
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

    /** The source's home currency as [FiatCurrency] — the base for banks, the default base for the rest. */
    val homeCurrency: FiatCurrency get() = FiatCurrencies.of(baseCode)
}

/**
 * The rate of one currency against the snapshot's base. Banks give two sides ([buy] / [sell]);
 * central banks and aggregators give one rate, in which case [buy] == [sell] and the UI shows a
 * single number ([isSingle]).
 * The value is [Double], not BigDecimal: this is reference data for display, not monetary amounts
 * that must round precisely (cf. the comment in `sumByCurrency`).
 */
data class ExchangeRate(
    val currency: FiatCurrency,
    val buy: Double,
    val sell: Double,
) {
    val isSingle: Boolean get() = buy == sell
}

/** One successful rate snapshot: where from, in which base, when fetched, and the date the source published it for. */
data class ExchangeRatesSnapshot(
    val source: RateSource,
    val base: FiatCurrency,
    val rates: List<ExchangeRate>,
    /** The date the source published the rate for (NBU's `exchangedate`, PrivatBank's `date`); `null` if not provided. */
    val date: LocalDate?,
    val fetchedAt: Instant,
)
