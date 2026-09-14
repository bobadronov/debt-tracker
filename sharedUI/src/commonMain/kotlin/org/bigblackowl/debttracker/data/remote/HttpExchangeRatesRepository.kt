package org.bigblackowl.debttracker.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.ExchangeRate
import org.bigblackowl.debttracker.domain.model.ExchangeRatesSnapshot
import org.bigblackowl.debttracker.domain.model.FiatCurrencies
import org.bigblackowl.debttracker.domain.model.FiatCurrency
import org.bigblackowl.debttracker.domain.model.RateSource
import org.bigblackowl.debttracker.domain.repository.ExchangeRatesRepository
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * [ExchangeRatesRepository] on top of public central bank/bank and aggregator APIs (PrivatBank, NBU,
 * Monobank, NBP, ECB via frankfurter.dev, ČNB, ExchangeRate-API via open.er-api.com). Each returns
 * its own format — parsing is isolated in the `fetch*` methods below. We show every currency the
 * source returns except the snapshot's own base; each rate is normalized to "1 currency = N base units".
 *
 * Base: fixed (home currency) for banks, chosen by the user ([baseCode]) for [RateSource.arbitraryBase]
 * sources. Cache: the last successful snapshot of each `source|base` pair sits as one JSON map in
 * [AppSettings.exchangeRatesCache], so the screen opens with data even before the request completes.
 */
