package org.bigblackowl.debttracker.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.ExchangeRate
import org.bigblackowl.debttracker.domain.model.ExchangeRatesSnapshot
import org.bigblackowl.debttracker.domain.model.RateSource
import org.bigblackowl.debttracker.domain.repository.ExchangeRatesRepository
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * [ExchangeRatesRepository] поверх публічних API центробанків/банків (ПриватБанк, НБУ, Monobank,
 * NBP, ECB через frankfurter.dev, ČNB). Кожне віддає свій формат — розбір ізольований у `fetch*`
 * нижче. Показуємо лише валюти застосунку ([DISPLAY_ORDER]) окрім бази самого джерела; кожен курс
 * нормалізуємо до «1 валюта = N одиниць бази».
 *
 * Кеш: останній вдалий зріз кожного джерела лежить однією JSON-мапою в
 * [AppSettings.exchangeRatesCache], тож екран відкривається з даними ще до мережевого запиту.
 */
class HttpExchangeRatesRepository(
    private val client: HttpClient,
    private val settings: AppSettings,
) : ExchangeRatesRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun cached(source: RateSource): ExchangeRatesSnapshot? =
        readCache()[source.name]?.toDomain(source)

    override suspend fun refresh(source: RateSource): ExchangeRatesSnapshot {
        val snapshot = when (source) {
            RateSource.PRIVATBANK -> fetchPrivatBank()
            RateSource.NBU -> fetchNbu()
            RateSource.MONOBANK -> fetchMonobank()
            RateSource.NBP -> fetchNbp()
            RateSource.ECB -> fetchEcb()
            RateSource.CNB -> fetchCnb()
        }
        writeCache(snapshot)
        return snapshot
    }

    /** Валюти застосунку, які [source] взагалі може показати: зі списку показу, окрім власної бази. */
    private fun wanted(source: RateSource): List<Currency> =
        DISPLAY_ORDER.filter { it != source.base }

    // --- ПриватБанк: архівний ендпоінт віддає і курс НБУ, і курс банку; беремо курс банку,
    // а якщо на цю дату банк його ще не виставив — відкочуємось на курс НБУ з тієї ж відповіді.
    private suspend fun fetchPrivatBank(): ExchangeRatesSnapshot {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val response: PbResponse = client.get("https://api.privatbank.ua/p24api/exchange_rates?json") {
            parameter("date", today.toDdMmYyyy())
        }.body()
        val wanted = wanted(RateSource.PRIVATBANK)
        val rates = response.exchangeRate.mapNotNull { row ->
            val currency = row.currency?.let { Currency.fromCode(it) } ?: return@mapNotNull null
            if (currency !in wanted) return@mapNotNull null
            val buy = row.purchaseRate ?: row.purchaseRateNB ?: return@mapNotNull null
            val sell = row.saleRate ?: row.saleRateNB ?: return@mapNotNull null
            ExchangeRate(currency, buy, sell)
        }
        return snapshot(RateSource.PRIVATBANK, rates, response.date?.parseDdMmYyyy())
    }

    private suspend fun fetchNbu(): ExchangeRatesSnapshot {
        val rows: List<NbuRow> =
            client.get("https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json").body()
        val wanted = wanted(RateSource.NBU)
        val byCurrency = rows.mapNotNull { row ->
            val currency = Currency.fromNumericCode(row.r030) ?: return@mapNotNull null
            if (currency !in wanted) return@mapNotNull null
            ExchangeRate(currency, row.rate, row.rate) to row.exchangeDate
        }
        return snapshot(
            RateSource.NBU,
            byCurrency.map { it.first },
            byCurrency.firstOrNull()?.second?.parseDdMmYyyy(),
        )
    }

    // --- Monobank: ISO-числові коди, база — 980 (UAH). Екзотичні пари приходять лише з rateCross
    // (без купівлі/продажу) — тоді показуємо один крос-курс з обох боків.
    private suspend fun fetchMonobank(): ExchangeRatesSnapshot {
        val rows: List<MonoRow> = client.get("https://api.monobank.ua/bank/currency").body()
        val wanted = wanted(RateSource.MONOBANK)
        var date: LocalDate? = null
        val rates = rows.mapNotNull { row ->
            if (row.currencyCodeB != Currency.UAH.numericCode) return@mapNotNull null
            val currency = Currency.fromNumericCode(row.currencyCodeA) ?: return@mapNotNull null
            if (currency !in wanted) return@mapNotNull null
            val buy = row.rateBuy ?: row.rateCross ?: return@mapNotNull null
            val sell = row.rateSell ?: row.rateCross ?: return@mapNotNull null
            row.date?.let { date = Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date }
            ExchangeRate(currency, buy, sell)
        }
        return snapshot(RateSource.MONOBANK, rates, date)
    }

    // --- NBP (Польща): таблиця A, середній курс `mid` = скільки злотих за 1 одиницю валюти.
    private suspend fun fetchNbp(): ExchangeRatesSnapshot {
        val tables: List<NbpTable> = client.get("https://api.nbp.pl/api/exchangerates/tables/A?format=json").body()
        val table = tables.firstOrNull() ?: error("NBP returned no table")
        val wanted = wanted(RateSource.NBP)
        val rates = table.rates.mapNotNull { row ->
            val currency = row.code?.let { Currency.fromCode(it) } ?: return@mapNotNull null
            if (currency !in wanted) return@mapNotNull null
            val mid = row.mid ?: return@mapNotNull null
            ExchangeRate(currency, mid, mid)
        }
        return snapshot(RateSource.NBP, rates, table.effectiveDate?.let { parseIso(it) })
    }

    // --- ECB через frankfurter.dev: `rates[X]` = скільки X за 1 EUR, тож інвертуємо на «X → EUR».
    // Запитуємо без `symbols` (ЄЦБ не публікує гривню — просто не потрапить у список).
    private suspend fun fetchEcb(): ExchangeRatesSnapshot {
        val response: FrankfurterResponse =
            client.get("https://api.frankfurter.dev/v1/latest?base=EUR").body()
        val rates = wanted(RateSource.ECB).mapNotNull { currency ->
            val perEur = response.rates[currency.code]?.takeIf { it > 0.0 } ?: return@mapNotNull null
            val inEur = 1.0 / perEur
            ExchangeRate(currency, inEur, inEur)
        }
        return snapshot(RateSource.ECB, rates, response.date?.let { parseIso(it) })
    }

    // --- ČNB (Чехія): `rate` крон за `amount` одиниць валюти (деякі — за 100), нормалізуємо на 1.
    private suspend fun fetchCnb(): ExchangeRatesSnapshot {
        val response: CnbResponse = client.get("https://api.cnb.cz/cnbapi/exrates/daily?lang=EN").body()
        val wanted = wanted(RateSource.CNB)
        var date: LocalDate? = null
        val rates = response.rates.mapNotNull { row ->
            val currency = row.currencyCode?.let { Currency.fromCode(it) } ?: return@mapNotNull null
            if (currency !in wanted) return@mapNotNull null
            val amount = row.amount?.takeIf { it > 0 } ?: 1
            val perUnit = (row.rate ?: return@mapNotNull null) / amount
            row.validFor?.let { parseIso(it) }?.let { date = it }
            ExchangeRate(currency, perUnit, perUnit)
        }
        return snapshot(RateSource.CNB, rates, date)
    }

    private fun snapshot(source: RateSource, rates: List<ExchangeRate>, date: LocalDate?): ExchangeRatesSnapshot {
        require(rates.isNotEmpty()) { "$source returned no usable rates" }
        return ExchangeRatesSnapshot(
            source = source,
            rates = rates.sortedBy { DISPLAY_ORDER.indexOf(it.currency) },
            date = date,
            fetchedAt = Clock.System.now(),
        )
    }

    // --- cache -------------------------------------------------------------------------------

    private fun readCache(): Map<String, CachedSnapshot> =
        settings.exchangeRatesCache?.let { raw ->
            runCatching { json.decodeFromString<Map<String, CachedSnapshot>>(raw) }
                .onFailure { Napier.w(tag = TAG) { "exchange rates cache unreadable, ignoring" } }
                .getOrNull()
        }.orEmpty()

    private fun writeCache(snapshot: ExchangeRatesSnapshot) {
        val merged = readCache() + (snapshot.source.name to snapshot.toCached())
        settings.exchangeRatesCache = json.encodeToString(merged)
    }

    @Serializable
    private data class CachedSnapshot(
        val date: String?,
        val fetchedAtEpochMs: Long,
        val rates: List<CachedRate>,
    ) {
        fun toDomain(source: RateSource) = ExchangeRatesSnapshot(
            source = source,
            rates = rates.mapNotNull { r ->
                runCatching { Currency.valueOf(r.currency) }.getOrNull()?.let { ExchangeRate(it, r.buy, r.sell) }
            },
            date = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            fetchedAt = Instant.fromEpochMilliseconds(fetchedAtEpochMs),
        )
    }

    @Serializable
    private data class CachedRate(val currency: String, val buy: Double, val sell: Double)

    private fun ExchangeRatesSnapshot.toCached() = CachedSnapshot(
        date = date?.toString(),
        fetchedAtEpochMs = fetchedAt.toEpochMilliseconds(),
        rates = rates.map { CachedRate(it.currency.name, it.buy, it.sell) },
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
        val r030: Int,
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

        /** Валюти застосунку у порядку показу; кожне джерело ще відкидає власну базу ([wanted]). */
        val DISPLAY_ORDER = listOf(Currency.USD, Currency.EUR, Currency.PLN, Currency.UAH)

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