class HttpExchangeRatesRepository(
    private val client: HttpClient,
    private val settings: AppSettings,
) : ExchangeRatesRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun effectiveBase(source: RateSource, baseCode: String): String =
        if (source.arbitraryBase) baseCode else source.baseCode

    override fun cached(source: RateSource, baseCode: String): ExchangeRatesSnapshot? {
        val base = effectiveBase(source, baseCode)
        return readCache()[cacheKey(source, base)]?.toDomain(source)
    }

    override suspend fun refresh(source: RateSource, baseCode: String): ExchangeRatesSnapshot {
        val base = effectiveBase(source, baseCode)
        val snapshot = when (source) {
            RateSource.PRIVATBANK -> fetchPrivatBank()
            RateSource.NBU -> fetchNbu()
            RateSource.MONOBANK -> fetchMonobank()
            RateSource.NBP -> fetchNbp()
            RateSource.ECB -> fetchEcb(base)
            RateSource.CNB -> fetchCnb()
            RateSource.EXCHANGERATE_API -> fetchExchangerateApi(base)
        }
        writeCache(snapshot)
        return snapshot
    }

    // --- PrivatBank: the archive endpoint returns both the NBU rate and the bank's own rate; we take
    // the bank's rate, and if a row only has the NBU rate, we show that instead. Early in the morning
    // the bank hasn't set today's rate yet (`exchangeRate: []`) — then we fall back to yesterday's snapshot.
    private suspend fun fetchPrivatBank(): ExchangeRatesSnapshot {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val response = fetchPrivatBankOn(today).takeIf { it.exchangeRate.isNotEmpty() }
            ?: fetchPrivatBankOn(today.minus(DatePeriod(days = 1)))
        val rates = response.exchangeRate.mapNotNull { row ->
            val code = row.currency?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val buy = row.purchaseRate ?: row.purchaseRateNB ?: return@mapNotNull null
            val sell = row.saleRate ?: row.saleRateNB ?: return@mapNotNull null
            ExchangeRate(FiatCurrencies.of(code), buy, sell)
        }
        return snapshot(RateSource.PRIVATBANK, RateSource.PRIVATBANK.homeCurrency, rates, response.date?.parseDdMmYyyy())
    }

    private suspend fun fetchPrivatBankOn(date: LocalDate): PbResponse =
        client.get("https://api.privatbank.ua/p24api/exchange_rates?json") {
            parameter("date", date.toDdMmYyyy())
        }.body()

    // --- NBU: `cc` is the letter code, `rate` is already "hryvnias per 1 unit".
    private suspend fun fetchNbu(): ExchangeRatesSnapshot {
        val rows: List<NbuRow> =
            client.get("https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json").body()
        val rates = rows.mapNotNull { row ->
            val code = row.cc?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ExchangeRate(FiatCurrencies.of(code), row.rate, row.rate)
        }
        return snapshot(
            RateSource.NBU,
            RateSource.NBU.homeCurrency,
            rates,
            rows.firstOrNull()?.exchangeDate?.parseDdMmYyyy(),
        )
    }

    // --- Monobank: ISO numeric codes, base is 980 (UAH). Exotic pairs come only via rateCross
    // (no separate buy/sell) — then we show a single cross-rate for both sides.
    private suspend fun fetchMonobank(): ExchangeRatesSnapshot {
        val rows: List<MonoRow> = client.get("https://api.monobank.ua/bank/currency").body()
        var date: LocalDate? = null
        val rates = rows.mapNotNull { row ->
            if (row.currencyCodeB != UAH_NUMERIC) return@mapNotNull null
            val code = ISO_4217_NUMERIC[row.currencyCodeA] ?: return@mapNotNull null
            val buy = row.rateBuy ?: row.rateCross ?: return@mapNotNull null
            val sell = row.rateSell ?: row.rateCross ?: return@mapNotNull null
            row.date?.let { date = Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date }
            ExchangeRate(FiatCurrencies.of(code), buy, sell)
        }
        return snapshot(RateSource.MONOBANK, RateSource.MONOBANK.homeCurrency, rates, date)
    }

    // --- NBP (Poland): table A, the mid rate `mid` = how many złoty per 1 unit of currency.
    private suspend fun fetchNbp(): ExchangeRatesSnapshot {
        val tables: List<NbpTable> = client.get("https://api.nbp.pl/api/exchangerates/tables/A?format=json").body()
        val table = tables.firstOrNull() ?: error("NBP returned no table")
        val rates = table.rates.mapNotNull { row ->
            val code = row.code?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val mid = row.mid ?: return@mapNotNull null
            ExchangeRate(FiatCurrencies.of(code), mid, mid)
        }
        return snapshot(RateSource.NBP, RateSource.NBP.homeCurrency, rates, table.effectiveDate?.let { parseIso(it) })
    }

    // --- ECB via frankfurter.dev: `?base=<base>` → `rates[X]` = how many X per 1 unit of the base,
    // so we invert to "1 X = N base". The ECB doesn't publish the hryvnia — it just won't appear in the list.
    private suspend fun fetchEcb(baseCode: String): ExchangeRatesSnapshot {
        val response: FrankfurterResponse =
            client.get("https://api.frankfurter.dev/v1/latest") { parameter("base", baseCode) }.body()
        val rates = response.rates.mapNotNull { (code, perBase) ->
            val inverted = perBase.takeIf { it > 0.0 }?.let { 1.0 / it } ?: return@mapNotNull null
            ExchangeRate(FiatCurrencies.of(code), inverted, inverted)
        }
        return snapshot(RateSource.ECB, FiatCurrencies.of(baseCode), rates, response.date?.let { parseIso(it) })
    }

    // --- ČNB (Czechia): `rate` is korunas per `amount` units of currency (some are per 100), we normalize to 1.
    private suspend fun fetchCnb(): ExchangeRatesSnapshot {
        val response: CnbResponse = client.get("https://api.cnb.cz/cnbapi/exrates/daily?lang=EN").body()
        var date: LocalDate? = null
        val rates = response.rates.mapNotNull { row ->
            val code = row.currencyCode?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val amount = row.amount?.takeIf { it > 0 } ?: 1
            val perUnit = (row.rate ?: return@mapNotNull null) / amount
            row.validFor?.let { parseIso(it) }?.let { date = it }
            ExchangeRate(FiatCurrencies.of(code), perUnit, perUnit)
        }
        return snapshot(RateSource.CNB, RateSource.CNB.homeCurrency, rates, date)
    }

    // --- ExchangeRate-API via open.er-api.com (free, no key, ~160 currencies):
    // `/v6/latest/<base>` → `rates[X]` = how many X per 1 unit of the base, we invert to "1 X = N base".
    private suspend fun fetchExchangerateApi(baseCode: String): ExchangeRatesSnapshot {
        val response: ErApiResponse = client.get("https://open.er-api.com/v6/latest/$baseCode").body()
        if (response.result != null && response.result != "success") error("ExchangeRate-API: ${response.result}")
        val rates = response.rates.mapNotNull { (code, perBase) ->
            val inverted = perBase.takeIf { it > 0.0 }?.let { 1.0 / it } ?: return@mapNotNull null
            ExchangeRate(FiatCurrencies.of(code), inverted, inverted)
        }
        val date = response.timeLastUpdateUnix
            ?.let { Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date }
        return snapshot(RateSource.EXCHANGERATE_API, FiatCurrencies.of(baseCode), rates, date)
    }

    private fun snapshot(
        source: RateSource,
        base: FiatCurrency,
        rates: List<ExchangeRate>,
        date: LocalDate?,
    ): ExchangeRatesSnapshot {
        val clean = rates
            .filter { it.currency.code != base.code }
            .distinctBy { it.currency.code }
            .sortedForDisplay()
        require(clean.isNotEmpty()) { "$source returned no usable rates" }
        return ExchangeRatesSnapshot(
            source = source,
            base = base,
            rates = clean,
            date = date,
            fetchedAt = Clock.System.now(),
        )
    }

    // --- cache -------------------------------------------------------------------------------

    private fun cacheKey(source: RateSource, baseCode: String) = "${source.name}|$baseCode"

    private fun readCache(): Map<String, CachedSnapshot> =
        settings.exchangeRatesCache?.let { raw ->
            runCatching { json.decodeFromString<Map<String, CachedSnapshot>>(raw) }
                .onFailure { Napier.w(tag = TAG) { "exchange rates cache unreadable, ignoring" } }
                .getOrNull()
        }.orEmpty()

    private fun writeCache(snapshot: ExchangeRatesSnapshot) {
        val merged = readCache() + (cacheKey(snapshot.source, snapshot.base.code) to snapshot.toCached())
        settings.exchangeRatesCache = json.encodeToString(merged)
    }

    @Serializable
    private data class CachedSnapshot(
        val base: String,
        val date: String?,
        val fetchedAtEpochMs: Long,
        val rates: List<CachedRate>,
    ) {
        fun toDomain(source: RateSource) = ExchangeRatesSnapshot(
            source = source,
            base = FiatCurrencies.of(base),
            rates = rates.map { ExchangeRate(FiatCurrencies.of(it.currency), it.buy, it.sell) },
            date = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            fetchedAt = Instant.fromEpochMilliseconds(fetchedAtEpochMs),
        )
    }

    @Serializable
    private data class CachedRate(val currency: String, val buy: Double, val sell: Double)

    private fun ExchangeRatesSnapshot.toCached() = CachedSnapshot(
        base = base.code,
        date = date?.toString(),
        fetchedAtEpochMs = fetchedAt.toEpochMilliseconds(),
        rates = rates.map { CachedRate(it.currency.code, it.buy, it.sell) },
    )

    // --- wire DTOs --------------------------------------------------------------------------

    @Serializable
    private data class PbResponse(val date: String? = null, val exchangeRate: List<PbRate> = emptyList())

    @Serializable
    private data class PbRate(
        val currency: String? = null,
        val saleRateNB: Double? = null,
        val purchaseRateNB: Double? = null,
        val saleRate: Double? = null,
        val purchaseRate: Double? = null,
    )

    @Serializable
    private data class NbuRow(
        val r030: Int = 0,
        val rate: Double,
        val cc: String? = null,
        @SerialName("exchangedate") val exchangeDate: String? = null,
    )

    @Serializable
    private data class MonoRow(
        val currencyCodeA: Int,
        val currencyCodeB: Int,
        val date: Long? = null,
        val rateBuy: Double? = null,
        val rateSell: Double? = null,
        val rateCross: Double? = null,
    )

    @Serializable
    private data class NbpTable(val effectiveDate: String? = null, val rates: List<NbpRate> = emptyList())

    @Serializable
    private data class NbpRate(val code: String? = null, val mid: Double? = null)

    @Serializable
    private data class FrankfurterResponse(val date: String? = null, val rates: Map<String, Double> = emptyMap())

    @Serializable
    private data class ErApiResponse(
        val result: String? = null,
        @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long? = null,
        val rates: Map<String, Double> = emptyMap(),
    )

    @Serializable
    private data class CnbResponse(val rates: List<CnbRate> = emptyList())

    @Serializable
    private data class CnbRate(
        val currencyCode: String? = null,
        val amount: Int? = null,
        val rate: Double? = null,
        val validFor: String? = null,
    )

    private companion object {
        const val TAG = "ExchangeRates"

        const val UAH_NUMERIC = 980

        /** Currencies shown first (the rest sorted alphabetically by code). */
        val PRIORITY = listOf("USD", "EUR", "GBP", "PLN", "UAH", "CHF", "JPY", "CZK", "CAD", "AUD", "CNY")

        fun List<ExchangeRate>.sortedForDisplay(): List<ExchangeRate> = sortedWith(
            compareBy(
                { PRIORITY.indexOf(it.currency.code).let { i -> if (i < 0) Int.MAX_VALUE else i } },
                { it.currency.code },
            ),
        )

        /** ISO-4217 numeric → letter code, for Monobank (it only returns numeric codes). */
        val ISO_4217_NUMERIC: Map<Int, String> = mapOf(
            840 to "USD", 978 to "EUR", 826 to "GBP", 985 to "PLN", 756 to "CHF", 392 to "JPY",
            203 to "CZK", 124 to "CAD", 36 to "AUD", 554 to "NZD", 156 to "CNY", 752 to "SEK",
            578 to "NOK", 208 to "DKK", 348 to "HUF", 946 to "RON", 975 to "BGN", 949 to "TRY",
            376 to "ILS", 784 to "AED", 682 to "SAR", 702 to "SGD", 344 to "HKD", 410 to "KRW",
            356 to "INR", 484 to "MXN", 710 to "ZAR", 764 to "THB", 360 to "IDR", 608 to "PHP",
            704 to "VND", 818 to "EGP", 981 to "GEL", 398 to "KZT", 498 to "MDL", 352 to "ISK",
            191 to "HRK", 941 to "RSD", 986 to "BRL", 980 to "UAH", 934 to "TMT", 944 to "AZN",
            51 to "AMD", 933 to "BYN",
        )

        fun LocalDate.toDdMmYyyy(): String {
            val d = day.toString().padStart(2, '0')
            val m = month.number.toString().padStart(2, '0')
            return "$d.$m.$year"
        }

        fun String.parseDdMmYyyy(): LocalDate? = runCatching {
            val (d, m, y) = split(".")
            LocalDate(y.toInt(), m.toInt(), d.toInt())
        }.getOrNull()

        fun parseIso(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
    }
}
